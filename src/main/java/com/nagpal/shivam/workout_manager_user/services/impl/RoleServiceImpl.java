package com.nagpal.shivam.workout_manager_user.services.impl;

import com.nagpal.shivam.workout_manager_user.daos.RoleDao;
import com.nagpal.shivam.workout_manager_user.enums.ResponseMessage;
import com.nagpal.shivam.workout_manager_user.enums.RoleName;
import com.nagpal.shivam.workout_manager_user.exceptions.ResponseException;
import com.nagpal.shivam.workout_manager_user.helpers.UserHelper;
import com.nagpal.shivam.workout_manager_user.models.Role;
import com.nagpal.shivam.workout_manager_user.services.RoleService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;

public class RoleServiceImpl implements RoleService {
    private final PgPool pgPool;
    private final RoleDao roleDao;
    private final UserHelper userHelper;

    public RoleServiceImpl(PgPool pgPool, RoleDao roleDao, UserHelper userHelper) {
        this.pgPool = pgPool;
        this.roleDao = roleDao;
        this.userHelper = userHelper;
    }

    @Override
    public Future<Void> assignManagerRole(Long userId) {
        return pgPool.withTransaction(sqlConnection -> userHelper.getUserById(sqlConnection, userId)
                .compose(user -> roleDao.fetchRoleByUserIdAndRoleName(sqlConnection, userId, RoleName.MANAGER))
                .compose(roleOptional -> {
                    if (roleOptional.isEmpty()) {
                        return roleDao.insertRole(sqlConnection, userId, RoleName.MANAGER)
                                .compose(id -> Future.succeededFuture());
                    }
                    Role role = roleOptional.get();
                    if (role.getDeleted() != null && !role.getDeleted()) {
                        ResponseMessage responseMessage = ResponseMessage.USER_IS_ALREADY_A_MANAGER;
                        return Future.failedFuture(new ResponseException(HttpResponseStatus.BAD_REQUEST.code(),
                                responseMessage.getMessageCode(), responseMessage.getMessage(), null
                        ));
                    }
                    return roleDao.updateRoleDeletedStatus(sqlConnection, role.getId(), false);
                })
        );
    }

    @Override
    public Future<Void> unAssignManagerRole(Long userId) {
        return pgPool.withTransaction(sqlConnection -> userHelper.getUserById(sqlConnection, userId)
                .compose(user -> roleDao.fetchRoleByUserIdAndRoleName(sqlConnection, userId, RoleName.MANAGER))
                .compose(roleOptional -> {
                    if (roleOptional.isEmpty()) {
                        ResponseMessage responseMessage = ResponseMessage.USER_IS_NOT_A_MANAGER;
                        return Future.failedFuture(new ResponseException(HttpResponseStatus.BAD_REQUEST.code(),
                                responseMessage.getMessageCode(), responseMessage.getMessage(), null
                        ));
                    }
                    Role role = roleOptional.get();
                    if (role.getDeleted() != null && role.getDeleted()) {
                        ResponseMessage responseMessage = ResponseMessage.USER_IS_NOT_A_MANAGER;
                        return Future.failedFuture(new ResponseException(HttpResponseStatus.BAD_REQUEST.code(),
                                responseMessage.getMessageCode(), responseMessage.getMessage(), null
                        ));
                    }
                    return roleDao.updateRoleDeletedStatus(sqlConnection, role.getId(), true);
                })
        );
    }
}
