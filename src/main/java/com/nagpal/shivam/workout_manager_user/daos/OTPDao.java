package com.nagpal.shivam.workout_manager_user.daos;

import com.nagpal.shivam.workout_manager_user.enums.OTPPurpose;
import com.nagpal.shivam.workout_manager_user.enums.OTPStatus;
import com.nagpal.shivam.workout_manager_user.models.OTP;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlClient;

import java.util.Optional;

public interface OTPDao {
    Future<Optional<OTP>> fetchAlreadyTriggeredOTP(SqlClient sqlClient, Long userId, String email,
                                                   OTPPurpose otpPurpose);

    Future<Optional<OTP>> fetchActiveOTP(SqlClient sqlClient, Long userId, String email,
                                         OTPPurpose otpPurpose);

    Future<Void> update(SqlClient sqlClient, OTP otp);

    Future<Long> insert(SqlClient sqlClient, OTP otp);

    Future<Void> updateOTPStatus(SqlClient sqlClient, Long otpId, OTPStatus otpStatus);
}
