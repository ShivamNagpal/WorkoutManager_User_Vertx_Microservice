package com.nagpal.shivam.workout_manager_user.services.impl;

import com.nagpal.shivam.workout_manager_user.daos.RoleDao;
import com.nagpal.shivam.workout_manager_user.daos.SessionDao;
import com.nagpal.shivam.workout_manager_user.daos.UserDao;
import com.nagpal.shivam.workout_manager_user.dtos.internal.JWTAuthTokenDTO;
import com.nagpal.shivam.workout_manager_user.dtos.internal.SessionPayload;
import com.nagpal.shivam.workout_manager_user.dtos.request.RefreshSessionRequestDTO;
import com.nagpal.shivam.workout_manager_user.dtos.response.LoginResponseDTO;
import com.nagpal.shivam.workout_manager_user.enums.AccountStatus;
import com.nagpal.shivam.workout_manager_user.enums.ResponseMessage;
import com.nagpal.shivam.workout_manager_user.enums.SessionStatus;
import com.nagpal.shivam.workout_manager_user.exceptions.ResponseException;
import com.nagpal.shivam.workout_manager_user.models.Session;
import com.nagpal.shivam.workout_manager_user.models.User;
import com.nagpal.shivam.workout_manager_user.services.JWTService;
import com.nagpal.shivam.workout_manager_user.services.SessionService;
import com.nagpal.shivam.workout_manager_user.utils.Constants;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.pgclient.PgPool;

public class SessionServiceImpl implements SessionService {
    private final PgPool pgPool;
    private final MongoClient mongoClient;
    private final JsonObject config;
    private final SessionDao sessionDao;
    private final JWTService jwtService;
    private final UserDao userDao;
    private final RoleDao roleDao;

    public SessionServiceImpl(PgPool pgPool, MongoClient mongoClient, JsonObject config, SessionDao sessionDao,
                              JWTService jwtService, UserDao userDao, RoleDao roleDao) {
        this.pgPool = pgPool;
        this.mongoClient = mongoClient;
        this.config = config;
        this.sessionDao = sessionDao;
        this.jwtService = jwtService;
        this.userDao = userDao;
        this.roleDao = roleDao;
    }

    @Override
    public Future<SessionPayload> createNewSession(MongoClient mongoClient, Long userId) {
        Session session = Session.newDocument(userId, config);
        return mongoClient.save(Constants.SESSION, JsonObject.mapFrom(session))
                .map(sessionId -> {
                    SessionPayload sessionPayload = new SessionPayload();
                    sessionPayload.setSessionId(sessionId);
                    sessionPayload.setRefreshToken(session.getCurrentRefreshToken());
                    return sessionPayload;
                });
    }

    @Override
    public Future<LoginResponseDTO> createNewSessionAndFormLoginResponse(MongoClient mongoClient, Long userId,
                                                                         String[] roles) {
        return createNewSession(mongoClient, userId)
                .map(sessionPayload -> getLoginResponseDTO(userId, roles, sessionPayload));
    }

