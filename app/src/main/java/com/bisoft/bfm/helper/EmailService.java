package com.bisoft.bfm.helper;
import java.io.File; import java.util.ArrayList; import java.util.Objects;
import jakarta.mail.MessagingException; import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired; import org.springframework.beans.factory.annotation.Value; import org.springframework.core.io.FileSystemResource; import org.springframework.mail.SimpleMailMessage; import org.springframework.mail.javamail.JavaMailSender; import org.springframework.mail.javamail.MimeMessageHelper; import org.springframework.scheduling.annotation.Async; import org.springframework.stereotype.Service; import lombok.RequiredArgsConstructor;
@Service
public class EmailService {
    @Value("${bfm.notification-mail-receivers:redmine@bisoft.com.tr}")
    public String notification_mail_receivers;

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

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        if (notification_mail_receivers.contains(",")) {
            mailMessage.setTo(notification_mail_receivers.split(","));
        } else {
            mailMessage.setTo(notification_mail_receivers);
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

        helper.setFrom("info@bisoft.com.tr");
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
