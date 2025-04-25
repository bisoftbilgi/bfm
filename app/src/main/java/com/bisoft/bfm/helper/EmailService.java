package com.bisoft.bfm.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${bfm.notification-mail-receivers:redmine@bisoft.com.tr}")
    private String notification_mail_receivers;

    @Value("${bfm.notification-mail-sender:info@bisoft.com.tr}")
    private String mailSenderAddress;

    private final JavaMailSender javaMailSender;

    public EmailService(@Autowired(required = false) JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Async
    public void sendMail(String subject, String message) {
        if (javaMailSender == null) {
            System.out.println("The mailer is not configured. Mail sending is skipped.");
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(mailSenderAddress);

            if (notification_mail_receivers.contains(",")) {
                mailMessage.setTo(notification_mail_receivers.split(","));
            } else {
                mailMessage.setTo(notification_mail_receivers);
            }

            mailMessage.setSubject(subject);
            mailMessage.setText(message);
            javaMailSender.send(mailMessage);
        } catch (Exception e) {
            System.err.println("Failed to send mail: " + e.getMessage());
        }
    }

    @Async
    public void sendMailWithAttachment(ArrayList<String> mailTOList, String subject, String text, ArrayList<String> pathToAttachmentList) {
        if (javaMailSender == null) {
            System.out.println("📭 Mail sender not configured. Attachment mail skipped.");
            return;
        }

        if (mailTOList == null || mailTOList.isEmpty()) {
            System.out.println("No recipient provided. Attachment mail skipped.");
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(mailSenderAddress);
            helper.setTo(mailTOList.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(text);

            if (pathToAttachmentList != null) {
                for (String path : pathToAttachmentList) {
                    File file = new File(path);
                    FileSystemResource resource = new FileSystemResource(file);
                    helper.addAttachment(Objects.requireNonNull(resource.getFilename()), resource);
                }
            }

            javaMailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send mail with attachment: " + e.getMessage());
        }
    }
}
