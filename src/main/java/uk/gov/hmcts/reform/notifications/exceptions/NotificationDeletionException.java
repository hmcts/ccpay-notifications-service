package uk.gov.hmcts.reform.notifications.exceptions;

public class NotificationDeletionException extends RuntimeException {

    public static final long serialVersionUID = 413287437L;

    public NotificationDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
