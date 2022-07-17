package com.nagpal.shivam.workout_manager_user.exceptions;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final String messageCode;

    public AppException(String messageCode, String message) {
        super(message);
        this.messageCode = messageCode;
    }
}
