package uk.gov.hmcts.reform.notifications.functional;


import org.springframework.http.HttpStatus;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.notifications.dtos.enums.NotificationType;
import uk.gov.hmcts.reform.notifications.dtos.request.Personalisation;
import uk.gov.hmcts.reform.notifications.dtos.request.RecipientPostalAddress;
import uk.gov.hmcts.reform.notifications.dtos.request.RefundNotificationEmailRequest;
import uk.gov.hmcts.reform.notifications.dtos.request.RefundNotificationLetterRequest;
import uk.gov.hmcts.reform.notifications.functional.config.IdamService;
import uk.gov.hmcts.reform.notifications.functional.config.NotificationsTestService;
import uk.gov.hmcts.reform.notifications.functional.config.S2sTokenService;
import uk.gov.hmcts.reform.notifications.functional.config.TestConfigProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.regex.Pattern;

@ActiveProfiles({"functional", "liberataMock"})
@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@Ignore("Ignoring the test cases for the purpose of Pre-implementation plan")
public class NotificationsServiceFunctionalTest {

    @Value("${notification.email-to-reply}")
    private String emailReplyToId;

    @Value("${notification.service-mailbox}")
    private String serviceMailBox;

    @Value("${notification.service-url}")
    private String serviceUrl;

    @Value("${notify.letter.template}")
    private String letterTemplateId;

    @Value("${notify.email.template}")
    private String emailTemplateId;

    @Autowired
    private TestConfigProperties testConfigProperties;

    @Autowired
    private IdamService idamService;

    @Autowired
    private S2sTokenService s2sTokenService;

    @Autowired
    private NotificationsTestService notificationsTestServicel;


    private static String SERVICE_TOKEN_REFUNDS_API;
    private static String USER_TOKEN_REFUNDS_API;
    private static boolean TOKENS_INITIALIZED;
    private static final Pattern
        REFUNDS_REGEX_PATTERN = Pattern.compile("^(RF)-([0-9]{4})-([0-9-]{4})-([0-9-]{4})-([0-9-]{4})$");

    @Before
    public void setUp() throws Exception {

        RestAssured.baseURI = testConfigProperties.baseTestUrl;
        if (!TOKENS_INITIALIZED) {

            USER_TOKEN_REFUNDS_API =
                idamService.createUserAuthToken("idam.user.ccpayrefundsapi@hmcts.net").getAuthorisationToken();

            SERVICE_TOKEN_REFUNDS_API =
                s2sTokenService.getS2sToken("refunds_api", testConfigProperties.s2sRefundsApi);

            TOKENS_INITIALIZED = true;

        }
    }

    @Test
    public void send_email_notification_request() {

        RefundNotificationEmailRequest refundNotificationEmailRequest = RefundNotificationEmailRequest.refundNotificationEmailRequestWith()
            .templateId(emailTemplateId)
            .recipientEmailAddress("test@test.com")
            .reference("Functional Test")
            .emailReplyToId(emailReplyToId)
            .notificationType(NotificationType.EMAIL)
            .personalisation(Personalisation.personalisationRequestWith()
                                 .ccdCaseNumber("1234567890123456")
                                 .refundReference("Functional Test")
                                 .serviceMailBox(serviceMailBox)
                                 .serviceUrl(serviceUrl)
                                 .build())
            .build();

        final Response responseNotificationEmail = notificationsTestServicel.postEmailNotification(
        USER_TOKEN_REFUNDS_API ,
        SERVICE_TOKEN_REFUNDS_API ,
        testConfigProperties.baseNotificationUrl,
        refundNotificationEmailRequest
        );
        assertThat(responseNotificationEmail.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
     }

    public void send_letter_notification_request() {

        RefundNotificationLetterRequest refundNotificationLetterRequest = RefundNotificationLetterRequest.refundNotificationLetterRequestWith()
            .templateId(letterTemplateId)
            .recipientPostalAddress(RecipientPostalAddress.recipientPostalAddressWith()
                                        .addressLine(" 1 Test Street")
                                        .city("London")
                                        .county("London")
                                        .country("England")
                                        .postalCode("XX1 YY1")
                                        .build())
            .reference("test reference")
            .notificationType(NotificationType.LETTER)
            .personalisation(Personalisation.personalisationRequestWith()
                                 .ccdCaseNumber("1234567890123456")
                                 .refundReference("test reference")
                                 .serviceMailBox(serviceMailBox)
                                 .serviceUrl(serviceUrl)
                                 .build())
            .build();

        final Response responseNotificationLetter = notificationsTestServicel.postLetterNotification(
            USER_TOKEN_REFUNDS_API ,
            SERVICE_TOKEN_REFUNDS_API ,
            testConfigProperties.baseNotificationUrl,
            refundNotificationLetterRequest
        );
        assertThat(responseNotificationLetter.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

}