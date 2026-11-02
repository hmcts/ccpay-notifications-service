package uk.gov.hmcts.reform.notifications.controllers;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.notifications.exceptions.NotificationDeletionException;
import uk.gov.hmcts.reform.notifications.service.NotificationDeletionService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationDeletionControllerTest {

    private static final String REFERENCE = "RF-1756-8279-1771-7688";

    @Mock
    private NotificationDeletionService notificationDeletionService;

    private MockMvc mockMvc;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new NotificationDeletionController(notificationDeletionService))
            .setControllerAdvice(new ExceptionHandlers())
            .build();
        logger = (Logger) LoggerFactory.getLogger(NotificationDeletionController.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
    }

    @Test
    void deleteNotificationReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/notifications/v2/{reference}", REFERENCE))
            .andExpect(status().isNoContent());

        verify(notificationDeletionService).deleteByReference(REFERENCE);
        assertTrue(logAppender.list.stream().anyMatch(event ->
            event.getLevel() == Level.INFO
                && event.getFormattedMessage().equals(
                    "Delete notifications requested for reference " + REFERENCE)));
    }

    @Test
    void deleteNotificationReturnsNoContentWhenNothingWasDeleted() throws Exception {
        mockMvc.perform(delete("/notifications/v2/{reference}", "RF-0000-0000-0000-0000"))
            .andExpect(status().isNoContent());

        verify(notificationDeletionService).deleteByReference("RF-0000-0000-0000-0000");
    }

    @Test
    void deleteNotificationLogsAndReturnsErrorMessageWhenDeletionFails() throws Exception {
        doThrow(new NotificationDeletionException(
            NotificationDeletionService.DELETION_FAILED_MESSAGE,
            new DataAccessResourceFailureException("database unavailable")
        )).when(notificationDeletionService).deleteByReference(REFERENCE);

        mockMvc.perform(delete("/notifications/v2/{reference}", REFERENCE))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(NotificationDeletionService.DELETION_FAILED_MESSAGE));
    }
}
