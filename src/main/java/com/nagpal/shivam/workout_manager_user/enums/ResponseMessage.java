package com.nagpal.shivam.workout_manager_user.enums;

import com.nagpal.shivam.workout_manager_user.utils.Constants;
import lombok.Getter;

import java.text.MessageFormat;

@Getter
public enum ResponseMessage {

    // Information Response Messages
    SERVER_STARTED_ON_PORT("I-001", "Server started on port: {0}"),
    SUCCESSFULLY_DEPLOYED_THE_VERTICLES("I-002", "Successfully deployed the Verticles"),
    STARTING_VERTICLE("I-003", "Starting verticle \"{0}\""),
    SHUTTING_DOWN_THE_VERT_X("I-004", "Shutting down the Vert.x"),
    SUCCESSFULLY_CREATED_SQL_CLIENT_INSTANCE("I-005", "Successfully created SqlClient instance"),
    VERTX_ACTIVE_PROFILES("I-006", "Vertx active profiles: {0}"),
    OTP_EMAIL_SUBJECT("I-007", "OTP From Workout Manager"),
    OTP_EMAIL_CONTENT_FORMAT("I-008", "Please use this OTP {0}"),

    // Error Response Messages
    DUPLICATE_RESPONSE_CODES("E-000", "Duplicate response codes defined: {0}"),
    MANDATORY_CONFIGS_ARE_NOT_FOUND("E-001", "Mandatory Configs are not found: {0}"),
    DUPLICATE_CONFIG_KEYS_PROVIDED("E-002", "Duplicate config keys provided: {0}"),
    RESPONSE_EXCEPTION_PAYLOAD("E-003", "ResponseException Payload: {0}"),
    MUST_NOT_BE_A_NULL_VALUE("E-004", "Must not be a null value"),
    MUST_NOT_BE_BLANK("E-005", "Must not be blank"),
    VALIDATION_ERRORS_IN_THE_REQUEST("E-006", "Validation errors in the request"),
    REQUEST_BODY_NOT_PROVIDED("E-007", "Request Body not provided"),
    PG_POOL_HEALTH_CHECK_FAILED("E-008", "PG Pool Health Check Failed"),
    MONGO_CLIENT_HEALTH_CHECK_FAILED("E-009", "Mongo Client Health Check Failed"),
    OTP_TOKEN_NOT_PROVIDED("E-010", "OTP Token not provided"),
    INVALID_OTP_TOKEN("E-011", "Invalid OTP Token"),
    OTP_RESEND_LIMIT_EXCEEDED("E-012", "OTP Resend Limit exceeded"),
    NO_ACTIVE_TRIGGERED_OTP_FOUND("E-013", "No active triggered otp found"),
    INCORRECT_OTP("E-014", "Incorrect OTP"),
    POST_VERIFICATION_ACTION_NOT_MAPPED_FOR_THE_OTP_PURPOSE("E-015",
            "Post verification action not mapped for the OTP Purpose: {0}"),
    INVALID_AUTH_TOKEN("E-016", "Invalid Auth Token"),
    USER_NOT_FOUND("E-017", "User not found"),
    INVALID_CREDENTIALS("E-018", "Invalid Credentials"),
    USER_ACCOUNT_IS_NOT_ACTIVE("E-019", "User account is not active"),
    USER_ACCOUNT_IS_UNVERIFIED("E-020", "User account is unverified"),
    INVALID_REFRESH_TOKEN("E-021", "Invalid refresh token"),
    SESSION_HAS_EXPIRED("E-022", "Session has expired"),
    SESSION_IS_NOT_ACTIVE("E-023", "Session is not active"),
    AUTHORIZATION_TOKEN_MUST_START_WITH_THE_BEARER("E-024",
            "Authorization token must start with the Bearer"),
    AUTHORIZATION_HEADER_NOT_PROVIDED("E-025", "Authorization header not provided"),
    QUERY_PARAM_MUST_HAVE_ONLY_BOOLEAN_VALUES("E-026",
            "Query param: {0} must have only boolean values"),
    NEW_EMAIL_CANNOT_BE_SAME_AS_THE_OLD_EMAIL("E-027", "New email cannot be same as the old email"),
    NEW_PASSWORD_CANNOT_BE_SAME_AS_THE_OLD_PASSWORD("E-028",
            "New password cannot be same as the old password"),
    USER_ACCOUNT_WASN_T_DEACTIVATED_OR_MARKED_FOR_DELETION("E-029",
            "User account wasn't deactivated or marked for deletion"),
    USER_IS_NOT_AUTHORIZED_TO_ACCESS("E-030", "User is not authorized to access"),
    USER_IS_ALREADY_A_MANAGER("E-031", "User is already a manager"),
    USER_IS_NOT_A_MANAGER("E-032", "User is not a Manager"),
    DELETION_CRON_FAILED("E-033", "Deletion Cron failed"),
    DELETION_CRON_EXECUTED_SUCCESSFULLY("E-034", "Deletion Cron executed successfully"),
    UNIQUE_USER_CONSTRAINT_VIOLATION("E-035", "User with {0}"),
    INTERNAL_SERVER_ERROR("E-036", "Internal Server Error"),
    ;

    private final String messageCode;
    private final String message;

    ResponseMessage(String messageCode, String message) {
        this.messageCode = Constants.RESPONSE_MESSAGE_PREFIX + messageCode;
        this.message = message;
    }

    public String getMessage(Object... variables) {
        if (variables == null) {
            return message;
        }
        return MessageFormat.format(message, variables);
    }
}
