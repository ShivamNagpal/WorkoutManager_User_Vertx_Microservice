package com.nagpal.shivam.workout_manager_user.services.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nagpal.shivam.workout_manager_user.dtos.internal.JWTAuthTokenDTO;
import com.nagpal.shivam.workout_manager_user.dtos.internal.JWTOTPTokenDTO;
import com.nagpal.shivam.workout_manager_user.enums.Configuration;
import com.nagpal.shivam.workout_manager_user.enums.OTPPurpose;
import com.nagpal.shivam.workout_manager_user.enums.ResponseMessage;
import com.nagpal.shivam.workout_manager_user.enums.RoleName;
import com.nagpal.shivam.workout_manager_user.exceptions.ResponseException;
import com.nagpal.shivam.workout_manager_user.services.JWTService;
import com.nagpal.shivam.workout_manager_user.utils.Constants;
import com.nagpal.shivam.workout_manager_user.utils.UtilMethods;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.Set;

public class JWTServiceImpl implements JWTService {

    private final JsonObject config;
    private final Algorithm otpSigningAlgorithm;
    private final Algorithm authTokenSigningAlgorithm;
    private final JWTVerifier otpTokenVerifier;
    private final JWTVerifier authTokenVerifier;

    public JWTServiceImpl(JsonObject config) {
        this.config = config;
        otpSigningAlgorithm = Algorithm.HMAC512(config.getString(Configuration.OTP_SECRET_TOKEN.getKey()));
        authTokenSigningAlgorithm = getRSAAlgorithm();
        otpTokenVerifier = JWT.require(otpSigningAlgorithm).withIssuer(Constants.ISSUER_WORKOUT_MANAGER).build();
        authTokenVerifier = JWT.require(authTokenSigningAlgorithm).withIssuer(Constants.ISSUER_WORKOUT_MANAGER).build();
    }

    @SneakyThrows
    private Algorithm getRSAAlgorithm() {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] privateKeyBytes = decoder.decode(config.getString(Configuration.AUTH_TOKEN_PRIVATE_KEY.getKey()));
        byte[] publicKeyBytes = decoder.decode(config.getString(Configuration.AUTH_TOKEN_PUBLIC_KEY.getKey()));
        KeyFactory keyFactory = KeyFactory.getInstance(Constants.RSA);
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        return Algorithm.RSA512(publicKey, privateKey);
    }

    @Override
    public String generateOTPToken(JWTOTPTokenDTO jwtotpTokenDTO) {
        return JWT.create()
                .withClaim(Constants.USER_ID, jwtotpTokenDTO.getUserId())
                .withClaim(Constants.EMAIL, jwtotpTokenDTO.getEmail())
                .withClaim(Constants.OTP_PURPOSE, jwtotpTokenDTO.getOtpPurpose().toString())
                .withExpiresAt(UtilMethods.convertLocalDateTimeToDate(
                        LocalDateTime.now().plusSeconds(config.getInteger(Constants.OTP_EXPIRY_TIME))
                ))
                .withIssuer(Constants.ISSUER_WORKOUT_MANAGER)
                .withIssuedAt(new Date())
                .sign(otpSigningAlgorithm);
    }

    @Override
    public Future<JWTOTPTokenDTO> verifyAndDecodeOTPToken(String otpToken) {
        DecodedJWT decodedJWT;
        try {
            decodedJWT = otpTokenVerifier.verify(otpToken);
        } catch (JWTVerificationException e) {
            ResponseMessage responseMessage = ResponseMessage.INVALID_OTP_TOKEN;
            return Future.failedFuture(
                    new ResponseException(HttpResponseStatus.BAD_REQUEST.code(), responseMessage.getMessageCode(),
                            responseMessage.getMessage(), null
                    )
            );
        }
        JWTOTPTokenDTO jwtotpTokenDTO = new JWTOTPTokenDTO();
        jwtotpTokenDTO.setUserId(decodedJWT.getClaim(Constants.USER_ID).asLong());
        jwtotpTokenDTO.setEmail(decodedJWT.getClaim(Constants.EMAIL).asString());
        jwtotpTokenDTO.setOtpPurpose(OTPPurpose.valueOf(decodedJWT.getClaim(Constants.OTP_PURPOSE).asString()));
        return Future.succeededFuture(jwtotpTokenDTO);
    }

    @Override
    public String generateAuthToken(JWTAuthTokenDTO jwtAuthTokenDTO) {
        return JWT.create()
                .withClaim(Constants.USER_ID, jwtAuthTokenDTO.getUserId())
                .withClaim(Constants.SESSION_ID, jwtAuthTokenDTO.getSessionId())
                .withArrayClaim(Constants.ROLES, jwtAuthTokenDTO.getRoles())
                .withExpiresAt(UtilMethods.convertLocalDateTimeToDate(
                        LocalDateTime.now().plusSeconds(config.getInteger(Constants.JWT_EXPIRY_TIME))
                ))
                .withIssuer(Constants.ISSUER_WORKOUT_MANAGER)
                .withIssuedAt(new Date())
                .sign(authTokenSigningAlgorithm);
    }

    @Override
    public Future<Void> verifyAuthToken(String authToken) {
        try {
            authTokenVerifier.verify(authToken);
            return Future.succeededFuture();
        } catch (JWTVerificationException e) {
            ResponseMessage responseMessage = ResponseMessage.INVALID_AUTH_TOKEN;
            return Future.failedFuture(
                    new ResponseException(HttpResponseStatus.BAD_REQUEST.code(), responseMessage.getMessageCode(),
                            responseMessage.getMessage(), null
                    )
            );
        }
    }

    @Override
    public JWTAuthTokenDTO decodeAuthToken(String authToken) {
        DecodedJWT decodedJWT = JWT.decode(authToken);
        JWTAuthTokenDTO jwtAuthTokenDTO = new JWTAuthTokenDTO();
        jwtAuthTokenDTO.setUserId(decodedJWT.getClaim(Constants.USER_ID).asLong());
        jwtAuthTokenDTO.setSessionId(decodedJWT.getClaim(Constants.SESSION_ID).asString());
        jwtAuthTokenDTO.setRoles(decodedJWT.getClaim(Constants.ROLES).asArray(String.class));
        return jwtAuthTokenDTO;
    }

    @Override
    public Future<Void> verifyRoles(JWTAuthTokenDTO jwtAuthTokenDTO, RoleName... roles) {
        Set<String> userRoles = Set.of(jwtAuthTokenDTO.getRoles());
        for (RoleName roleName : roles) {
            if (userRoles.contains(roleName.name())) {
                return Future.succeededFuture();
            }
        }
        ResponseMessage responseMessage = ResponseMessage.USER_IS_NOT_AUTHORIZED_TO_ACCESS;
        return Future.failedFuture(new ResponseException(HttpResponseStatus.UNAUTHORIZED.code(),
                responseMessage.getMessageCode(), responseMessage.getMessage(), null
        ));
    }
}
