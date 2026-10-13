package uk.gov.hmcts.reform.notifications.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.notifications.exceptions.NotificationDeletionException;
import uk.gov.hmcts.reform.notifications.repository.ContactDetailsRepository;
import uk.gov.hmcts.reform.notifications.repository.NotificationRepository;

@Service
public class NotificationDeletionService {

    public static final String DELETION_FAILED_MESSAGE = "Unexpected error occurred during deletion";

    private static final Logger LOG = LoggerFactory.getLogger(NotificationDeletionService.class);

    private final ContactDetailsRepository contactDetailsRepository;
    private final NotificationRepository notificationRepository;

    public NotificationDeletionService(ContactDetailsRepository contactDetailsRepository,
                                       NotificationRepository notificationRepository) {
        this.contactDetailsRepository = contactDetailsRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void deleteByReference(String reference) {
        try {
            int contactDetailsDeleted = contactDetailsRepository.deleteByNotificationReference(reference);
            long notificationsDeleted = notificationRepository.deleteByReference(reference);
            if (contactDetailsDeleted == 0 && notificationsDeleted == 0) {
                LOG.info("No notifications found to delete for reference {}", reference);
                return;
            }
            LOG.info(
                "Deleted {} contact detail record(s) and {} notification record(s) for reference {}",
                contactDetailsDeleted,
                notificationsDeleted,
                reference
            );
        } catch (RuntimeException exception) {
            LOG.error("Failed to delete notifications for reference {}", reference, exception);
            throw new NotificationDeletionException(DELETION_FAILED_MESSAGE, exception);
        }
    }
}
