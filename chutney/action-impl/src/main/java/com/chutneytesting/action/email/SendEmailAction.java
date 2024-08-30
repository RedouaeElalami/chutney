package com.chutneytesting.action.email;

import com.chutneytesting.action.spi.Action;
import com.chutneytesting.action.spi.ActionExecutionResult;
import com.chutneytesting.action.spi.injectable.Input;
import com.chutneytesting.action.spi.injectable.Logger;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class SendEmailAction implements Action {

    private final String to;
    private final String from;
    private final String subject;
    private final String body;
    private final String smtpHost;
    private final Integer smtpPort;
    private final String username;
    private final String password;
    private final String appPassword;
    private final Boolean useTls;
    private final Boolean useSsl;
    private final String provider;
    private final Logger logger;

    public SendEmailAction(
        @Input("to") String to,
        @Input("from") String from,
        @Input("subject") String subject,
        @Input("body") String body,
        @Input("smtpHost") String smtpHost,
        @Input("smtpPort") Integer smtpPort,
        @Input("username") String username,
        @Input("password") String password,
        @Input("appPassword") String appPassword,
        @Input("useTls") Boolean useTls,
        @Input("useSsl") Boolean useSsl,
        @Input("provider") String provider,
        Logger logger) {
        this.to = to;
        this.from = from;
        this.subject = subject;
        this.body = body;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.username = username;
        this.password = password;
        this.appPassword = appPassword;
        this.useTls = useTls;
        this.useSsl = useSsl;
        this.provider = provider;
        this.logger = logger;
    }

    @Override
    public ActionExecutionResult execute() {
        try {
            Properties properties = new Properties();
            configureProperties(properties);

            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username,
                        "gmail".equalsIgnoreCase(provider) ? appPassword : password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
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

    private void configureProperties(Properties properties) {
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort);

        if (useSsl) {
            properties.put("mail.smtp.socketFactory.port", smtpPort);
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }

        if (useTls) {
            properties.put("mail.smtp.starttls.enable", "true");
        }

        switch (provider.toLowerCase()) {
            case "gmail":
                configureGmail(properties);
                break;
            case "outlook":
                configureOutlook(properties);
                break;
            default:
                // Default configuration, already set
        }
    }

    private void configureGmail(Properties properties) {
        properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");
    }

    private void configureOutlook(Properties properties) {
        properties.put("mail.smtp.ssl.trust", "smtp.office365.com");
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2");
    }
}
