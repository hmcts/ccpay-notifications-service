package uk.gov.hmcts.reform.notifications.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.reform.notifications.config.PostcodeLookupConfiguration;
import uk.gov.hmcts.reform.notifications.config.security.idam.IdamServiceImpl;
import uk.gov.hmcts.reform.notifications.dtos.enums.NotificationType;
import uk.gov.hmcts.reform.notifications.dtos.request.DocPreviewRequest;
import uk.gov.hmcts.reform.notifications.dtos.request.Personalisation;
import uk.gov.hmcts.reform.notifications.dtos.request.RecipientPostalAddress;
import uk.gov.hmcts.reform.notifications.dtos.request.RefundNotificationEmailRequest;
import uk.gov.hmcts.reform.notifications.dtos.request.RefundNotificationLetterRequest;
import uk.gov.hmcts.reform.notifications.dtos.response.AddressDetails;
import uk.gov.hmcts.reform.notifications.dtos.response.FromTemplateContact;
import uk.gov.hmcts.reform.notifications.dtos.response.IdamUserIdResponse;
import uk.gov.hmcts.reform.notifications.dtos.response.MailAddress;
import uk.gov.hmcts.reform.notifications.dtos.response.NotificationResponseDto;
import uk.gov.hmcts.reform.notifications.dtos.response.PostCodeResponse;
import uk.gov.hmcts.reform.notifications.dtos.response.PostCodeResult;
import uk.gov.hmcts.reform.notifications.exceptions.InvalidAddressException;
import uk.gov.hmcts.reform.notifications.exceptions.InvalidApiKeyException;
import uk.gov.hmcts.reform.notifications.exceptions.InvalidTemplateId;
import uk.gov.hmcts.reform.notifications.exceptions.NotificationListEmptyException;
import uk.gov.hmcts.reform.notifications.exceptions.NotificationNotFoundException;
import uk.gov.hmcts.reform.notifications.exceptions.PostCodeLookUpException;
import uk.gov.hmcts.reform.notifications.exceptions.RefundReasonNotFoundException;
import uk.gov.hmcts.reform.notifications.mapper.EmailNotificationMapper;
import uk.gov.hmcts.reform.notifications.mapper.LetterNotificationMapper;
import uk.gov.hmcts.reform.notifications.mapper.NotificationResponseMapper;
import uk.gov.hmcts.reform.notifications.mapper.NotificationTemplateResponseMapper;
import uk.gov.hmcts.reform.notifications.model.ContactDetails;
import uk.gov.hmcts.reform.notifications.model.Notification;
import uk.gov.hmcts.reform.notifications.model.NotificationRefundReasons;
import uk.gov.hmcts.reform.notifications.model.ServiceContact;
import uk.gov.hmcts.reform.notifications.model.TemplatePreviewDto;
import uk.gov.hmcts.reform.notifications.repository.NotificationRefundReasonRepository;
import uk.gov.hmcts.reform.notifications.repository.NotificationRepository;
import uk.gov.hmcts.reform.notifications.repository.ServiceContactRepository;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendLetterResponse;
import uk.gov.service.notify.TemplatePreview;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@ActiveProfiles({"local", "test"})
@SpringBootTest(webEnvironment = MOCK)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings("PMD")
public class NotificationServiceImplTest {

    public static final Supplier<Notification> letterNotificationListSupplierBasedOnRefundRef = () -> Notification.builder()
        .id(1)
        .notificationType("LETTER")
        .reference("RF-123")
        .templateId("8833960c-4ffa-42db-806c-451a68c56e98")
        .createdBy("System")
        .dateUpdated(new Date())
        .contactDetails(getContactLetter())
        .templatePreview(TemplatePreviewDto.templatePreviewDtoWith()
                              .id(UUID.randomUUID())
                              .html("test")
                              .body("test")
                              .subject("testSubject")
                              .templateType("letter")
                             .from(FromTemplateContact
                                       .buildFromTemplateContactWith()
                                       .fromMailAddress(
                                           MailAddress
                                               .buildRecipientMailAddressWith()
                                               .addressLine("6 Test")
                                               .city("city")
                                               .country("country")
                                               .county("county")
                                               .postalCode("HA3 5TT")
                                               .build())
                                       .build())
                             .build())
        .build();
    @InjectMocks
    private NotificationServiceImpl notificationServiceImpl;
    @Mock
    private NotificationRepository notificationRepository;
    private ContactDetails contactDetails;
    @Spy
    private NotificationResponseMapper notificationResponseMapper;

    @Mock
    private IdamServiceImpl idamService;

    @MockBean
    @Qualifier("restTemplateIdam")
    private RestTemplate restTemplateIdam;
    @Value("${idam.api.url}")
    private String idamBaseUrl;

    @Mock
    @Qualifier("Email")
    private NotificationClientApi notificationEmailClient;

    @Mock
    @Qualifier("Letter")
    private NotificationClientApi notificationLetterClient;

    @Mock
    private EmailNotificationMapper emailNotificationMapper;

    @Mock
    private LetterNotificationMapper letterNotificationMapper;

    @Mock
    private MultiValueMap<String, String> map;

    @Mock
    private ServiceContactRepository serviceContactRepository;

    @Mock
    private NotificationRefundReasonRepository notificationRefundReasonRepository;

    @Mock
    private NotificationTemplateResponseMapper notificationTemplateResponseMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    @Qualifier("restTemplatePostCodeLookUp")
    private RestTemplate restTemplatePostCodeLookUp;

    @Mock
    private PostcodeLookupConfiguration postcodeLookupConfiguration;

    public static final String GET_REFUND_LIST_CCD_CASE_USER_ID1 = "1f2b7025-0f91-4737-92c6-b7a9baef14c6";

