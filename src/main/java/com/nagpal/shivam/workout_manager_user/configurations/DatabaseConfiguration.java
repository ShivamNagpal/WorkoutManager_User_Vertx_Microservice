package com.nagpal.shivam.workout_manager_user.configurations;

import com.nagpal.shivam.workout_manager_user.enums.Configuration;
import com.nagpal.shivam.workout_manager_user.enums.ResponseMessage;
import com.nagpal.shivam.workout_manager_user.utils.Constants;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.flywaydb.core.Flyway;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConfiguration {
    private static final Logger logger = Logger.getLogger(DatabaseConfiguration.class.getName());
    private static volatile PgPool pgPool;

    private DatabaseConfiguration() {
    }

    public static PgPool getSqlClient(Vertx vertx, JsonObject config) {
        if (pgPool == null) {
            synchronized (DatabaseConfiguration.class) {
                if (pgPool == null) {
                    PgConnectOptions pgConnectOptions = new PgConnectOptions()
                            .setHost(config.getString(Configuration.PG_HOST.getKey()))
                            .setPort(config.getInteger(Configuration.PG_PORT.getKey()))
                            .setDatabase(config.getString(Configuration.PG_DATABASE.getKey()))
                            .setUser(config.getString(Configuration.PG_USERNAME.getKey()))
                            .setPassword(config.getString(Configuration.PG_PASSWORD.getKey()))
                            .setCachePreparedStatements(true);
                    PoolOptions poolOptions = new PoolOptions();
                    pgPool = PgPool.pool(vertx, pgConnectOptions, poolOptions);
                    logger.log(Level.INFO, ResponseMessage.SUCCESSFULLY_CREATED_SQL_CLIENT_INSTANCE.getMessage());
                }
            }
        }
        return pgPool;
    }

    public static MongoClient getMongoClient(Vertx vertx, JsonObject config) {
        return MongoClient.createShared(vertx, new JsonObject()
                .put(Constants.CONNECTION_STRING, config.getString(Configuration.MONGO_CONNECTION_URI.getKey()))
                .put(Constants.DB_NAME, config.getString(Configuration.MONGO_DATABASE.getKey()))
        );
    }

    public static void initFlyway(JsonObject config) {
        String jdbcUri = MessageFormat.format(Constants.JDBC_POSTGRESQL_URI,
                config.getString(Configuration.PG_HOST.getKey()),
                config.getString(Configuration.PG_PORT.getKey()),
                config.getString(Configuration.PG_DATABASE.getKey())
        );
        Flyway flyway =
                Flyway.configure().dataSource(
                        jdbcUri,
                        config.getString(Configuration.PG_USERNAME.getKey()),
                        config.getString(Configuration.PG_PASSWORD.getKey())
                ).load();
        flyway.migrate();
    }

    public static Future<Void> verifyMongoIndices(Vertx vertx, JsonObject config) {
        MongoClient mongoClient = getMongoClient(vertx, config);
        return mongoClient.createIndex(Constants.SESSION, new JsonObject().put(Constants.USER_ID, 1));
    }
}
