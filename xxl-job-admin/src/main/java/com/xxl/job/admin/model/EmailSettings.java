package com.xxl.job.admin.model;

public class EmailSettings {

    private boolean enabled;
    private String host;
    private int port;
    private String username;
    private String from;
    private String password;
    private boolean passwordConfigured;
    private boolean smtpAuth;
    private boolean starttlsEnabled;
    private boolean starttlsRequired;
    private boolean sslEnabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isPasswordConfigured() {
        return passwordConfigured;
    }

    public void setPasswordConfigured(boolean passwordConfigured) {
        this.passwordConfigured = passwordConfigured;
    }

    public boolean isSmtpAuth() {
        return smtpAuth;
    }

    public void setSmtpAuth(boolean smtpAuth) {
        this.smtpAuth = smtpAuth;
    }

    public boolean isStarttlsEnabled() {
        return starttlsEnabled;
    }

    public void setStarttlsEnabled(boolean starttlsEnabled) {
        this.starttlsEnabled = starttlsEnabled;
    }

    public boolean isStarttlsRequired() {
        return starttlsRequired;
    }

    public void setStarttlsRequired(boolean starttlsRequired) {
        this.starttlsRequired = starttlsRequired;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }
}