    private static ContactDetails getContactLetter() {

        return ContactDetails.contactDetailsWith()
            .id(1)
            .addressLine("11 King street")
            .city("london")
            .country("UK")
            .email(null)
            .dateCreated(new Date())
            .postcode("e146kk")
            .county("london")
            .dateUpdated(new Date())
            .createdBy("e30ccf3a-8457-4e45-b251-74a346e7ec88")
            .build();
    }

    public static final Supplier<Notification> emailNotificationListSupplierBasedOnRefundRef = () -> Notification.builder()
        .id(1)
        .notificationType("EMAIL")
        .reference("RF-124")
        .templateId("8833960c-4ffa-42db-806c-451a68c56e98")
        .createdBy("e30ccf3a-8457-4e45-b251-74a346e7ec88")
        .dateUpdated(new Date())
        .contactDetails(getContactEmail())
        .templatePreview(TemplatePreviewDto.templatePreviewDtoWith()
                             .id(UUID.randomUUID())
                             .html("test")
                             .body("test")
                             .subject("testSubject")
                             .templateType("email")
                             .from(FromTemplateContact.buildFromTemplateContactWith().fromEmailAddress("test@hmcts.net").build())
        .build())
        .build();

    private static ContactDetails getContactEmail() {

        return ContactDetails.contactDetailsWith()
            .id(1)
            .addressLine(null)
            .city(null)
            .country(null)
            .email("test@hmcts.net")
            .dateCreated(new Date())
            .postcode(null)
            .county(null)
            .dateUpdated(new Date())
            .createdBy("e30ccf3a-8457-4e45-b251-74a346e7ec88")
            .build();
    }

    public void mockUserinfoCall(IdamUserIdResponse idamUserIdResponse) {
        UriComponentsBuilder builderForUserInfo = UriComponentsBuilder.fromUriString(
            idamBaseUrl + IdamServiceImpl.USERID_ENDPOINT);
        ResponseEntity<IdamUserIdResponse> responseEntity = new ResponseEntity<>(idamUserIdResponse, HttpStatus.OK);
        when(restTemplateIdam.exchange(
            eq(builderForUserInfo.toUriString()),
            any(HttpMethod.class),
            any(HttpEntity.class),
            eq(IdamUserIdResponse.class)
        )).thenReturn(responseEntity);
    }

    public static final Supplier<IdamUserIdResponse> idamUserIDResponseSupplier = () -> IdamUserIdResponse.idamUserIdResponseWith()
        .familyName("mock-Surname")
        .givenName("mock-ForeName")
        .name("mock-ForeName mock-Surname")
        .sub("mockfullname@gmail.com")
        .roles(List.of("payments-refund", "payments-refund-approver", "refund-admin"))
        .uid(GET_REFUND_LIST_CCD_CASE_USER_ID1)
        .build();

    @BeforeEach
    void setup() {
        when(postcodeLookupConfiguration.getUrl()).thenReturn("https://api.os.uk/search/places/v1");
        when(postcodeLookupConfiguration.getAccessKey()).thenReturn("dummy");
    }

    @Test
  public void throwNotificationListEmptyExceptionWhenNotificationListIsEmpty() {
        when(notificationRepository.findByReferenceOrderByDateUpdatedDesc(anyString())).thenReturn(Optional.empty());

        assertThrows(NotificationListEmptyException.class, () -> notificationServiceImpl
            .getNotification(null));
    }

    @Test
    public void testNotificationListForLetterNotificationForGivenRefundReference() {

        when(notificationRepository.findByReferenceOrderByDateUpdatedDesc(anyString())).thenReturn(Optional.ofNullable(List.of(
            letterNotificationListSupplierBasedOnRefundRef.get())));

        NotificationResponseDto notificationListDtoResponse = notificationServiceImpl.getNotification("Notify-123");

        assertNotNull(notificationListDtoResponse);
        assertEquals(1, notificationListDtoResponse.getNotifications().size());
        assertEquals("LETTER", notificationListDtoResponse.getNotifications().get(0).getNotificationType());
        assertEquals("RF-123", notificationListDtoResponse.getNotifications().get(0).getReference());

    }

    @Test
   public  void givenEmptyNotificationList_whenGetNotification_thenNotificationListEmptyExceptionIsReceived() {

        when(notificationRepository.findByReferenceOrderByDateUpdatedDesc(anyString())).thenReturn(Optional.empty());

        Exception exception = assertThrows(
            NotificationListEmptyException.class,
            () -> notificationServiceImpl.getNotification(
                null
            )
        );
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Notification has not been sent for this refund"));
    }

    @Test
    void testNotificationListForEmailNotificationForGivenRefundReference() {

        when(notificationRepository.findByReferenceOrderByDateUpdatedDesc(anyString())).thenReturn(Optional.ofNullable(List.of(
            emailNotificationListSupplierBasedOnRefundRef.get())));

        NotificationResponseDto notificationListDtoResponse = notificationServiceImpl.getNotification("Notify-124");

        assertNotNull(notificationListDtoResponse);
        assertEquals(1, notificationListDtoResponse.getNotifications().size());
        assertEquals("EMAIL", notificationListDtoResponse.getNotifications().get(0).getNotificationType());
        assertEquals("RF-124", notificationListDtoResponse.getNotifications().get(0).getReference());

    }