    @Override
    public Future<LoginResponseDTO> refreshSession(RefreshSessionRequestDTO refreshSessionRequestDTO) {
        return SessionPayload.fromRefreshToken(refreshSessionRequestDTO.getRefreshToken())
                .compose(sessionPayload ->
                        sessionDao.findById(mongoClient, sessionPayload.getSessionId())
                                .compose(sessionOptional -> {
                                    if (sessionOptional.isEmpty()) {
                                        ResponseMessage responseMessage = ResponseMessage.INVALID_REFRESH_TOKEN;
                                        return Future.failedFuture(
                                                new ResponseException(HttpResponseStatus.BAD_REQUEST.code(),
                                                        responseMessage.getMessageCode(), responseMessage.getMessage(),
                                                        null
                                                )
                                        );
                                    }
                                    Session session = sessionOptional.get();
                                    if (session.getExpiryTime() < System.currentTimeMillis()) {
                                        ResponseMessage responseMessage = ResponseMessage.SESSION_HAS_EXPIRED;
                                        return Future.failedFuture(
                                                new ResponseException(HttpResponseStatus.NOT_ACCEPTABLE.code(),
                                                        responseMessage.getMessageCode(), responseMessage.getMessage(),
                                                        null
                                                )
                                        );
                                    }
                                    if (session.getStatus() != SessionStatus.ACTIVE) {
                                        ResponseMessage responseMessage = ResponseMessage.SESSION_IS_NOT_ACTIVE;
                                        return Future.failedFuture(
                                                new ResponseException(HttpResponseStatus.NOT_ACCEPTABLE.code(),
                                                        responseMessage.getMessageCode(), responseMessage.getMessage(),
                                                        null
                                                )
                                        );
                                    }
                                    return pgPool.withTransaction(
                                            sqlConnection -> userDao.getById(sqlConnection, session.getUserId())
                                                    .compose(userOptional -> {
                                                        if (userOptional.isEmpty()) {
                                                            ResponseMessage responseMessage =
                                                                    ResponseMessage.USER_NOT_FOUND;
                                                            return Future.failedFuture(
                                                                    new ResponseException(
                                                                            HttpResponseStatus.NOT_ACCEPTABLE.code(),
                                                                            responseMessage.getMessageCode(),
                                                                            responseMessage.getMessage(),
                                                                            null
                                                                    )
                                                            );
                                                        }
                                                        User user = userOptional.get();
                                                        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                                                            ResponseMessage responseMessage =
                                                                    ResponseMessage.USER_ACCOUNT_IS_NOT_ACTIVE;
                                                            return Future.failedFuture(
                                                                    new ResponseException(
                                                                            HttpResponseStatus.NOT_ACCEPTABLE.code(),
                                                                            responseMessage.getMessageCode(),
                                                                            responseMessage.getMessage(),
                                                                            null
                                                                    )
                                                            );
                                                        }
                                                        return Future.succeededFuture();
                                                    })
                                                    .compose(obj -> roleDao.fetchRolesByUserIdAndDeleted(sqlConnection,
                                                            session.getUserId(),
                                                            false
                                                    ))
                                                    .compose(roles -> {
                                                        if (!session.getCurrentRefreshToken()
                                                                .equals(sessionPayload.getRefreshToken())) {
                                                            Future<Void> future = Future.succeededFuture();
                                                            if (session.getUsedRefreshTokens()
                                                                    .contains(sessionPayload.getRefreshToken())) {
                                                                future =
                                                                        sessionDao.updateStatus(mongoClient,
                                                                                session.getId(), SessionStatus.VOID
                                                                        );
                                                            }
                                                            return future.compose(v -> {
                                                                ResponseMessage responseMessage =
                                                                        ResponseMessage.INVALID_REFRESH_TOKEN;
                                                                return Future.failedFuture(
                                                                        new ResponseException(
                                                                                HttpResponseStatus.BAD_REQUEST.code(),
                                                                                responseMessage.getMessageCode(),
                                                                                responseMessage.getMessage(),
                                                                                null
                                                                        )
                                                                );
                                                            });
                                                        }
                                                        session.refresh();
                                                        sessionPayload.setRefreshToken(
                                                                session.getCurrentRefreshToken());
                                                        String[] roleNamesArray =
                                                                roles.stream().map(r -> r.getRoleName().name())
                                                                        .toArray(String[]::new);
                                                        return sessionDao.updateRefreshToken(mongoClient, session)
                                                                .map(getLoginResponseDTO(session.getUserId(),
                                                                        roleNamesArray, sessionPayload));
                                                    })
                                    );
                                })
                );
    }

    private LoginResponseDTO getLoginResponseDTO(Long userId, String[] roles, SessionPayload sessionPayload) {
        LoginResponseDTO loginResponseDTO = new LoginResponseDTO();
        JWTAuthTokenDTO jwtAuthTokenDTO = new JWTAuthTokenDTO();
        jwtAuthTokenDTO.setUserId(userId);
        jwtAuthTokenDTO.setSessionId(sessionPayload.getSessionId());
        jwtAuthTokenDTO.setRoles(roles);

        loginResponseDTO.setAuthToken(jwtService.generateAuthToken(jwtAuthTokenDTO));
        loginResponseDTO.setRefreshToken(sessionPayload.createRefreshToken());
        return loginResponseDTO;
    }
}
