package com.qlphongtro.service;

public class OtpDeliveryException extends RuntimeException {
    public OtpDeliveryException() {
        super("Password reset email delivery failed.");
    }
}
