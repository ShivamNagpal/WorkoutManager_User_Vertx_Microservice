package com.nagpal.shivam.workout_manager_user.utils;

import com.nagpal.shivam.workout_manager_user.enums.ResponseMessage;
import com.nagpal.shivam.workout_manager_user.exceptions.ResponseException;
import com.nagpal.shivam.workout_manager_user.services.JWTService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;

public class AuthenticationUtils {

    private AuthenticationUtils() {
    }

    public static Future<Void> authenticate(HttpServerRequest request, JWTService jwtService) {
        String path = request.path();
        if (RoutingConstants.PUBLIC_ROUTES.contains(path)) {
            return Future.succeededFuture();
        }
        String authorizationValue = request.getHeader(Constants.AUTHORIZATION);
        if (authorizationValue == null) {
            ResponseMessage responseMessage = ResponseMessage.AUTHORIZATION_HEADER_NOT_PROVIDED;
            return Future.failedFuture(new ResponseException(HttpResponseStatus.BAD_REQUEST.code(),
                    responseMessage.getMessageCode(), responseMessage.getMessage(), null
            ));
        }
        if (!authorizationValue.startsWith(Constants.BEARER_)) {

            ResponseMessage responseMessage =
                    ResponseMessage.AUTHORIZATION_TOKEN_MUST_START_WITH_THE_BEARER;
            return Future.failedFuture(new ResponseException(HttpResponseStatus.BAD_REQUEST.code(),
                    responseMessage.getMessageCode(), responseMessage.getMessage(), null
            ));
        }
        String authToken = authorizationValue.substring(7);
        return jwtService.verifyAuthToken(authToken);
    }

    public static String getAuthToken(HttpServerRequest request) {
        return request.getHeader(Constants.AUTHORIZATION).substring(7);
    }
}
