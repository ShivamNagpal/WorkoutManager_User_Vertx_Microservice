package com.nagpal.shivam.workout_manager_user.utils;

public class Constants {
    public static final String PATH = "path";
    public static final String SYS = "sys";
    public static final String ENV = "env";
    public static final String FILE = "file";
    public static final String CONFIG_JSON_PATH = "config.json";
    public static final String CONFIG_PROFILE_JSON_PATH = "config-{0}.json";
    public static final String SELECT_1 = "SELECT 1;";
    public static final String UP = "UP";
    public static final String DOWN = "DOWN";
    public static final String ENV_KEY_VERTX_PROFILES_ACTIVE = "VERTX_PROFILES_ACTIVE";
    public static final String PROFILES_SEPARATOR_REGEX_PATTERN = "\\s*,\\s*";
    public static final String JDBC_POSTGRESQL_URI = "jdbc:postgresql://{0}:{1}/{2}?useSSL=false";
    public static final String AVAILABLE_PROCESSORS = "AVAILABLE_PROCESSORS";
    public static final String MAIL_HOST = "mail.host";
    public static final String MAIL_PORT = "mail.port";
    public static final String CONNECTION_STRING = "connection_string";
    public static final String DB_NAME = "db_name";
    public static final String PING = "ping";
    public static final String SESSION = "session";
    public static final String USER_ID = "userId";
    public static final String APPLICATION_JSON = "application/json";
    public static final String CONTENT_TYPE = "content-type";
    public static final String OTP_RETRY_LIMIT = "otp.retry.limit";
    public static final String OTP_BACKOFF_TIME = "otp.backoff.time";
    public static final String OTP_EXPIRY_TIME = "otp.expiry.time";
    public static final String BCRYPT_PASSWORD_LOG_ROUNDS = "bcrypt.password.log.rounds";
    public static final String BCRYPT_OTP_LOG_ROUNDS = "bcrypt.otp.log.rounds";
    public static final String EMAIL = "email";
    public static final String OTP_PURPOSE = "otpPurpose";
    public static final String ISSUER_WORKOUT_MANAGER = "WORKOUT_MANAGER";
    public static final String OTP_TOKEN = "otp-token";
    public static final String SESSION_ID = "sessionId";
    public static final String ROLES = "roles";
    public static final String JWT_EXPIRY_TIME = "jwt.expiry.time";
    public static final String RSA = "RSA";
    public static final String SESSION_EXPIRY_TIME = "session.expiry.time";
    public static final String REFRESH_TOKEN_SEPARATOR = "-";
    public static final String DOLLAR_SET = "$set";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER_ = "Bearer ";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final String DELETION_SELECT_PERIOD_IN_DAYS = "deletion.cron.select.period.in.days";
    public static final String DELETION_CRON_DELAY = "deletion.cron.delay";
    public static final String DELETION_CRON_BATCH_SIZE = "deletion.cron.batch.size";
    public static final String RESPONSE_MESSAGE_PREFIX = "WM-U-";

    private Constants() {
    }
}
