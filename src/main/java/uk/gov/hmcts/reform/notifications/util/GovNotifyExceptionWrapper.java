package uk.gov.hmcts.reform.notifications.util;

import uk.gov.hmcts.reform.notifications.exceptions.*;

public class GovNotifyExceptionWrapper {

    GovNotifyErrorMessage govNotifyErrorMessage = new GovNotifyErrorMessage();

    final static String INVALID_TEMPLATE_ID_RESPONSE = "template_id is not a valid UUID";
    final static String INVALID_POSTCODE_RESPONSE_ONE = "Must be a real UK postcode";
    final static String INVALID_POSTCODE_RESPONSE_TWO = "Last line of address must be a real UK postcode or another country";


    public GovNotifyException mapGovNotifyEmailException(int httpResult, String message){
        switch (httpResult) {
            case 400:
                String errorMessage = govNotifyErrorMessage.getErrorMessage(message);

                if (INVALID_TEMPLATE_ID_RESPONSE.equals(errorMessage)) {
                    return new InvalidTemplateId("Invalid Template ID");
                }else {
                    return new RestrictedApiKeyException("Internal Server Error, restricted API key");
                }
            case 403:
                return new InvalidApiKeyException("Internal Server Error, invalid API key");
            case 429:
                return new ExceededRequestLimitException("Internal Server Error, send limit exceeded");
            case 500:
                return new GovNotifyConnectionException("Service is not available, please try again");
            default:
                return new GovNotifyUnmappedException("Internal Server Error");
        }
    }

    public GovNotifyException mapGovNotifyLetterException(int httpResult, String message){
        switch (httpResult) {
            case 400:
                String errorMessage = govNotifyErrorMessage.getErrorMessage(message);

                if (INVALID_POSTCODE_RESPONSE_ONE.equals(errorMessage) ||
                    INVALID_POSTCODE_RESPONSE_TWO.equals(errorMessage)){
                    return new InvalidAddressException("Please enter a valid/real postcode");
                }else if (INVALID_TEMPLATE_ID_RESPONSE.equals(errorMessage)) {
                    return new InvalidTemplateId("Invalid Template ID");
                }else {
                    return new RestrictedApiKeyException("Internal Server Error, restricted API key");
                }
            case 403:
                return new InvalidApiKeyException("Internal Server Error, invalid API key");
            case 429:
                return new ExceededRequestLimitException("Internal Server Error, send limit exceeded");
            case 500:
                return new GovNotifyConnectionException("Service is not available, please try again");
            default:
                return new GovNotifyUnmappedException("Internal Server Error");
        }
    }
}
