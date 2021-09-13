package com.nagpal.shivam.workout_manager_user.services.impl;

import com.nagpal.shivam.workout_manager_user.daos.OTPDao;
import com.nagpal.shivam.workout_manager_user.daos.RoleDao;
import com.nagpal.shivam.workout_manager_user.daos.UserDao;
import com.nagpal.shivam.workout_manager_user.dtos.internal.JWTOTPTokenDTO;
import com.nagpal.shivam.workout_manager_user.dtos.request.VerifyOTPRequestDTO;
import com.nagpal.shivam.workout_manager_user.dtos.response.OTPResponseDTO;
import com.nagpal.shivam.workout_manager_user.enums.OTPPurpose;
import com.nagpal.shivam.workout_manager_user.enums.OTPStatus;
import com.nagpal.shivam.workout_manager_user.enums.RoleName;
import com.nagpal.shivam.workout_manager_user.exceptions.ResponseException;
import com.nagpal.shivam.workout_manager_user.models.OTP;
import com.nagpal.shivam.workout_manager_user.services.EmailService;
import com.nagpal.shivam.workout_manager_user.services.JWTService;
import com.nagpal.shivam.workout_manager_user.services.OTPService;
import com.nagpal.shivam.workout_manager_user.services.SessionService;
import com.nagpal.shivam.workout_manager_user.utils.Constants;
import com.nagpal.shivam.workout_manager_user.utils.MessageConstants;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlClient;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.security.SecureRandom;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.util.Random;

public class OTPServiceImpl implements OTPService {
    private final JsonObject config;
    private final PgPool pgPool;
    private final MongoClient mongoClient;
    private final OTPDao otpDao;
    private final EmailService emailService;
    private final SessionService sessionService;
    private final JWTService jwtService;
    private final UserDao userDao;
    private final RoleDao roleDao;
    private final Random random;

    public OTPServiceImpl(JsonObject config, PgPool pgPool, MongoClient mongoClient, OTPDao otpDao,
                          EmailService emailService, SessionService sessionService, JWTService jwtService,
                          UserDao userDao, RoleDao roleDao) {
        this.config = config;
        this.pgPool = pgPool;
        this.mongoClient = mongoClient;
        this.otpDao = otpDao;
        this.emailService = emailService;
        this.sessionService = sessionService;
        this.jwtService = jwtService;
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.random = new SecureRandom();
    }

    @Override
    public Future<OTPResponseDTO> resendOTP(JWTOTPTokenDTO jwtotpTokenDTO) {
        return triggerEmailVerification(pgPool, jwtotpTokenDTO.getUserId(), jwtotpTokenDTO.getEmail(),
                jwtotpTokenDTO.getOtpPurpose())
                .map(newOtpToken -> {
                    OTPResponseDTO otpResponseDTO = new OTPResponseDTO();
                    otpResponseDTO.setOtpToken(newOtpToken);
                    return otpResponseDTO;
                });
    }

    @Override
    public Future<Object> verifyOTP(JWTOTPTokenDTO jwtotpTokenDTO, VerifyOTPRequestDTO verifyOTPRequestDTO) {
        return pgPool.withTransaction(sqlConnection -> otpDao.fetchActiveOTP(
                                sqlConnection,
                                jwtotpTokenDTO.getUserId(),
                                jwtotpTokenDTO.getEmail()
                        )
                        .compose(otpOptional -> {
                            if (otpOptional.isEmpty()) {
                                return Future.failedFuture(new ResponseException(HttpResponseStatus.NOT_ACCEPTABLE.code(),
                                        MessageConstants.NO_ACTIVE_TRIGGERED_OTP_FOUND, null
                                ));
                            }
                            OTP otp = otpOptional.get();
                            if (!BCrypt.checkpw(String.valueOf(verifyOTPRequestDTO.getOtp()), otp.getOtpHash())) {
                                return Future.failedFuture(new ResponseException(HttpResponseStatus.NOT_ACCEPTABLE.code(),
                                        MessageConstants.INCORRECT_OTP, null
                                ));
                            }
                            return Future.succeededFuture(otp);
                        })
                        .compose(otp -> otpDao.updateOTPStatus(sqlConnection, otp.getId(), OTPStatus.USED))
                        .compose(v -> {
                            Future<Object> actionPostOTPVerification;
                            switch (jwtotpTokenDTO.getOtpPurpose()) {
                                case VERIFY_USER:
                                    actionPostOTPVerification = userDao.activateUser(
                                                    sqlConnection,
                                                    jwtotpTokenDTO.getUserId()
                                            )
                                            .compose(v2 -> roleDao.insertUserRole(sqlConnection, jwtotpTokenDTO.getUserId()))
                                            .compose(id -> sessionService.createNewSessionAndFormLoginResponse(mongoClient,
                                                    jwtotpTokenDTO.getUserId(), new String[]{RoleName.USER.name()}
                                            ))
                                            .map(loginResponseDTO -> loginResponseDTO);
                                    break;
                                default:
                                    actionPostOTPVerification = Future.failedFuture(
                                            MessageFormat.format(
                                                    MessageConstants.POST_VERIFICATION_ACTION_NOT_MAPPED_FOR_THE_OTP_PURPOSE,
                                                    jwtotpTokenDTO.getOtpPurpose()
                                            )
                                    );
                            }
                            return actionPostOTPVerification;
                        })
        );
    }

