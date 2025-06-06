package uk.gov.hmcts.reform.notifications.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "notification_refund_reasons")
@Builder(builderMethodName = "notificationRefundReasonWith")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class NotificationRefundReasons {

    @Id
    @Column(name = "refund_reason_code", nullable = false)
    String refundReasonCode;

    @Column(name = "refund_reason", nullable = false)
    String refundReason;

    @Column(name = "refund_reason_notification", nullable = false)
    String refundReasonNotification;

}
