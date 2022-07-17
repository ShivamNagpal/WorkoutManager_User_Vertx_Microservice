package com.nagpal.shivam.workout_manager_user.services.impl;

import com.nagpal.shivam.workout_manager_user.enums.Configuration;
import com.nagpal.shivam.workout_manager_user.enums.ResponseMessage;
import com.nagpal.shivam.workout_manager_user.services.EmailService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;

public class EmailServiceImpl implements EmailService {
    private final MailClient mailClient;
    private final JsonObject config;

    public EmailServiceImpl(MailClient mailClient, JsonObject config) {
        this.mailClient = mailClient;
        this.config = config;
    }

    @Override
    public Future<MailResult> sendOTPEmail(String emailId, int otp) {
        String mailContent = ResponseMessage.OTP_EMAIL_CONTENT_FORMAT.getMessage(otp);
        MailMessage mailMessage = new MailMessage()
                .setTo(emailId)
                .setFrom(config.getString(Configuration.MAIL_USERNAME.getKey()))
                .setSubject(ResponseMessage.OTP_EMAIL_SUBJECT.getMessage())
                .setText(mailContent);
        return mailClient.sendMail(mailMessage);
    }
}
