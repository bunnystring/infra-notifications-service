package com.infragest.infra_notifications_service.util;

public abstract class MessageException {

    private MessageException() {}

    // Sengrid
    public static final String SENDER_EMAIL_NOT_CONFIGURED = "The sender email (fromEmail) is not configured.";
    public static final String SENDGRID_API_ERROR = "Failed to send email. Status code: %s, Response: %s";
    public static final String SENDGRID_CONNECTION_FAILURE = "Failed to connect to SendGrid. Details: %s";

}
