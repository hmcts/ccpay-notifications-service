package uk.gov.hmcts.reform.notifications.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.notifications.model.ContactDetails;

@Repository
public interface ContactDetailsRepository extends CrudRepository<ContactDetails, Integer> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = "DELETE FROM contact_details WHERE notification_id IN "
            + "(SELECT id FROM notification WHERE reference = :reference)",
        nativeQuery = true
    )
    int deleteByNotificationReference(@Param("reference") String reference);
}
