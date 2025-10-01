package com.bisoft.bfm.helper;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${bfm.notification-mail-receivers:redmine@bisoft.com.tr}")
    public String notification_mail_receivers;

    @Value("${bfm.user-mailx:false}")
    public boolean use_mailx;

    // @Autowired
    // private final JavaMailSender javaMailSender;


    @Async
    public void sendMail(String subject, String message){
        if (use_mailx == Boolean.TRUE){
            JavaMailSender javaMailSender = new JavaMailSenderImpl();
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
        } else {
            try {
                String[] cmd = {
                    "/bin/sh", "-c",
                    "echo \"" + message + "\" | mailx -s \"" + subject + "\" " + notification_mail_receivers.replace(",", " ")
                };

                Process process = Runtime.getRuntime().exec(cmd);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("mailx result:" + line);
                }
                BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()));
                while ((line = errorReader.readLine()) != null) {
                    log.info("mailx error:" + line);
                }

                int exitCode = process.waitFor();
                log.info("mailx exit code: " + exitCode);

            } catch (Exception e) {
                e.printStackTrace();
            }            
        }
        
    }

    // @Async 
    // public void sendMailWithAttachment(ArrayList<String> mailTOList, String subject, String text, ArrayList<String> pathToAttachmentList) throws MessagingException {
        
    //     MimeMessage message = javaMailSender.createMimeMessage();
        
    //     MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
    //     helper.setFrom("bfm.reporter@bisoft.com.tr");
    //     for (String mailTO : mailTOList){
    //         helper.setTo(mailTO);
    //     }        
    //     helper.setSubject(subject);
    //     helper.setText(text);        
    //     for (String pathToAttachment : pathToAttachmentList){
    //         FileSystemResource file = new FileSystemResource(new File(pathToAttachment));
    //         helper.addAttachment(file.getFilename(), file);    
    //     }

    //     javaMailSender.send(message);
    // }
}
