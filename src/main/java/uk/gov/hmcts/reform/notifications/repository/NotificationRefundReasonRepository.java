package uk.gov.hmcts.reform.notifications.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.notifications.model.NotificationRefundReasons;

import java.util.Optional;

public interface NotificationRefundReasonRepository extends CrudRepository<NotificationRefundReasons, String> {

    Optional<NotificationRefundReasons> findByRefundReasonCode(String code);
}
