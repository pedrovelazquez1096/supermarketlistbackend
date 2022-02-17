package com.pvelazquez.supermarketlistbackend.Utilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service @RequiredArgsConstructor @Slf4j
public class EmailSender {
    private final JavaMailSender mailSender;

    @Async
    public void sendEmail(String toEmail, String subject, String body){
        log.info("Creating Mail");

        //log.info("Mail to {} Body: {}", toEmail, body);
        try{
            MimeMessage mimeMailMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMailMessage, "utf-8");
            helper.setText(body, true);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setFrom("supermarketlistbot@gmail.com");
            log.info("Senging mail");
            mailSender.send(mimeMailMessage);
            log.info("Mail sent");
        }catch(MessagingException e){
            throw new IllegalStateException("Failed to send email");
        }
    }

    public void sendSimpleEmail(String toEmail, String subject, String body){
        log.info("Creating Mail");

        log.info("Mail to {} Body: {}", toEmail, body);
        try{
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom("supermarketlistbot@gmail.com");
            message.setTo(toEmail);
            message.setText(body);
            message.setSubject(subject);
            log.info("Senging mail");
            mailSender.send(message);
            log.info("Mail sent");
        }catch(Exception e){
            throw new IllegalStateException("Failed to send email");
        }
    }
}
