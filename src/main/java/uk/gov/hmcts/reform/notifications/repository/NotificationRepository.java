package uk.gov.hmcts.reform.notifications.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.notifications.model.Notification;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends CrudRepository<Notification, Integer> {

    Optional<List<Notification>> findByReferenceAndCreatedByOrderByDateUpdatedDesc(String reference,
                                                                                   String createdBy);

    long deleteByReferenceAndCreatedBy(String reference, String createdBy);

    @Query("select n from Notification n "
        + "where n.reference = ?1  AND n.notificationType = ?2 order by n.dateUpdated desc")
    Optional<List<Notification>> findByReferenceAndNotificationTypeOrderByDateUpdatedDesc(String reference,
                                                                                         String notificationType);
}
