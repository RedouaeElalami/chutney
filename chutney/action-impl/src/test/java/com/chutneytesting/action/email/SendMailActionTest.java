package com.chutneytesting.action.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.chutneytesting.action.spi.ActionExecutionResult;
import com.chutneytesting.action.spi.injectable.Logger;
import com.chutneytesting.action.spi.injectable.Target;
import jakarta.mail.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class SendMailActionTest {

    @Mock
    private Logger logger;

    @Mock
    private Target target;

    private SendMailAction sendMailAction;

    @BeforeEach
    void setUp() {
        when(target.host()).thenReturn("smtp.example.com");
        when(target.port()).thenReturn(587);
        when(target.property("smtp.host")).thenReturn(Optional.of("smtp.example.com"));
        when(target.numericProperty("smtp.port")).thenReturn(Optional.of(587));
        when(target.booleanProperty("smtp.tls")).thenReturn(Optional.of(true));
        when(target.booleanProperty("smtp.ssl")).thenReturn(Optional.of(false));
    }

    @Test
    void should_return_success_when_email_sent_successfully_with_username_and_password() throws Exception {
        setupUsernameAndPassword();
        testSuccessfulEmailSend();
    }

    @Test
    void should_return_success_when_email_sent_successfully_with_app_password() throws Exception {
        setupAppPassword();
        testSuccessfulEmailSend();
    }

    @Test
    void should_return_failure_when_email_sending_fails() throws Exception {
        setupUsernameAndPassword();
        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            mockedTransport.when(() -> Transport.send(any())).thenThrow(new jakarta.mail.MessagingException("SMTP server unavailable"));

            ActionExecutionResult result = sendMailAction.execute();

            assertThat(result.status).isEqualTo(ActionExecutionResult.Status.Failure);
            verify(logger).error(contains("Failed to send email"));
        }
    }

    @Test
    void should_return_failure_when_no_valid_authentication_method_provided() {
        when(target.user()).thenReturn(Optional.empty());
        when(target.userPassword()).thenReturn(Optional.empty());
        when(target.property("smtp.appPassword")).thenReturn(Optional.empty());

        sendMailAction = new SendMailAction(
            "recipient@example.com",
            "Test Subject",
            "Test Body",
            target,
            logger
        );

        ActionExecutionResult result = sendMailAction.execute();

        assertThat(result.status).isEqualTo(ActionExecutionResult.Status.Failure);
        verify(logger).error("Authentication configuration error: No valid authentication method provided");
    }

    private void setupUsernameAndPassword() {
        when(target.user()).thenReturn(Optional.of("sender@example.com"));
        when(target.userPassword()).thenReturn(Optional.of("password"));
        when(target.property("smtp.appPassword")).thenReturn(Optional.empty());

        sendMailAction = new SendMailAction(
            "recipient@example.com",
            "Test Subject",
            "Test Body",
            target,
            logger
        );
    }

    private void setupAppPassword() {
        when(target.user()).thenReturn(Optional.of("sender@example.com"));
        when(target.userPassword()).thenReturn(Optional.empty());
        when(target.property("smtp.appPassword")).thenReturn(Optional.of("1234567890123456"));

        sendMailAction = new SendMailAction(
            "recipient@example.com",
            "Test Subject",
            "Test Body",
            target,
            logger
        );
    }

    private void testSuccessfulEmailSend() throws Exception {
        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            mockedTransport.when(() -> Transport.send(any())).thenAnswer(invocation -> null);

            ActionExecutionResult result = sendMailAction.execute();

            assertThat(result.status).isEqualTo(ActionExecutionResult.Status.Success);
            verify(logger).info("Email sent successfully");
            mockedTransport.verify(() -> Transport.send(any()));
        }
    }
}
