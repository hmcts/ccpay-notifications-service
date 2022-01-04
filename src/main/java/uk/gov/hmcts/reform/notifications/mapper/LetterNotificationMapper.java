package uk.gov.hmcts.reform.notifications.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.notifications.dtos.request.RefundNotificationEmailRequest;
import uk.gov.hmcts.reform.notifications.dtos.request.RefundNotificationLetterRequest;
import uk.gov.hmcts.reform.notifications.model.ContactDetails;
import uk.gov.hmcts.reform.notifications.model.Notification;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendLetterResponse;

import java.util.ArrayList;
import java.util.List;

@Component
public class LetterNotificationMapper {

    public Notification letterResponseMapper(SendLetterResponse sendLetterResponse, RefundNotificationLetterRequest letterNotificationRequest) {
        List<ContactDetails> contactDetailsList = new ArrayList<>();
        contactDetailsList.add(ContactDetails.contactDetailsWith()
                                   .addressLine(letterNotificationRequest.getRecipientPostalAddress().getAddressLine())
                                   .postcode(letterNotificationRequest.getRecipientPostalAddress().getPostalCode())
                                   .county(letterNotificationRequest.getRecipientPostalAddress().getCounty())
                                   .city(letterNotificationRequest.getRecipientPostalAddress().getCity())
                                   .country(letterNotificationRequest.getRecipientPostalAddress().getCountry())
                                   .createdBy("System")
                                   .build());
        return Notification.builder()
            .notificationType(letterNotificationRequest.getNotificationType().toString())
            .reference(sendLetterResponse.getReference().get())
            .templateId(sendLetterResponse.getTemplateId().toString())
            .createdBy("System")
            .contactDetails(contactDetailsList)
            .build();
    }
}
