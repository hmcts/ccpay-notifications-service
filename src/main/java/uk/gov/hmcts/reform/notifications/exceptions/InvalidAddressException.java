package uk.gov.hmcts.reform.notifications.exceptions;


public class InvalidAddressException extends GovNotifyException {

    private static final long serialVersionUID = 42348L;

    public InvalidAddressException(String message) {
        super(message);
    }

}


