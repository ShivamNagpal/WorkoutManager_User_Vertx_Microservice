package com.nagpal.shivam.workout_manager_user.services;

import com.nagpal.shivam.workout_manager_user.dtos.internal.JWTAuthTokenDTO;
import com.nagpal.shivam.workout_manager_user.dtos.internal.JWTOTPTokenDTO;
import com.nagpal.shivam.workout_manager_user.enums.RoleName;
import io.vertx.core.Future;

public interface JWTService {
    String generateOTPToken(JWTOTPTokenDTO jwtotpTokenDTO);

    Future<JWTOTPTokenDTO> verifyAndDecodeOTPToken(String otpToken);

    String generateAuthToken(JWTAuthTokenDTO jwtAuthTokenDTO);

    Future<Void> verifyAuthToken(String authToken);

    JWTAuthTokenDTO decodeAuthToken(String authToken);

    Future<Void> verifyRoles(JWTAuthTokenDTO jwtAuthTokenDTO, RoleName... roles);
}
