package com.chutneytesting.action.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.chutneytesting.action.spi.ActionExecutionResult;
import com.chutneytesting.action.spi.injectable.Logger;
import jakarta.mail.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SendEmailActionTest {

    @Mock
    private Logger logger;

    private SendEmailAction gmailAction;
    private SendEmailAction outlookAction;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        gmailAction = new SendEmailAction(
            "to@example.com",
            "from@gmail.com",
            "Test Subject",
            "Test Body",
            "smtp.gmail.com",
            587,
            "username@gmail.com",
            "not-used",
            "abcdefghijklmnop",
            true,
            false,
            "gmail",
            logger
        );

        outlookAction = new SendEmailAction(
            "to@example.com",
            "from@outlook.com",
            "Test Subject",
            "Test Body",
            "smtp.office365.com",
            587,
            "username@outlook.com",
            "password",
            null,
            true,
            false,
            "outlook",
            logger
        );
    }

    @Test
    void should_return_success_when_gmail_email_sent_successfully() throws Exception {
        testSuccessfulEmailSend(gmailAction);
    }

    @Test
    void should_return_success_when_outlook_email_sent_successfully() throws Exception {
        testSuccessfulEmailSend(outlookAction);
    }

    @Test
    void should_return_failure_when_gmail_email_sending_fails() throws Exception {
        testFailedEmailSend(gmailAction);
    }

    @Test
    void should_return_failure_when_outlook_email_sending_fails() throws Exception {
        testFailedEmailSend(outlookAction);
    }

    private void testSuccessfulEmailSend(SendEmailAction action) throws Exception {
        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            mockedTransport.when(() -> Transport.send(any())).thenAnswer(invocation -> null);

            ActionExecutionResult result = action.execute();

            assertThat(result.status).isEqualTo(ActionExecutionResult.Status.Success);
            verify(logger).info("Email sent successfully");
            mockedTransport.verify(() -> Transport.send(any()));
        }
    }

    private void testFailedEmailSend(SendEmailAction action) throws Exception {
        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            mockedTransport.when(() -> Transport.send(any())).thenThrow(new jakarta.mail.MessagingException("SMTP server unavailable"));

            ActionExecutionResult result = action.execute();

            assertThat(result.status).isEqualTo(ActionExecutionResult.Status.Failure);
            verify(logger).error(contains("Failed to send email"));
        }
    }
}
