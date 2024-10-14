package uk.gov.hmcts.reform.notifications.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import uk.gov.hmcts.reform.notifications.dtos.response.MailAddress;

@Entity
@Getter
@Setter
@ToString
@Builder(builderMethodName = "serviceContactWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "service_contact")
public class ServiceContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "service_name", nullable = false)
    String serviceName;

    @Column(name = "service_mailbox")
    String serviceMailbox;

    @Column(name = "from_email_address")
    private String fromEmailAddress;

    @Type(JsonType.class)
    @Column(columnDefinition = "json", name = "from_mail_address")
    private MailAddress fromMailAddress;
}
