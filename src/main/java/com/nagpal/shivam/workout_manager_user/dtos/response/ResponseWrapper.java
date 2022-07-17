package com.nagpal.shivam.workout_manager_user.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ResponseWrapper<T> {
    private boolean success;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T payload;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String messageCode;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    public static <T> ResponseWrapper<T> success(T payload, String messageCode, String message) {
        return new ResponseWrapper<>(true, payload, messageCode, message);
    }

    public static <T> ResponseWrapper<T> success(T payload) {
        return success(payload, null, null);
    }

    public static <T> ResponseWrapper<T> failure(T payload, String messageCode, String message) {
        return new ResponseWrapper<>(false, payload, messageCode, message);
    }

    public static <T> ResponseWrapper<T> failure(T payload) {
        return failure(payload, null, null);
    }
}
