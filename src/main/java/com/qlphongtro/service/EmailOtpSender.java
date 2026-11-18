package com.qlphongtro.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev")
public class EmailOtpSender implements OtpSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailOtpSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(String recipient, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("Password reset verification code");
        message.setText("Your password reset code is " + otp + ". It expires in 10 minutes.");

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new OtpDeliveryException();
        }
    }
}