    @Test
    void testsendEmailNotificationForRefundNotificationEmailRequest() throws NotificationClientException {
        mockUserinfoCall(idamUserIDResponseSupplier.get());
        final RefundNotificationEmailRequest request = RefundNotificationEmailRequest.refundNotificationEmailRequestWith()
            .notificationType(NotificationType.EMAIL)
            .templateId("test")
            .reference("REF-123")
            .recipientEmailAddress("test@test.com")
            .personalisation(
                Personalisation.personalisationRequestWith().ccdCaseNumber("1600162727220633").refundReference("RF-1234-1234-1234-1234").refundAmount(
                    BigDecimal.valueOf(10)).refundReason("test").build())
            .build();
        when(serviceContactRepository.findByServiceName(any()))
            .thenReturn(Optional.of(ServiceContact.serviceContactWith().id(1).serviceName("Probate").serviceMailbox("probate@gov.uk").build()));
        when(notificationRefundReasonRepository.findByRefundReasonCode(any()))
            .thenReturn(Optional.of(
                NotificationRefundReasons
                    .notificationRefundReasonWith()
                    .refundReasonNotification("There has been an amendment to your claim")
                    .build()
        ));
        SendEmailResponse response = new SendEmailResponse(
            "{\"content\":{\"body\":\"Hello Unknown, your reference is string\\r\\n\\r\\nRefund Approved\\"
                + "r\\n\\r\\nThanks\",\"from_email\":\"test@gov.uk\",\"subject\":"
                + "\"Refund Notification Approval\"},\"id\":\"10f101e0-6ab8-4a83-8ebd-124d648dd282\","
                + "\"reference\":\"string\",\"scheduled_for\":null,\"template\":"
                + "{\"id\":\"10f101e0-6ab8-4a83-8ebd-124d648dd282\",\"uri\":"
                + "\"https://api.notifications.service.gov.uk/services\""
                + ",\"version\":1},\"uri\":\"https://api.notifications.service.gov.uk\"}\n");
        Notification notification = Notification.builder().build();

        TemplatePreview templatePreview = new TemplatePreview(
            "{                                                             "
                + "\"id\": \"1222960c-4ffa-42db-806c-451a68c56e09\","
                + "\"type\": \"email\","
                + "\"version\": 11,"
                + "\"body\": \"Dear Sir/Madam\","
                + "\"subject\": \"HMCTS refund request approved\","
                + "\"html\": \"Dear Sir/Madam\","
                + "}");
        when(notificationEmailClient.generateTemplatePreview(any(), anyMap())).thenReturn(templatePreview);
        when(notificationTemplateResponseMapper.toFromMapper(any(), any())).thenReturn(FromTemplateContact
                                                                                    .buildFromTemplateContactWith()
                                                                                    .fromEmailAddress("test@test.com")
                                                                                    .build());
        when(notificationEmailClient.sendEmail(any(), any(), any(), any())).thenReturn(response);
        when(emailNotificationMapper.emailResponseMapper(any(),any())).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(notification);


        response = notificationServiceImpl.sendEmailNotification(request,any());

        assertEquals("Refund Notification Approval", response.getSubject());
        assertEquals("test@gov.uk", response.getFromEmail().get());

    }

    @Test
    void testsendLetterNotificationForRefundNotificationLetterRequest() throws NotificationClientException {
        mockUserinfoCall(idamUserIDResponseSupplier.get());
        final RefundNotificationLetterRequest request = RefundNotificationLetterRequest.refundNotificationLetterRequestWith()
            .notificationType(NotificationType.LETTER)
            .templateId("test")
            .reference("REF-123")
            .recipientPostalAddress(RecipientPostalAddress.recipientPostalAddressWith()
                                        .addressLine("test")
                                        .city("test")
                                        .county("test")
                                        .country("test")
                                        .postalCode("TE ST1")
                                        .build())
            .personalisation(
                Personalisation.personalisationRequestWith().ccdCaseNumber("123").refundReference("RF-1234-1234-1234-1234").refundAmount(
                    BigDecimal.valueOf(10)).refundReason("test").build())
            .build();
        when(serviceContactRepository.findByServiceName(any()))
            .thenReturn(Optional.of(ServiceContact.serviceContactWith().id(1).serviceName("Probate").serviceMailbox("probate@gov.uk").build()));
        when(notificationRefundReasonRepository.findByRefundReasonCode(any()))
            .thenReturn(Optional.of(
                NotificationRefundReasons
                    .notificationRefundReasonWith()
                    .refundReasonNotification("There has been an amendment to your claim")
                    .build()
        ));
        SendLetterResponse response = new SendLetterResponse(
            "{\"content\":{\"body\":\"Hello Unknown\\r\\n\\r\\nRefund Approved on 2022-01-01\","
                + "\"subject\":\"Refund Notification\"},\"id\":\"0f101e0-6ab8-4a83-8ebd-124d648dd282\","
                + "\"reference\":\"string\",\"scheduled_for\":null,\"template\":{\"id\":"
                + "\"0f101e0-6ab8-4a83-8ebd-124d648dd282\",\"uri\":"
                + "\"https://api.notifications.service\","
                + "\"version\":1},\"uri\":\"https://api.notifications.service\"}\n");
        Notification notification = Notification.builder().build();

        TemplatePreview templatePreview = new TemplatePreview(
            "{                                                             "
                + "\"id\": \"1222960c-4ffa-42db-806c-451a68c56e09\","
                + "\"type\": \"email\","
                + "\"version\": 11,"
                + "\"body\": \"Dear Sir/Madam\","
                + "\"subject\": \"HMCTS refund request approved\","
                + "\"html\": \"Dear Sir/Madam\","
                + "}");
        when(notificationLetterClient.generateTemplatePreview(any(), anyMap())).thenReturn(templatePreview);
        when(notificationTemplateResponseMapper.toFromMapper(any(), any())).thenReturn(FromTemplateContact
                                                                                    .buildFromTemplateContactWith()
                                                                                    .fromMailAddress(
                                                                                        MailAddress
                                                                                            .buildRecipientMailAddressWith()
                                                                                            .addressLine("6 Test")
                                                                                            .city("city")
                                                                                            .country("country")
                                                                                            .county("county")
                                                                                            .postalCode("HA3 5TT")
                                                                                            .build())
                                                                                    .build());
        when(notificationLetterClient.sendLetter(any(), any(), any())).thenReturn(response);
        when(letterNotificationMapper.letterResponseMapper(any(),any(),any())).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(notification);


        response = notificationServiceImpl.sendLetterNotification(request,any());
        assertEquals("Refund Notification", response.getSubject());


    }


