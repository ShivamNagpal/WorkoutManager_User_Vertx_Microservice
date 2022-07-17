package com.nagpal.shivam.workout_manager_user.dtos.internal;

import com.nagpal.shivam.workout_manager_user.enums.ResponseMessage;
import com.nagpal.shivam.workout_manager_user.exceptions.ResponseException;
import com.nagpal.shivam.workout_manager_user.utils.Constants;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SessionPayload {
    private String sessionId;
    private String refreshToken;

    public static Future<SessionPayload> fromRefreshToken(String refreshToken) {
        String[] split = refreshToken.split(Constants.REFRESH_TOKEN_SEPARATOR, 2);
        if (split.length != 2) {
            ResponseMessage responseMessage = ResponseMessage.INVALID_REFRESH_TOKEN;
            return Future.failedFuture(new ResponseException(HttpResponseStatus.BAD_REQUEST.code(),
                    responseMessage.getMessageCode(), responseMessage.getMessage(), null
            ));
        }
        SessionPayload sessionPayload = new SessionPayload();
        sessionPayload.setSessionId(split[0]);
        sessionPayload.setRefreshToken(split[1]);
        return Future.succeededFuture(sessionPayload);
    }

    public String createRefreshToken() {
        return sessionId + Constants.REFRESH_TOKEN_SEPARATOR + refreshToken;
    }
}
