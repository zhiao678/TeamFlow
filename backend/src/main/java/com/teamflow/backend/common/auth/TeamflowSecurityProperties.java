package com.teamflow.backend.common.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "teamflow.security")
public class TeamflowSecurityProperties {

    private String jwtSecret;
    private long tokenExpirationHours = 24;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getTokenExpirationHours() {
        return tokenExpirationHours;
    }

    public void setTokenExpirationHours(long tokenExpirationHours) {
        this.tokenExpirationHours = tokenExpirationHours;
    }
}
