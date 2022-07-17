package com.nagpal.shivam.workout_manager_user.exceptions;

import lombok.Getter;

@Getter
public class ResponseException extends AppException {
    private final int status;
    private final transient Object payload;

    public ResponseException(int status, String messageCode, String message, Object payload) {
        super(messageCode, message);
        this.status = status;
        this.payload = payload;
    }
}
