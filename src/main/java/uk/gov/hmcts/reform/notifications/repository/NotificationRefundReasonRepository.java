package uk.gov.hmcts.reform.notifications.repository;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.notifications.model.NotificationRefundReasons;

public interface NotificationRefundReasonRepository extends CrudRepository<NotificationRefundReasons, String> {

    Optional<NotificationRefundReasons> findByRefundReasonCode(String code);
}
