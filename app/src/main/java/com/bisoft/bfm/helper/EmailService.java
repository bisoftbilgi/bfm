package com.bisoft.bfm.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

@Service
public class EmailService {

    public String notificationMailReceivers;
    public String notificationMailSender;

    private final JavaMailSender javaMailSender;

    public EmailService(@Autowired(required = false) JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;

        
        Map<String, String> env = System.getenv();
        this.notificationMailReceivers = env.getOrDefault("NOTIFICATION_MAIL_RECEIVERS", "admin@example.com");
        this.notificationMailSender = env.getOrDefault("NOTIFICATION_MAIL_SENDER", "noreply@example.com");
    }

    @Async
    public void sendMail(String subject, String message) {
        if (javaMailSender == null) {
            System.out.println("The mailer is not configured. Mail sending is skipped.");
            return;
        }

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        if (notificationMailReceivers.contains(",")) {
            mailMessage.setTo(notificationMailReceivers.split(","));
        } else {
            mailMessage.setTo(notificationMailReceivers);
        }
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        javaMailSender.send(mailMessage);
    }

    @Async
    public void sendMailWithAttachment(ArrayList<String> mailTOList, String subject, String text, ArrayList<String> pathToAttachmentList) throws MessagingException {
        if (javaMailSender == null) {
            System.out.println("There is no e-mail sender, e-mail with attachment is skipped.");
            return;
        }

        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(notificationMailSender);
        for (String mailTO : mailTOList) {
            helper.setTo(mailTO);
        }
        helper.setSubject(subject);
        helper.setText(text);

        for (String path : pathToAttachmentList) {
            FileSystemResource file = new FileSystemResource(new File(path));
            helper.addAttachment(Objects.requireNonNull(file.getFilename()), file);
        }

        javaMailSender.send(message);
    }
}
