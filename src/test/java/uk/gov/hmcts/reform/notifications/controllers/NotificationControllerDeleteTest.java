package uk.gov.hmcts.reform.notifications.controllers;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.notifications.exceptions.NotificationNotFoundException;
import uk.gov.hmcts.reform.notifications.service.NotificationService;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerDeleteTest {

    private static final String REFERENCE = "RF-123";

    @Mock
    private NotificationService notificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        NotificationController notificationController = new NotificationController();
        ReflectionTestUtils.setField(notificationController, "notificationService", notificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
            .setControllerAdvice(new ExceptionHandlers())
            .build();
    }

    @Test
    void deleteNotificationReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/notifications/{reference}", REFERENCE)
                .header(AUTHORIZATION, "Bearer token"))
            .andExpect(status().isNoContent());

        verify(notificationService).deleteNotification(REFERENCE);
    }

    @Test
    void deleteNotificationReturnsErrorWhenReferenceIsNotFound() throws Exception {
        doThrow(new NotificationNotFoundException("No records found for given refund reference"))
            .when(notificationService).deleteNotification(REFERENCE);

        ServletException exception = assertThrows(ServletException.class, () ->
            mockMvc.perform(delete("/notifications/{reference}", REFERENCE)
                .header(AUTHORIZATION, "Bearer token"))
        );
        assertInstanceOf(NotificationNotFoundException.class, exception.getCause());
    }
}
