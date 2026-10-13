package uk.gov.hmcts.reform.notifications.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import uk.gov.hmcts.reform.notifications.exceptions.NotificationDeletionException;
import uk.gov.hmcts.reform.notifications.repository.ContactDetailsRepository;
import uk.gov.hmcts.reform.notifications.repository.NotificationRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDeletionServiceTest {

    private static final String REFERENCE = "RF-1756-8279-1771-7688";

    @Mock
    private ContactDetailsRepository contactDetailsRepository;

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationDeletionService notificationDeletionService;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        notificationDeletionService = new NotificationDeletionService(
            contactDetailsRepository,
            notificationRepository
        );
        logger = (Logger) LoggerFactory.getLogger(NotificationDeletionService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
    }

    @Test
    void deletesContactDetailsBeforeNotifications() {
        when(contactDetailsRepository.deleteByNotificationReference(REFERENCE)).thenReturn(2);
        when(notificationRepository.deleteByReference(REFERENCE)).thenReturn(2L);

        notificationDeletionService.deleteByReference(REFERENCE);

        InOrder inOrder = inOrder(contactDetailsRepository, notificationRepository);
        inOrder.verify(contactDetailsRepository).deleteByNotificationReference(REFERENCE);
        inOrder.verify(notificationRepository).deleteByReference(REFERENCE);
        assertTrue(logAppender.list.stream().noneMatch(event -> event.getLevel() == Level.ERROR));
    }

    @Test
    void returnsWhenNoRecordsExist() {
        when(contactDetailsRepository.deleteByNotificationReference(REFERENCE)).thenReturn(0);
        when(notificationRepository.deleteByReference(REFERENCE)).thenReturn(0L);

        notificationDeletionService.deleteByReference(REFERENCE);

        verify(contactDetailsRepository).deleteByNotificationReference(REFERENCE);
        verify(notificationRepository).deleteByReference(REFERENCE);
        assertTrue(logAppender.list.stream().anyMatch(event ->
            event.getLevel() == Level.INFO
                && event.getFormattedMessage().contains("No notifications found")));
    }

    @Test
    void logsErrorAndThrowsWhenDeletionFails() {
        when(contactDetailsRepository.deleteByNotificationReference(REFERENCE))
            .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        NotificationDeletionException exception = assertThrows(
            NotificationDeletionException.class,
            () -> notificationDeletionService.deleteByReference(REFERENCE)
        );

        assertEquals(NotificationDeletionService.DELETION_FAILED_MESSAGE, exception.getMessage());
        verify(notificationRepository, never()).deleteByReference(REFERENCE);
        assertTrue(logAppender.list.stream().anyMatch(event ->
            event.getLevel() == Level.ERROR
                && event.getFormattedMessage().contains(REFERENCE)));
    }
}