    @Override
    public Future<String> triggerEmailVerification(SqlClient sqlClient, Long userId, String email,
                                                   OTPPurpose otpPurpose) {
        return otpDao.fetchAlreadyTriggeredOTP(sqlClient, userId, email)
                .compose(otpOptional -> {
                    int otpValue = generateOTP();
                    String otpHash = hashOTP(otpValue);
                    OffsetDateTime currentTime = OffsetDateTime.now();
                    Future<Integer> saveFuture;
                    if (otpOptional.isPresent()) {
                        OTP otp = otpOptional.get();
                        otp.setCount(otp.getCount() + 1);
                        otp.setOtpHash(otpHash);
                        otp.setValidAfter(currentTime);
                        if (otp.getCount() > config.getInteger(Constants.OTP_RETRY_LIMIT)) {
                            Future<Void> future = Future.succeededFuture();
                            if (otp.getOtpStatus() == OTPStatus.ACTIVE) {
                                int backOffMinutes = config.getInteger(Constants.OTP_BACKOFF_TIME) -
                                        config.getInteger(Constants.OTP_EXPIRY_TIME);
                                otp.setValidAfter(currentTime.plusSeconds(backOffMinutes));
                                otp.setOtpStatus(OTPStatus.OTP_RESEND_LIMIT_REACHED);
                                future = otpDao.update(sqlClient, otp);
                            }
                            return future.compose(v -> Future.failedFuture(
                                    new ResponseException(HttpResponseStatus.NOT_ACCEPTABLE.code(),
                                            MessageConstants.OTP_RESEND_LIMIT_EXCEEDED, null)));
                        }
                        saveFuture = otpDao.update(sqlClient, otp).map(otpValue);
                    } else {
                        OTP otp = new OTP();
                        otp.setUserId(userId);
                        otp.setEmail(email);
                        otp.setOtpHash(otpHash);
                        otp.setCount(1);
                        otp.setValidAfter(currentTime);
                        otp.setPurpose(otpPurpose);
                        otp.setDeleted(false);
                        otp.setTimeCreated(currentTime);
                        otp.setOtpStatus(OTPStatus.ACTIVE);
                        saveFuture = otpDao.insert(sqlClient, otp).map(otpValue);
                    }
                    return saveFuture;
                }).compose(otpValue -> {
                    emailService.sendOTPEmail(email, otpValue);
                    JWTOTPTokenDTO jwtotpTokenDTO = new JWTOTPTokenDTO();
                    jwtotpTokenDTO.setUserId(userId);
                    jwtotpTokenDTO.setEmail(email);
                    jwtotpTokenDTO.setOtpPurpose(otpPurpose);
                    String otpToken = jwtService.generateOTPToken(jwtotpTokenDTO);
                    return Future.succeededFuture(otpToken);
                });
    }

    private int generateOTP() {
        return random.nextInt(900000) + 100000;
    }

    private String hashOTP(int otp) {
        return BCrypt.hashpw(String.valueOf(otp), BCrypt.gensalt(config.getInteger(Constants.BCRYPT_OTP_LOG_ROUNDS)));
    }
}
