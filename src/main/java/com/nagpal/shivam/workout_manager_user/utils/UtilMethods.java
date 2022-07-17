package com.nagpal.shivam.workout_manager_user.utils;

import com.nagpal.shivam.workout_manager_user.enums.ResponseMessage;
import com.nagpal.shivam.workout_manager_user.exceptions.AppException;
import io.vertx.core.Future;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class UtilMethods {

    private UtilMethods() {
    }

    public static Date convertLocalDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static <T> Future<T> nonBlockingWhile(Supplier<Future<T>> supplier, Predicate<T> predicate) {
        return supplier.get()
                .compose(result -> {
                    if (predicate.test(result)) {
                        return Future.succeededFuture(result);
                    }
                    return nonBlockingWhile(supplier, predicate);
                });
    }

    public static void validateNonDuplicateResponseMessageCodes() {
        HashSet<String> messageCodes = new HashSet<>();
        ResponseMessage[] responseMessages = ResponseMessage.values();
        for (ResponseMessage responseMessage : responseMessages) {
            String messageCode = responseMessage.getMessageCode();
            if (messageCodes.contains(messageCode)) {
                ResponseMessage duplicateResponseMessage = ResponseMessage.DUPLICATE_RESPONSE_CODES;
                throw new AppException(duplicateResponseMessage.getMessageCode(),
                        duplicateResponseMessage.getMessage(messageCode));
            }
            messageCodes.add(messageCode);
        }
    }
}
