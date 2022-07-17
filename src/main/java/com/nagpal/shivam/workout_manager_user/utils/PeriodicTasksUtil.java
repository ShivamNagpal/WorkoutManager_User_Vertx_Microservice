package com.nagpal.shivam.workout_manager_user.utils;

import com.nagpal.shivam.workout_manager_user.configurations.DatabaseConfiguration;
import com.nagpal.shivam.workout_manager_user.daos.SessionDao;
import com.nagpal.shivam.workout_manager_user.daos.UserDao;
import com.nagpal.shivam.workout_manager_user.daos.impl.SessionDaoImpl;
import com.nagpal.shivam.workout_manager_user.daos.impl.UserDaoImpl;
import com.nagpal.shivam.workout_manager_user.enums.ResponseMessage;
import com.nagpal.shivam.workout_manager_user.helpers.UserHelper;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PeriodicTasksUtil {
    private static final Logger logger = Logger.getLogger(PeriodicTasksUtil.class.getName());

    private PeriodicTasksUtil() {
    }

    public static Future<Void> setupPeriodicTasks(Vertx vertx, JsonObject config) {
        UserDao userDao = new UserDaoImpl(config);
        SessionDao sessionDao = new SessionDaoImpl();
        UserHelper userHelper = new UserHelper(config, userDao, sessionDao);
        PgPool pgPool = DatabaseConfiguration.getSqlClient(vertx, config);

        setupPeriodicTasks(vertx, config, pgPool, userHelper);
        return Future.succeededFuture();
    }

    private static void setupPeriodicTasks(Vertx vertx, JsonObject config, PgPool pgPool, UserHelper userHelper) {
        setupDeletionCron(vertx, config, pgPool, userHelper);
    }

    private static void setupDeletionCron(Vertx vertx, JsonObject config, PgPool pgPool, UserHelper userHelper) {
        vertx.setPeriodic(config.getLong(Constants.DELETION_CRON_DELAY), id -> userHelper.deleteScheduleAccount(pgPool)
                .onSuccess(v -> logger.log(Level.INFO, ResponseMessage.DELETION_CRON_EXECUTED_SUCCESSFULLY.getMessage()
                ))
                .onFailure(throwable -> logger.log(Level.SEVERE, ResponseMessage.DELETION_CRON_FAILED.getMessage(),
                        throwable
                ))
        );
    }
}
