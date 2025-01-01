package com.bisoft.bfm.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.bisoft.bfm.ConfigurationManager;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Autowired
    private ConfigurationManager configurationManager;
    
    @Bean
    public JavaMailSender javaMailSender() {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        // mailSender.setProtocol("SMTP");
        mailSender.setHost("smtp.localdomain.com");  // SMTP sunucu adresi
        mailSender.setPort(587);  // SMTP portu

        // Kimlik doğrulama için kullanıcı adı ve şifre
        mailSender.setUsername("your-email@example.com");
        mailSender.setPassword("your-email-password");

        // Properties ayarlarını ekleyelim
        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.ssl.trust", "smtp.example.com");  // SSL güvenliği için

        return mailSender;

    }

    @Async
    public void sendMail(String subject, String message){
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        if (this.configurationManager.getConfiguration().getMailReceivers().contains(",")){
            mailMessage.setTo(this.configurationManager.getConfiguration().getMailReceivers().split(","));
        } else {
            mailMessage.setTo(this.configurationManager.getConfiguration().getMailReceivers());
        }        
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        //mailMessage.setFrom("bfm.reporter@bisoft.com.tr");
        // javaMailSender.send(mailMessage);
        javaMailSender().send(mailMessage);
    }

    @Async 
    public void sendMailWithAttachment(ArrayList<String> mailTOList, String subject, String text, ArrayList<String> pathToAttachmentList) throws MessagingException {
        
        // MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessage message = javaMailSender().createMimeMessage();
        
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

        // javaMailSender.send(message);
        javaMailSender().send(message);
    }
}



// import java.util.Properties;
// import javax.mail.Session;
// import javax.mail.Transport;
// import javax.mail.internet.InternetAddress;
// import javax.mail.internet.MimeMessage;

// public class SmtpTest {

//     public static void main(String[] args) {
//         // SMTP sunucu bilgileri
//         String host = "smtp.example.com";
//         String port = "587";  // Genellikle 587 (STARTTLS) veya 465 (SSL) kullanılır
//         String fromEmail = "your-email@example.com";
//         String toEmail = "recipient@example.com";
//         String username = "your-email@example.com";
//         String password = "your-email-password";

//         // Mail sunucusu özellikleri
//         Properties properties = new Properties();
//         properties.put("mail.smtp.host", host);
//         properties.put("mail.smtp.port", port);
//         properties.put("mail.smtp.auth", "true");
//         properties.put("mail.smtp.starttls.enable", "true");  // STARTTLS kullanılıyorsa

//         // Session oluştur
//         Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
//             protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
//                 return new javax.mail.PasswordAuthentication(username, password);
//             }
//         });

//         try {
//             // Bağlantıyı test et
//             Transport transport = session.getTransport("smtp");
//             transport.connect(host, username, password);
//             System.out.println("Bağlantı başarılı!");

//             // E-posta gönderme
//             MimeMessage message = new MimeMessage(session);
//             message.setFrom(new InternetAddress(fromEmail));
//             message.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(toEmail));
//             message.setSubject("Test Email");
//             message.setText("This is a test email.");

//             transport.sendMessage(message, message.getAllRecipients());
//             transport.close();
//             System.out.println("E-posta gönderildi!");

//         } catch (Exception e) {
//             e.printStackTrace();
//             System.out.println("Bağlantı hatası: " + e.getMessage());
//         }
//     }
// }