    @Test
    void testsendEmailNotificationForRefundNotificationEmailRequestWithTemplatePreview() throws NotificationClientException {
        mockUserinfoCall(idamUserIDResponseSupplier.get());
        final RefundNotificationEmailRequest request = RefundNotificationEmailRequest.refundNotificationEmailRequestWith()
            .notificationType(NotificationType.EMAIL)
            .templateId("test")
            .reference("REF-123")
            .recipientEmailAddress("test@test.com")
            .personalisation(
                Personalisation.personalisationRequestWith().ccdCaseNumber("1600162727220633").refundReference("RF-1234-1234-1234-1234").refundAmount(
                    BigDecimal.valueOf(10)).refundReason("test").build())
            .templatePreview(TemplatePreviewDto.templatePreviewDtoWith().id(UUID.randomUUID())
                                 .templateType("email")
                                 .version(11)
                                 .body("Dear Sir Madam")
                                 .subject("HMCTS refund request approved")
                                 .html("Dear Sir Madam")
                                 .from(FromTemplateContact
                                           .buildFromTemplateContactWith()
                                           .fromEmailAddress("test@test.com")
                                           .build())
                                 .build())
            .build();

        when(serviceContactRepository.findByServiceName(any()))
            .thenReturn(Optional.of(ServiceContact.serviceContactWith().id(1).serviceName("Probate").serviceMailbox("probate@gov.uk").build()));
        when(notificationRefundReasonRepository.findByRefundReasonCode(any()))
            .thenReturn(Optional.of(
                NotificationRefundReasons
                    .notificationRefundReasonWith()
                    .refundReasonNotification("There has been an amendment to your claim")
                    .build()
        ));
        SendEmailResponse response = new SendEmailResponse(
            "{\"content\":{\"body\":\"Hello Unknown, your reference is string\\r\\n\\r\\nRefund Approved\\"
                + "r\\n\\r\\nThanks\",\"from_email\":\"test@gov.uk\",\"subject\":"
                + "\"Refund Notification Approval\"},\"id\":\"10f101e0-6ab8-4a83-8ebd-124d648dd282\","
                + "\"reference\":\"string\",\"scheduled_for\":null,\"template\":"
                + "{\"id\":\"10f101e0-6ab8-4a83-8ebd-124d648dd282\",\"uri\":"
                + "\"https://api.notifications.service.gov.uk/services\""
                + ",\"version\":1},\"uri\":\"https://api.notifications.service.gov.uk\"}\n");
        Notification notification = Notification.builder().build();

        when(notificationEmailClient.sendEmail(any(), any(), any(), any())).thenReturn(response);
        when(emailNotificationMapper.emailResponseMapper(any(),any())).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(notification);


        response = notificationServiceImpl.sendEmailNotification(request,any());

        assertEquals("Refund Notification Approval", response.getSubject());
        assertEquals("test@gov.uk", response.getFromEmail().get());

    }

    @Test
    void testsendLetterNotificationForRefundNotificationLetterRequestWithTemplatePreview() throws NotificationClientException {
        mockUserinfoCall(idamUserIDResponseSupplier.get());
        final RefundNotificationLetterRequest request = RefundNotificationLetterRequest.refundNotificationLetterRequestWith()
            .notificationType(NotificationType.LETTER)
            .templateId("test")
            .reference("REF-123")
            .recipientPostalAddress(RecipientPostalAddress.recipientPostalAddressWith()
                                        .addressLine("test")
                                        .city("test")
                                        .county("test")
                                        .country("test")
                                        .postalCode("TE ST1")
                                        .build())
            .personalisation(
                Personalisation.personalisationRequestWith().ccdCaseNumber("123").refundReference("RF-1234-1234-1234-1234").refundAmount(
                    BigDecimal.valueOf(10)).refundReason("test").build())
            .templatePreview(TemplatePreviewDto.templatePreviewDtoWith().id(UUID.randomUUID())
                                 .templateType("email")
                                 .version(11)
                                 .body("Dear Sir Madam")
                                 .subject("HMCTS refund request approved")
                                 .html("Dear Sir Madam")
                                 .from(FromTemplateContact
                                           .buildFromTemplateContactWith()
                                           .fromMailAddress(
                                               MailAddress
                                                   .buildRecipientMailAddressWith()
                                                   .addressLine("6 Test")
                                                   .city("city")
                                                   .country("country")
                                                   .county("county")
                                                   .postalCode("HA3 5TT")
                                                   .build())
                                            .build())
                                 .build())
            .build();
        when(serviceContactRepository.findByServiceName(any()))
            .thenReturn(Optional.of(ServiceContact.serviceContactWith().id(1).serviceName("Probate").serviceMailbox("probate@gov.uk").build()));

        SendLetterResponse response = new SendLetterResponse(
            "{\"content\":{\"body\":\"Hello Unknown\\r\\n\\r\\nRefund Approved on 2022-01-01\","
                + "\"subject\":\"Refund Notification\"},\"id\":\"0f101e0-6ab8-4a83-8ebd-124d648dd282\","
                + "\"reference\":\"string\",\"scheduled_for\":null,\"template\":{\"id\":"
                + "\"0f101e0-6ab8-4a83-8ebd-124d648dd282\",\"uri\":"
                + "\"https://api.notifications.service\","
                + "\"version\":1},\"uri\":\"https://api.notifications.service\"}\n");
        Notification notification = Notification.builder().build();

        when(notificationTemplateResponseMapper.toFromMapper(any(), any())).thenReturn(FromTemplateContact
                                                                                    .buildFromTemplateContactWith()
                                                                                    .fromEmailAddress("test@test.com")
                                                                                    .build());
        when(notificationLetterClient.sendLetter(any(), any(), any())).thenReturn(response);
        when(letterNotificationMapper.letterResponseMapper(any(),any(),any())).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationRefundReasonRepository.findByRefundReasonCode(any()))
            .thenReturn(Optional.of(
                NotificationRefundReasons
                    .notificationRefundReasonWith()
                    .refundReasonNotification("There has been an amendment to your claim")
                    .build()
        ));

