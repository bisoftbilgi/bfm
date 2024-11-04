package com.bisoft.bfm.helper;

import java.io.File;
import java.util.ArrayList;

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
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${bfm.notification-mail-receivers:redmine@bisoft.com.tr}")
    public String notification_mail_receivers;

    @Autowired
    private final JavaMailSender javaMailSender;

    @Async
    public void sendMail(String subject, String message){
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        if (notification_mail_receivers.contains(",")){
            mailMessage.setTo(notification_mail_receivers.split(","));
        } else {
            mailMessage.setTo(notification_mail_receivers);
        }        
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        //mailMessage.setFrom("bfm.reporter@bisoft.com.tr");
        javaMailSender.send(mailMessage);
    }

    @Async 
    public void sendMailWithAttachment(ArrayList<String> mailTOList, String subject, String text, ArrayList<String> pathToAttachmentList) throws MessagingException {
        
        MimeMessage message = javaMailSender.createMimeMessage();
        
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setFrom("bfm.reporter@bisoft.com.tr");
        for (String mailTO : mailTOList){
            helper.setTo(mailTO);
        }        
        helper.setSubject(subject);
        helper.setText(text);        
        for (String pathToAttachment : pathToAttachmentList){
            FileSystemResource file = new FileSystemResource(new File(pathToAttachment));
            helper.addAttachment(file.getFilename(), file);    
        }

        javaMailSender.send(message);
    }
}
