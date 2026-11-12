package com.qlphongtro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevLogOtpSender implements OtpSender {

    private static final Logger logger = LoggerFactory.getLogger(DevLogOtpSender.class);

    @Override
    public void send(String recipient, String otp) {
        logger.info("Password reset email delivery requested in dev profile; delivery is disabled.");
    }
}
