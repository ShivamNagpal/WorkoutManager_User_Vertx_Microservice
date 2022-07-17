package com.nagpal.shivam.workout_manager_user.exceptions.handlers;

import com.nagpal.shivam.workout_manager_user.dtos.response.ResponseWrapper;
import com.nagpal.shivam.workout_manager_user.enums.ResponseMessage;
import com.nagpal.shivam.workout_manager_user.exceptions.ResponseException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;


public class GlobalExceptionHandler {
    private static final Logger logger = Logger.getLogger(GlobalExceptionHandler.class.getName());

    private GlobalExceptionHandler() {
    }

    public static void handle(Throwable throwable, HttpServerResponse httpServerResponse) {
        logger.log(Level.SEVERE, throwable.getMessage(), throwable);
        if (throwable instanceof ResponseException) {
            ResponseException responseException = (ResponseException) throwable;
            Object responseExceptionPayload = responseException.getPayload();
            if (Objects.nonNull(responseExceptionPayload)) {
                String message = ResponseMessage.RESPONSE_EXCEPTION_PAYLOAD.getMessage(
                        Json.encodePrettily(responseExceptionPayload));
                logger.log(Level.SEVERE, message);
            }
            ResponseWrapper<Object> failureResponseWrapper =
                    ResponseWrapper.failure(responseExceptionPayload, responseException.getMessageCode(),
                            responseException.getMessage()
                    );
            httpServerResponse.setStatusCode(responseException.getStatus())
                    .end(Json.encodePrettily(failureResponseWrapper));
        } else {
            ResponseMessage responseMessage = ResponseMessage.INTERNAL_SERVER_ERROR;
            ResponseWrapper<Object> failureResponseWrapper =
                    ResponseWrapper.failure(null, responseMessage.getMessageCode(), responseMessage.getMessage());
            httpServerResponse.setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .end(Json.encodePrettily(failureResponseWrapper));
        }
    }
}
