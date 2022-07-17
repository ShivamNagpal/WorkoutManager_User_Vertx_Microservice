package com.nagpal.shivam.workout_manager_user.helpers;

import com.nagpal.shivam.workout_manager_user.daos.SessionDao;
import com.nagpal.shivam.workout_manager_user.daos.UserDao;
import com.nagpal.shivam.workout_manager_user.dtos.request.PasswordUpdateRequestDTO;
import com.nagpal.shivam.workout_manager_user.enums.AccountStatus;
import com.nagpal.shivam.workout_manager_user.enums.ResponseMessage;
import com.nagpal.shivam.workout_manager_user.exceptions.ResponseException;
import com.nagpal.shivam.workout_manager_user.models.User;
import com.nagpal.shivam.workout_manager_user.utils.Constants;
import com.nagpal.shivam.workout_manager_user.utils.ModelConstants;
import com.nagpal.shivam.workout_manager_user.utils.UtilMethods;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlConnection;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.time.OffsetDateTime;
import java.util.UUID;


public class UserHelper {
    private final JsonObject config;
    private final UserDao userDao;
    private final SessionDao sessionDao;

    public UserHelper(JsonObject config, UserDao userDao, SessionDao sessionDao) {
        this.config = config;
        this.userDao = userDao;
        this.sessionDao = sessionDao;
    }

    public Future<User> getUserById(SqlConnection sqlConnection, Long userId) {
        return userDao.getById(sqlConnection, userId)
                .compose(userOptional -> {
                    if (userOptional.isEmpty()) {
                        ResponseMessage responseMessage = ResponseMessage.USER_NOT_FOUND;
                        return Future.failedFuture(
                                new ResponseException(HttpResponseStatus.BAD_REQUEST.code(),
                                        responseMessage.getMessageCode(), responseMessage.getMessage(), null)
                        );
                    }
                    return Future.succeededFuture(userOptional.get());
                });
    }

    public Future<User> getUserByEmail(SqlConnection sqlConnection, String email) {
        return userDao.getUserByEmail(sqlConnection, email)
                .compose(userOptional -> {
                    if (userOptional.isEmpty()) {
                        ResponseMessage responseMessage = ResponseMessage.USER_NOT_FOUND;
                        return Future.failedFuture(new ResponseException(HttpResponseStatus.BAD_REQUEST.code(),
                                responseMessage.getMessageCode(), responseMessage.getMessage(),
                                null
                        ));
                    }
                    return Future.succeededFuture(userOptional.get());
                });
    }

    public Future<Void> updatePasswordAndLogOutAllSessions(SqlConnection sqlConnection, MongoClient mongoClient,
                                                           Long userId,
                                                           PasswordUpdateRequestDTO passwordUpdateRequestDTO) {
        return getUserById(sqlConnection, userId)
                .compose(user -> updatePasswordAndLogOutAllSessions(sqlConnection, mongoClient, user,
                        passwordUpdateRequestDTO
                ));
    }

    public Future<Void> updatePasswordAndLogOutAllSessions(SqlConnection sqlConnection, MongoClient mongoClient,
                                                           User user,
                                                           PasswordUpdateRequestDTO passwordUpdateRequestDTO) {
        if (BCrypt.checkpw(passwordUpdateRequestDTO.getPlainPassword(), user.getPassword())) {
            ResponseMessage responseMessage =
                    ResponseMessage.NEW_PASSWORD_CANNOT_BE_SAME_AS_THE_OLD_PASSWORD;
            return Future.failedFuture(new ResponseException(HttpResponseStatus.NOT_ACCEPTABLE.code(),
                    responseMessage.getMessageCode(), responseMessage.getMessage(), null
            ));
        }
        return userDao.updatePassword(sqlConnection, user.getId(), passwordUpdateRequestDTO.getHashedPassword())
                .compose(v -> sessionDao.logoutAllSessions(mongoClient, user.getId()));
    }

    public Future<Void> deleteScheduleAccount(PgPool pgPool) {
        return UtilMethods.nonBlockingWhile(() -> deleteScheduleAccountBatch(pgPool),
                        size -> size < config.getInteger(Constants.DELETION_CRON_BATCH_SIZE))
                .compose(s -> Future.succeededFuture());
    }

    private Future<Integer> deleteScheduleAccountBatch(PgPool pgPool) {
        return pgPool.withTransaction(sqlConnection -> userDao.findAccountsScheduledForDeletion(sqlConnection,
                        config.getInteger(Constants.DELETION_CRON_BATCH_SIZE)
                )
                .compose(users -> {
                    if (users.isEmpty()) {
                        return Future.succeededFuture(0);
                    }
                    OffsetDateTime currentTime = OffsetDateTime.now();
                    for (User user : users) {
                        user.setAccountStatus(AccountStatus.DELETED);
                        if (user.getMeta() == null) {
                            user.setMeta(new JsonObject());
                        }
                        user.getMeta().put(ModelConstants.EMAIL, user.getEmail());
                        user.setEmail(UUID.randomUUID().toString());
                        user.setDeleted(true);
                        user.setTimeLastModified(currentTime);
                    }
                    return userDao.updateUserAccountsAsDeleted(sqlConnection, users)
                            .map(users.size());
                }));
    }
}
