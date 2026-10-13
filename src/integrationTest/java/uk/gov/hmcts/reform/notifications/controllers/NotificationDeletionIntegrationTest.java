package uk.gov.hmcts.reform.notifications.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.notifications.config.security.idam.IdamRepository;
import uk.gov.hmcts.reform.notifications.model.ContactDetails;
import uk.gov.hmcts.reform.notifications.model.Notification;
import uk.gov.hmcts.reform.notifications.repository.ContactDetailsRepository;
import uk.gov.hmcts.reform.notifications.repository.NotificationRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:notifications;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.liquibase.enabled=false",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationDeletionIntegrationTest {

    private static final String REFERENCE = "RF-1756-8279-1771-7688";
    private static final String OTHER_REFERENCE = "RF-9999-9999-9999-9999";
    private static final String SERVICE_HEADER = "ServiceAuthorization";
    private static final String DISPOSER_FEE_AND_PAY = "disposer-fee-and-pay";
    private static final String DISPOSER_ROLE = "disposer-payment-user";
    private static final String DISPOSER_USER_TOKEN = "disposer-user-token";
    private static final String PAYMENTS_USER_TOKEN = "payments-user-token";
    private static final String UNAUTHORISED_USER_TOKEN = "refund-user-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ContactDetailsRepository contactDetailsRepository;

    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private IdamRepository idamRepository;

    @BeforeEach
    void setUp() {
        when(serviceAuthorisationApi.getServiceName(anyString())).thenReturn(DISPOSER_FEE_AND_PAY);
        when(jwtDecoder.decode(DISPOSER_USER_TOKEN)).thenReturn(jwt(DISPOSER_USER_TOKEN));
        when(jwtDecoder.decode(PAYMENTS_USER_TOKEN)).thenReturn(jwt(PAYMENTS_USER_TOKEN));
        when(jwtDecoder.decode(UNAUTHORISED_USER_TOKEN)).thenReturn(jwt(UNAUTHORISED_USER_TOKEN));
        when(idamRepository.getUserInfo(DISPOSER_USER_TOKEN)).thenReturn(userInfo(List.of(DISPOSER_ROLE)));
        when(idamRepository.getUserInfo(PAYMENTS_USER_TOKEN)).thenReturn(userInfo(List.of("payments")));
        when(idamRepository.getUserInfo(UNAUTHORISED_USER_TOKEN)).thenReturn(userInfo(List.of("payments-refund")));
        contactDetailsRepository.deleteAll();
        notificationRepository.deleteAll();
    }

    @Test
    void deletesContactDetailsAndNotificationForReference() throws Exception {
        saveNotification(REFERENCE, "deleted@example.com");
        saveNotification(OTHER_REFERENCE, "kept@example.com");

        mockMvc.perform(delete("/notifications/v2/{reference}", REFERENCE)
                .header(AUTHORIZATION, "Bearer " + DISPOSER_USER_TOKEN)
                .header(SERVICE_HEADER, "Bearer disposer-token"))
            .andExpect(status().isNoContent());

        assertThat(notificationsFor(REFERENCE)).isEmpty();
        assertThat(notificationsFor(OTHER_REFERENCE)).hasSize(1);
        assertThat(contactDetailsRepository.count()).isEqualTo(1);
    }

    @Test
    void returnsNoContentWhenReferenceDoesNotExist() throws Exception {
        mockMvc.perform(delete("/notifications/v2/{reference}", "RF-0000-0000-0000-0001")
                .header(AUTHORIZATION, "Bearer " + DISPOSER_USER_TOKEN)
                .header(SERVICE_HEADER, "Bearer disposer-token"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deletesNotificationWhenCallerHasPaymentsRole() throws Exception {
        saveNotification(REFERENCE, "deleted@example.com");

        mockMvc.perform(delete("/notifications/v2/{reference}", REFERENCE)
                .header(AUTHORIZATION, "Bearer " + PAYMENTS_USER_TOKEN)
                .header(SERVICE_HEADER, "Bearer disposer-token"))
            .andExpect(status().isNoContent());

        assertThat(notificationsFor(REFERENCE)).isEmpty();
    }

    @Test
    void rejectsUserWithoutPaymentsOrDisposerRole() throws Exception {
        saveNotification(REFERENCE, "kept@example.com");

        mockMvc.perform(delete("/notifications/v2/{reference}", REFERENCE)
                .header(AUTHORIZATION, "Bearer " + UNAUTHORISED_USER_TOKEN)
                .header(SERVICE_HEADER, "Bearer disposer-token"))
            .andExpect(status().isForbidden());

        assertThat(notificationsFor(REFERENCE)).hasSize(1);
    }

    @Test
    void rejectsMissingServiceAuthorisation() throws Exception {
        mockMvc.perform(delete("/notifications/v2/{reference}", REFERENCE)
                .header(AUTHORIZATION, "Bearer " + DISPOSER_USER_TOKEN))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnknownService() throws Exception {
        when(serviceAuthorisationApi.getServiceName(anyString())).thenReturn("unknown_service");

        mockMvc.perform(delete("/notifications/v2/{reference}", REFERENCE)
                .header(AUTHORIZATION, "Bearer " + DISPOSER_USER_TOKEN)
                .header(SERVICE_HEADER, "Bearer unknown-token"))
            .andExpect(status().isForbidden());
    }

    private Jwt jwt(String token) {
        return Jwt.withTokenValue(token)
            .header("alg", "none")
            .claim("tokenName", "access_token")
            .subject("disposer-user")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    }

    private UserInfo userInfo(List<String> roles) {
        return UserInfo.builder()
            .sub("disposer-user")
            .uid("disposer-user")
            .roles(roles)
            .build();
    }

    private void saveNotification(String reference, String email) {
        Notification notification = notificationRepository.save(notification(reference));
        ContactDetails contactDetails = ContactDetails.contactDetailsWith()
            .email(email)
            .createdBy("disposer")
            .notification(notification)
            .build();
        notification.setContactDetails(contactDetails);
        contactDetailsRepository.save(contactDetails);
    }

    private Notification notification(String reference) {
        return Notification.builder()
            .reference(reference)
            .notificationType("EMAIL")
            .templateId("template-id")
            .createdBy("disposer")
            .build();
    }

    private List<Notification> notificationsFor(String reference) {
        Optional<List<Notification>> notifications =
            notificationRepository.findByReferenceOrderByDateUpdatedDesc(reference);
        return notifications.orElse(List.of());
    }
}
