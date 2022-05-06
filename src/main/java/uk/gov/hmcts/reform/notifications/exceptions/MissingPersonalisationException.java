package uk.gov.hmcts.reform.notifications.exceptions;

public class MissingPersonalisationException extends GovNotifyException {

    public static final long serialVersionUID = 43287438;

    public MissingPersonalisationException (String message) {
        super(message);
    }
}