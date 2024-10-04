package com.chutneytesting.action.email;

import com.chutneytesting.action.spi.injectable.Target;
import javax.annotation.Nonnull;

public class EmailConfig {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String appPassword;
    private final boolean useTls;
    private final boolean useSsl;

    public EmailConfig(Target target) {
        this.host = target.property("smtp.host").orElse(target.host());
        this.port = target.numericProperty("smtp.port").map(Number::intValue).orElse(target.port());
        this.username = target.user().orElse("");
        this.password = target.userPassword().orElse("");
        this.appPassword = target.property("smtp.appPassword").orElse("");
        this.useTls = target.booleanProperty("smtp.tls").orElse(true);
        this.useSsl = target.booleanProperty("smtp.ssl").orElse(false);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAppPassword() {
        return appPassword;
    }

    public boolean isUseTls() {
        return useTls;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public boolean hasAppPassword() {
        return !appPassword.isEmpty();
    }

    public boolean hasUsernameAndPassword() {
        return !username.isEmpty() && !password.isEmpty();
    }
}
