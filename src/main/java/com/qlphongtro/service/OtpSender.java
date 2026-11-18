package com.qlphongtro.service;

public interface OtpSender {
    void send(String recipient, String otp);
}
