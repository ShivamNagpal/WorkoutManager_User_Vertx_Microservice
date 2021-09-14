package com.nagpal.shivam.workout_manager_user.dtos.request;

import com.nagpal.shivam.workout_manager_user.utils.RequestConstants;
import com.nagpal.shivam.workout_manager_user.utils.RequestValidationUtils;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;

@Getter
@Setter
@NoArgsConstructor
public class RefreshSessionRequestDTO {
    private String refreshToken;

    public static Future<RefreshSessionRequestDTO> fromRequest(JsonObject body) {
        HashMap<String, String> errors = new HashMap<>();

        RequestValidationUtils.validateNotBlank(body, RequestConstants.REFRESH_TOKEN, errors);

        if (!errors.isEmpty()) {
            return RequestValidationUtils.formErrorResponse(errors);
        }

        RefreshSessionRequestDTO refreshSessionRequestDTO = new RefreshSessionRequestDTO();
        refreshSessionRequestDTO.setRefreshToken(body.getString(RequestConstants.REFRESH_TOKEN));
        return Future.succeededFuture(refreshSessionRequestDTO);
    }
}
