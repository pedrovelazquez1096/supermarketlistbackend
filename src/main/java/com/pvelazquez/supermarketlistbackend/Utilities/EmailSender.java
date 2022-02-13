package com.pvelazquez.supermarketlistbackend.Utilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor @Slf4j
public class EmailSender {
    private final JavaMailSender mailSender;

    public void sendEmail(String toEmail, String subject, String body){
        log.info("Creating Mail");

        log.info("Mail to {} Body: {}", toEmail, body);
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom("supermarketlistbot@gmail.com");
        message.setTo(toEmail);
        message.setText(body);
        message.setSubject(subject);
        log.info("Senging mail");
        mailSender.send(message);
        log.info("Mail sent");
    }
}
