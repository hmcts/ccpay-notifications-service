package uk.gov.hmcts.reform.notifications.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "notification")
@ToString
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Reference has to be present")
    @ToString.Exclude
    private String reference;

    @ToString.Exclude
    @Column(name = "notification_type")
    private String notificationType;

    @ToString.Exclude
    @Column(name = "template_id")
    @NotNull(message = "Template ID has to be present")
    private String templateId;

    @CreationTimestamp
    @Column(name = "date_created")
    private Date dateCreated;

    @UpdateTimestamp
    @Column(name = "date_updated")
    private Date dateUpdated;

    @ToString.Exclude
    @Column(name = "created_by")
    private String createdBy;

    @ToString.Exclude
    @OneToOne(fetch = FetchType.EAGER, mappedBy = "notification", cascade = CascadeType.ALL)
    private ContactDetails contactDetails;

    @Type(JsonType.class)
    @Column(columnDefinition = "json", name = "template_preview")
    private TemplatePreviewDto templatePreview;
}
