package com.nagpal.shivam.workout_manager_user.utils;

import com.nagpal.shivam.workout_manager_user.enums.ResponseMessage;
import com.nagpal.shivam.workout_manager_user.exceptions.ResponseException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;
import java.util.Objects;

public class RequestValidationUtils {

    private RequestValidationUtils() {
    }

    public static Future<JsonObject> fetchBodyAsJson(RoutingContext routingContext) {
        JsonObject bodyAsJson = routingContext.getBodyAsJson();
        if (Objects.isNull(bodyAsJson)) {
            ResponseMessage responseMessage = ResponseMessage.REQUEST_BODY_NOT_PROVIDED;
            return Future.failedFuture(new ResponseException(HttpResponseStatus.BAD_REQUEST.code(),
                    responseMessage.getMessageCode(), responseMessage.getMessage(), null));
        }
        return Future.succeededFuture(bodyAsJson);
    }

    public static void validateNotNull(JsonObject requestBody, String key, Map<String, String> errors) {
        Object value = requestBody.getValue(key);
        if (Objects.isNull(value)) {
            errors.put(key, ResponseMessage.MUST_NOT_BE_A_NULL_VALUE.getMessage());
        }
    }

    public static void validateNotBlank(JsonObject requestBody, String key, Map<String, String> errors) {
        String value = requestBody.getString(key);
        if (Objects.isNull(value) || value.isBlank()) {
            errors.put(key, ResponseMessage.MUST_NOT_BE_BLANK.getMessage());
        }
    }

    public static <T> Future<T> formErrorResponse(Map<String, String> errors) {
        ResponseMessage responseMessage = ResponseMessage.VALIDATION_ERRORS_IN_THE_REQUEST;
        return Future.failedFuture(new ResponseException(HttpResponseStatus.BAD_REQUEST.code(),
                responseMessage.getMessageCode(), responseMessage.getMessage(), JsonObject.mapFrom(errors)));
    }

    public static Future<Boolean> getBooleanQueryParam(HttpServerRequest request, String key) {
        String paramVal = request.getParam(key);
        if (paramVal == null) {
            return Future.succeededFuture(null);
        }
        if (paramVal.equalsIgnoreCase(Constants.TRUE)) {
            return Future.succeededFuture(true);
        } else if (paramVal.equalsIgnoreCase(Constants.FALSE)) {
            return Future.succeededFuture(false);
        }
        ResponseMessage responseMessage = ResponseMessage.QUERY_PARAM_MUST_HAVE_ONLY_BOOLEAN_VALUES;
        return Future.failedFuture(new ResponseException(HttpResponseStatus.BAD_REQUEST.code(),
                responseMessage.getMessageCode(), responseMessage.getMessage(key), null
        ));
    }

    public static Future<Boolean> getBooleanQueryParam(HttpServerRequest request, String key, boolean defaultValue) {
        return getBooleanQueryParam(request, key)
                .map(value -> Objects.requireNonNullElse(value, defaultValue));
    }
}