        response = notificationServiceImpl.sendLetterNotification(request,any());
        assertEquals("Refund Notification", response.getSubject());


    }

    @Test
    public void throwsInvalidAddressExceptionWhenInvalidPostCodeProvidedInRequest() throws Exception {
        mockUserinfoCall(idamUserIDResponseSupplier.get());
        String errorMessage = "Status code: 400 {\"errors\":[{\"error\":\"BadRequestError\","
            + "\"message\":\"Must be a real UK postcode\"}],\"status_code\":400}\n";

        final RefundNotificationLetterRequest request = RefundNotificationLetterRequest.refundNotificationLetterRequestWith()
            .notificationType(NotificationType.LETTER)
            .templateId("test")
            .reference("REF-123")
            .recipientPostalAddress(RecipientPostalAddress.recipientPostalAddressWith()
                                        .addressLine("test")
                                        .city("test")
                                        .county("test")
                                        .country("test")
                                        .postalCode("TE ST1")
                                        .build())
            .personalisation(
                Personalisation.personalisationRequestWith().ccdCaseNumber("1600162727220633").refundReference("RF-1234-1234-1234-1234").refundAmount(
                    BigDecimal.valueOf(10)).refundReason("test").build())
            .build();
        when(serviceContactRepository.findByServiceName(any()))
            .thenReturn(Optional.of(ServiceContact.serviceContactWith().id(1).serviceName("Probate").serviceMailbox("probate@gov.uk").build()));
        when(notificationRefundReasonRepository.findByRefundReasonCode(any()))
            .thenReturn(Optional.of(
                NotificationRefundReasons
                    .notificationRefundReasonWith()
                    .refundReasonNotification("There has been an amendment to your claim")
                    .build()
        ));
        TemplatePreview templatePreview = new TemplatePreview(
            "{                                                             "
                + "\"id\": \"1222960c-4ffa-42db-806c-451a68c56e09\","
                + "\"type\": \"email\","
                + "\"version\": 11,"
                + "\"body\": \"Dear Sir/Madam\","
                + "\"subject\": \"HMCTS refund request approved\","
                + "\"html\": \"Dear Sir/Madam\","
                + "}");
        when(notificationLetterClient.generateTemplatePreview(any(), anyMap())).thenReturn(templatePreview);
        when(notificationTemplateResponseMapper.toFromMapper(any(), any())).thenReturn(FromTemplateContact
                                                                                    .buildFromTemplateContactWith()
                                                                                    .fromMailAddress(
                                                                                        MailAddress
                                                                                            .buildRecipientMailAddressWith()
                                                                                            .addressLine("6 Test")
                                                                                            .city("city")
                                                                                            .country("country")
                                                                                            .county("county")
                                                                                            .postalCode("HA3 5TT")
                                                                                            .build())
                                                                                    .build());
        when(notificationLetterClient.sendLetter(any(), any(), any())).thenThrow(new NotificationClientException(errorMessage));
        assertThrows(InvalidAddressException.class, () -> notificationServiceImpl.sendLetterNotification(request, any()
        ));
    }

    @Test
    public void throwsInvalidTemplateIdExcetionWhenInvalidTemplateIdProvidedInRequest() throws Exception {

        mockUserinfoCall(idamUserIDResponseSupplier.get());
        String errorMessage = "Status code: 400 {\"errors\":[{\"error\":\"BadRequestError\","
            + "\"message\":\"template_id is not a valid UUID\"}],\"status_code\":400}\n";
        final RefundNotificationLetterRequest request = RefundNotificationLetterRequest.refundNotificationLetterRequestWith()
            .notificationType(NotificationType.LETTER)
            .templateId("test")
            .reference("REF-123")
            .recipientPostalAddress(RecipientPostalAddress.recipientPostalAddressWith()
                                        .addressLine("test")
                                        .city("test")
                                        .county("test")
                                        .country("test")
                                        .postalCode("TE ST1")
                                        .build())
            .personalisation(
                Personalisation.personalisationRequestWith().ccdCaseNumber("1600162727220633").refundReference("RF-1234-1234-1234-1234").refundAmount(
                    BigDecimal.valueOf(10)).refundReason("test").build())
            .build();
        when(serviceContactRepository.findByServiceName(any()))
            .thenReturn(Optional.of(
                ServiceContact
                    .serviceContactWith()
                    .id(1)
                    .serviceName("Probate")
                    .serviceMailbox("probate@gov.uk")
                    .build()));
        when(notificationRefundReasonRepository.findByRefundReasonCode(any()))
            .thenReturn(Optional.of(
                NotificationRefundReasons
                    .notificationRefundReasonWith()
                    .refundReasonNotification("There has been an amendment to your claim")
                    .build()
        ));
        TemplatePreview templatePreview = new TemplatePreview(
            "{                                                             "
                + "\"id\": \"1222960c-4ffa-42db-806c-451a68c56e09\","
                + "\"type\": \"email\","
                + "\"version\": 11,"
                + "\"body\": \"Dear Sir/Madam\","
                + "\"subject\": \"HMCTS refund request approved\","
                + "\"html\": \"Dear Sir/Madam\","
                + "}");
        when(notificationLetterClient.generateTemplatePreview(any(), anyMap())).thenReturn(templatePreview);

        when(notificationLetterClient.sendLetter(any(), any(), any())).thenThrow(new NotificationClientException(errorMessage));
        assertThrows(InvalidTemplateId.class, () -> notificationServiceImpl.sendLetterNotification(request, any()
        ));
    }

    @Test
    public void throwsInvalidApiKeyExceptionnWhenInvalidApiKeyInRequest() throws Exception {

        mockUserinfoCall(idamUserIDResponseSupplier.get());
        String errorMessage = "Status code: 403 {\"errors\":[{\"error\":\"BadRequestError\","
            + "\"message\":\"Invalid api key\"}],\"status_code\":400}\n";
        final RefundNotificationEmailRequest request = RefundNotificationEmailRequest.refundNotificationEmailRequestWith()
            .notificationType(NotificationType.EMAIL)
            .templateId("test")
            .reference("REF-123")
            .recipientEmailAddress("test@test.com")
            .personalisation(
                Personalisation
                    .personalisationRequestWith()
                    .ccdCaseNumber("1600162727220633")
                    .refundReference("RF-1234-1234-1234-1234")
                    .refundAmount(BigDecimal.valueOf(10))
                    .refundReason("test").build())
            .build();
        when(serviceContactRepository.findByServiceName(any()))
            .thenReturn(Optional.of(ServiceContact.serviceContactWith().id(1).serviceName("Probate").serviceMailbox("probate@gov.uk").build()));
        when(notificationRefundReasonRepository.findByRefundReasonCode(any())).thenReturn(Optional.of(
            NotificationRefundReasons.notificationRefundReasonWith().refundReasonNotification("There has been an amendment to your claim").build()
        ));
        TemplatePreview templatePreview = new TemplatePreview(
            "{                                                             "
                + "\"id\": \"1222960c-4ffa-42db-806c-451a68c56e09\","
                + "\"type\": \"email\","
                + "\"version\": 11,"
                + "\"body\": \"Dear Sir/Madam\","
                + "\"subject\": \"HMCTS refund request approved\","
                + "\"html\": \"Dear Sir/Madam\","
                + "}");
        when(notificationEmailClient.generateTemplatePreview(any(), anyMap())).thenReturn(templatePreview);

        when(notificationEmailClient.sendEmail(any(), any(),any(),any())).thenThrow(new NotificationClientException(errorMessage));
        assertThrows(InvalidApiKeyException.class, () -> notificationServiceImpl.sendEmailNotification(request, any()
        ));
    }

    @Test
    void shouldReturnAddressWhenGivenPostCodeIsValid() throws JsonProcessingException {

        PostCodeResult result =
            PostCodeResult.builder()
                .dpa(
                    AddressDetails.builder()
                        .address("Flat 100 ABC Regent Road")
                        .buildingNumber("FLAT 100")
                        .countryCode("UK")
                        .build())
                .build();
        ObjectMapper mapper = new ObjectMapper();

        PostCodeResponse res = PostCodeResponse.builder().results(Arrays.asList(result)).build();
        String resultJson = mapper.writeValueAsString(res);
        ResponseEntity<String> responseEntity = ResponseEntity.ok(resultJson);

        when(objectMapper.readValue(anyString(), eq(PostCodeResponse.class))).thenReturn(res);
        when(restTemplatePostCodeLookUp.exchange(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any()))
            .thenReturn(responseEntity);

        PostCodeResponse postCodeResponse = notificationServiceImpl.getAddress("TW5 1BC");
        assertEquals("FLAT 100",postCodeResponse.getResults().get(0).getDpa().getBuildingNumber());
    }

    @Test
    void shouldReturnFullAddressWhenGivenPostCodeIsValid() throws JsonProcessingException {

        PostCodeResult result =
            PostCodeResult.builder()
                .dpa(
                    AddressDetails.builder()
                        .address("Flat 100 ABC Regent Road")
                        .buildingNumber("FLAT 100")
                        .countryCode("UK")
                        .thoroughfareName("thoroughname")
                        .postTown("Nor")
                        .postcode("TW5 1BC")
                        .status("Active")
                        .countryCodeDescription("description")
                        .postalAddressCode("code")
                        .localCustomerCodeDescription("local")
                        .build())
                .build();
        ObjectMapper mapper = new ObjectMapper();

        PostCodeResponse res = PostCodeResponse.builder().results(Arrays.asList(result)).build();
        String resultJson = mapper.writeValueAsString(res);
        ResponseEntity<String> responseEntity = ResponseEntity.ok(resultJson);

        when(objectMapper.readValue(anyString(), eq(PostCodeResponse.class))).thenReturn(res);
        when(restTemplatePostCodeLookUp.exchange(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any()))
            .thenReturn(responseEntity);

        PostCodeResponse postCodeResponse = notificationServiceImpl.getAddress("TW5 1BC");
        assertEquals("FLAT 100",postCodeResponse.getResults().get(0).getDpa().getBuildingNumber());
        assertEquals("Flat 100 ABC Regent Road",postCodeResponse.getResults().get(0).getDpa().getAddress());
        assertEquals("UK",postCodeResponse.getResults().get(0).getDpa().getCountryCode());
        assertEquals("thoroughname",postCodeResponse.getResults().get(0).getDpa().getThoroughfareName());
        assertEquals("Nor",postCodeResponse.getResults().get(0).getDpa().getPostTown());
        assertEquals("TW5 1BC",postCodeResponse.getResults().get(0).getDpa().getPostcode());
        assertEquals("Active",postCodeResponse.getResults().get(0).getDpa().getStatus());
        assertEquals("description",postCodeResponse.getResults().get(0).getDpa().getCountryCodeDescription());
        assertEquals("code",postCodeResponse.getResults().get(0).getDpa().getPostalAddressCode());
        assertEquals("local",postCodeResponse.getResults().get(0).getDpa().getLocalCustomerCodeDescription());
    }

    @Test
    void shouldReturnExceptionWhenUrlIsEmpty() {
        when(postcodeLookupConfiguration.getUrl()).thenReturn(null);
        assertThrows(
            PostCodeLookUpException.class,
            () -> notificationServiceImpl.getAddress("TW5 1BC"));
    }

    @Test
    void shouldReturnExceptionWhenKeyIsEmpty() {
        when(postcodeLookupConfiguration.getAccessKey()).thenReturn("");
        assertThrows(
            PostCodeLookUpException.class,
            () -> notificationServiceImpl.getAddress("TW5 1BC"));
    }

    @Test
    void testGetAddressError400ForPostCodeLookupNotFoundException() {

        ResponseEntity<String> responseEntity = new ResponseEntity<String>("res", HttpStatus.valueOf(404));

        when(restTemplatePostCodeLookUp.exchange(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any()))
            .thenReturn(responseEntity);

        assertThrows(PostCodeLookUpException.class, () -> notificationServiceImpl.getAddress("ABC 1BC"));

    }

    @Test
    void testGetRefundReasonSuccessWithSendEmailNotification() throws NotificationClientException {
        mockUserinfoCall(idamUserIDResponseSupplier.get());
        final RefundNotificationEmailRequest request = RefundNotificationEmailRequest.refundNotificationEmailRequestWith()
            .notificationType(NotificationType.EMAIL)
            .templateId("test")
            .reference("REF-123")
            .recipientEmailAddress("test@test.com")
            .personalisation(
                Personalisation.personalisationRequestWith().ccdCaseNumber("1600162727220633").refundReference("RF-1234-1234-1234-1234").refundAmount(
                    BigDecimal.valueOf(10)).refundReason("test-code").build())
            .build();
        when(serviceContactRepository.findByServiceName(any()))
            .thenReturn(Optional.of(ServiceContact.serviceContactWith().id(1).serviceName("Probate").serviceMailbox("probate@gov.uk").build()));
        when(notificationRefundReasonRepository.findByRefundReasonCode(any()))
            .thenReturn(Optional.of(NotificationRefundReasons.notificationRefundReasonWith()
                                        .refundReasonNotification("There has been an amendment to your claim").build()));
        SendEmailResponse response = new SendEmailResponse(
            "{\"content\":{\"body\":\"Hello Unknown, your reference is string\\r\\n\\r\\nRefund Approved\\"
                + "r\\n\\r\\nThanks\",\"from_email\":\"test@gov.uk\",\"subject\":"
                + "\"Refund Notification Approval\"},\"id\":\"10f101e0-6ab8-4a83-8ebd-124d648dd282\","
                + "\"reference\":\"string\",\"scheduled_for\":null,\"template\":"
                + "{\"id\":\"10f101e0-6ab8-4a83-8ebd-124d648dd282\",\"uri\":"
                + "\"https://api.notifications.service.gov.uk/services\""
                + ",\"version\":1},\"uri\":\"https://api.notifications.service.gov.uk\"}\n");
        Notification notification = Notification.builder().build();

        TemplatePreview templatePreview = new TemplatePreview(
            "{                                                             "
                + "\"id\": \"1222960c-4ffa-42db-806c-451a68c56e09\","
                + "\"type\": \"email\","
                + "\"version\": 11,"
                + "\"body\": \"Dear Sir/Madam\","
                + "\"subject\": \"HMCTS refund request approved\","
                + "\"html\": \"Dear Sir/Madam\","
                + "}");
        when(notificationEmailClient.generateTemplatePreview(any(), anyMap())).thenReturn(templatePreview);
        when(notificationTemplateResponseMapper.toFromMapper(any(), any())).thenReturn(FromTemplateContact
                                                                                           .buildFromTemplateContactWith()
                                                                                           .fromEmailAddress("test@test.com")
                                                                                           .build());
        when(notificationEmailClient.sendEmail(any(), any(), any(), any())).thenReturn(response);
        when(emailNotificationMapper.emailResponseMapper(any(),any())).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(notification);

        response = notificationServiceImpl.sendEmailNotification(request,any());

        assertEquals("Refund Notification Approval", response.getSubject());
        assertEquals("test@gov.uk", response.getFromEmail().get());

    }

    @Test
    void testGetRefundReasonThrowExceptionWithSendEmailNotification() throws NotificationClientException {
        mockUserinfoCall(idamUserIDResponseSupplier.get());
        final RefundNotificationEmailRequest request = RefundNotificationEmailRequest.refundNotificationEmailRequestWith()
            .notificationType(NotificationType.EMAIL)
            .templateId("test")
            .reference("REF-123")
            .recipientEmailAddress("test@test.com")
            .personalisation(
                Personalisation
                    .personalisationRequestWith()
                    .ccdCaseNumber("1600162727220633")
                    .refundReference("RF-1234-1234-1234-1234")
                    .refundAmount(BigDecimal.valueOf(10))
                    .refundReason("test-code")
                    .build())
            .build();
        when(serviceContactRepository.findByServiceName(any()))
            .thenReturn(Optional.of(ServiceContact.serviceContactWith().id(1).serviceName("Probate").serviceMailbox("probate@gov.uk").build()));

        when(notificationRefundReasonRepository.findByRefundReasonCode(any()))
            .thenReturn(Optional.empty());
        SendEmailResponse response = new SendEmailResponse(
            "{\"content\":{\"body\":\"Hello Unknown, your reference is string\\r\\n\\r\\nRefund Approved\\"
                + "r\\n\\r\\nThanks\",\"from_email\":\"test@gov.uk\",\"subject\":"
                + "\"Refund Notification Approval\"},\"id\":\"10f101e0-6ab8-4a83-8ebd-124d648dd282\","
                + "\"reference\":\"string\",\"scheduled_for\":null,\"template\":"
                + "{\"id\":\"10f101e0-6ab8-4a83-8ebd-124d648dd282\",\"uri\":"
                + "\"https://api.notifications.service.gov.uk/services\""
                + ",\"version\":1},\"uri\":\"https://api.notifications.service.gov.uk\"}\n");
        Notification notification = Notification.builder().build();

        TemplatePreview templatePreview = new TemplatePreview(
            "{                                                             "
                + "\"id\": \"1222960c-4ffa-42db-806c-451a68c56e09\","
                + "\"type\": \"email\","
                + "\"version\": 11,"
                + "\"body\": \"Dear Sir/Madam\","
                + "\"subject\": \"HMCTS refund request approved\","
                + "\"html\": \"Dear Sir/Madam\","
                + "}");
        when(notificationEmailClient.generateTemplatePreview(any(), anyMap())).thenReturn(templatePreview);
        when(notificationTemplateResponseMapper.toFromMapper(any(), any())).thenReturn(FromTemplateContact
                                                                                           .buildFromTemplateContactWith()
                                                                                           .fromEmailAddress("test@test.com")
                                                                                           .build());
        when(notificationEmailClient.sendEmail(any(), any(), any(), any())).thenReturn(response);
        when(emailNotificationMapper.emailResponseMapper(any(),any())).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(notification);

        assertThrows(RefundReasonNotFoundException.class, () -> notificationServiceImpl.sendEmailNotification(request, any()
        ));
    }


    @Test
    void deleteNotification_shouldNotThrow_whenRecordsDeleted() {
        NotificationRepository mockRepo = mock(NotificationRepository.class);
        NotificationServiceImpl service = new NotificationServiceImpl();
        service.notificationRepository = mockRepo;

        when(mockRepo.deleteByReference("RF-123")).thenReturn(1L);

        assertDoesNotThrow(() -> service.deleteNotification("RF-123"));
    }

    @Test
    void deleteNotification_shouldThrow_whenNoRecordsDeleted() {
        NotificationRepository mockRepo = mock(NotificationRepository.class);
        NotificationServiceImpl service = new NotificationServiceImpl();
        service.notificationRepository = mockRepo;

        when(mockRepo.deleteByReference("RF-123")).thenReturn(0L);

        NotificationNotFoundException ex = assertThrows(
            NotificationNotFoundException.class,
            () -> service.deleteNotification("RF-123")
        );
        assertEquals("No records found for given refund reference", ex.getMessage());
    }

    @Test
    void previewNotification_shouldThrowException_whenNotificationClientExceptionOccurs() throws NotificationClientException {
        DocPreviewRequest request = mock(DocPreviewRequest.class);
        Personalisation personalisation = mock(Personalisation.class);
        when(personalisation.getRefundReason()).thenReturn("test");
        when(request.getPersonalisation()).thenReturn(personalisation);
        when(request.getNotificationType()).thenReturn(NotificationType.EMAIL);
        when(request.getPersonalisation().getRefundReason()).thenReturn(String.valueOf(Personalisation.personalisationRequestWith().refundReason("test").build()));
        when(request.getServiceName()).thenReturn("Probate");
        when(serviceContactRepository.findByServiceName(any())).thenReturn(Optional.of(ServiceContact.serviceContactWith().serviceMailbox("mailbox").build()));
        when(notificationRefundReasonRepository.findByRefundReasonCode(any())).thenReturn(Optional.of(NotificationRefundReasons.notificationRefundReasonWith().refundReasonNotification("reason").build()));
        when(request.getPaymentChannel()).thenReturn("bulk scan");
        when(request.getPaymentMethod()).thenReturn("cash");
        when(request.getRecipientPostalAddress()).thenReturn(null);
        when(request.getPersonalisation().getCcdCaseNumber()).thenReturn("ccd");
        when(notificationEmailClient.generateTemplatePreview(any(), anyMap()))
            .thenThrow(new NotificationClientException("error"));

        assertThrows(
            RuntimeException.class,
            () -> notificationServiceImpl.previewNotification(request, map)
        );
    }


    @Test
    void testGetInstructionType_bulkScan() throws Exception {
        NotificationServiceImpl service = new NotificationServiceImpl();
        Method method = NotificationServiceImpl.class.getDeclaredMethod("getInstructionType", String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, "bulk scan", "cheque");
        assertEquals("SendRefund", result);

        result = (String) method.invoke(service, "bulk scan", "cash");
        assertEquals("RefundWhenContacted", result);

        result = (String) method.invoke(service, "bulk scan", "postal order");
        assertEquals("RefundWhenContacted", result);

        result = (String) method.invoke(service, "other", "card");
        assertEquals("SendRefund", result);
    }
}
