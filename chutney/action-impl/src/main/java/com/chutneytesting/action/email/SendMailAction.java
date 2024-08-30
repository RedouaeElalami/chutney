package com.chutneytesting.action.email;

import com.chutneytesting.action.spi.Action;
import com.chutneytesting.action.spi.ActionExecutionResult;
import com.chutneytesting.action.spi.injectable.Input;
import com.chutneytesting.action.spi.injectable.Logger;
import com.chutneytesting.action.spi.injectable.Target;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class SendMailAction implements Action {

    private final String to;
    private final String subject;
    private final String body;
    private final EmailConfig config;
    private final Logger logger;

    public SendMailAction(
        @Input("to") String to,
        @Input("subject") String subject,
        @Input("body") String body,
        Target target,
        Logger logger) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.config = new EmailConfig(target);
        this.logger = logger;
    }

    @Override
    public ActionExecutionResult execute() {
        if (!config.hasAppPassword() && !config.hasUsernameAndPassword()) {
            logger.error("Authentication configuration error: No valid authentication method provided");
            return ActionExecutionResult.ko();
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", config.getHost());
            props.put("mail.smtp.port", config.getPort());
            props.put("mail.smtp.starttls.enable", config.isUseTls());
            props.put("mail.smtp.ssl.enable", config.isUseSsl());

            Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
                protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                    if (config.hasAppPassword()) {
                        return new jakarta.mail.PasswordAuthentication(config.getUsername(), config.getAppPassword());
                    } else {
                        return new jakarta.mail.PasswordAuthentication(config.getUsername(), config.getPassword());
                    }
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getUsername()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);

            logger.info("Email sent successfully");
            return ActionExecutionResult.ok();
        } catch (MessagingException e) {
            logger.error("Failed to send email: " + e.getMessage());
            return ActionExecutionResult.ko();
        }
    }
}
