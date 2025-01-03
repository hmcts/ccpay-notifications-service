package uk.gov.hmcts.reform.notifications.mappers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.notifications.dtos.enums.NotificationType;
import uk.gov.hmcts.reform.notifications.dtos.request.Personalisation;
import uk.gov.hmcts.reform.notifications.dtos.request.RefundNotificationEmailRequest;
import uk.gov.hmcts.reform.notifications.dtos.response.IdamUserIdResponse;
import uk.gov.hmcts.reform.notifications.mapper.EmailNotificationMapper;
import uk.gov.hmcts.reform.notifications.model.Notification;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@ActiveProfiles({"local", "test"})
@SpringBootTest(webEnvironment = MOCK)
@SuppressWarnings("PMD")
class EmailNotificationMapperTest {

    @Autowired
    private EmailNotificationMapper emailNotificationMapper;

    private static final String EMAIL = "test@hmcts.net";

    @Test
    void testEmailotificationMapper() {

        Notification  notification = emailNotificationMapper.emailResponseMapper(getRefundNotificationEmailRequest(),IDAM_USER_ID_RESPONSE);
        assertEquals("EMAIL", notification.getNotificationType());
        assertEquals("RF-123", notification.getReference());
        assertEquals("8833960c-4ffa-42db-806c-451a68c56e98", notification.getTemplateId());
        assertEquals("e30ccf3a-8457-4e45-b251-74a346e7ec88", notification.getCreatedBy());
        assertEquals(EMAIL, notification.getContactDetails().getEmail());


    }

    private RefundNotificationEmailRequest getRefundNotificationEmailRequest() {

        return RefundNotificationEmailRequest.refundNotificationEmailRequestWith()
            .notificationType(NotificationType.EMAIL)
            .recipientEmailAddress(EMAIL)
            .emailReplyToId(EMAIL)
            .reference("RF-123")
            .personalisation(getPersonalisation())
            .templateId("8833960c-4ffa-42db-806c-451a68c56e98")
            .build();
    }


    private Personalisation getPersonalisation() {

        return Personalisation.personalisationRequestWith()
            .ccdCaseNumber("1111222233334444")
            .refundReference("REF-123")
            .build();
    }

    private static final IdamUserIdResponse IDAM_USER_ID_RESPONSE =
        IdamUserIdResponse.idamUserIdResponseWith().uid("e30ccf3a-8457-4e45-b251-74a346e7ec88").givenName("XX").familyName("YY")
            .name("XX YY")
            .roles(Arrays.asList("payments-refund-approver", "payments-refund")).sub("ZZ")
            .build();
}


