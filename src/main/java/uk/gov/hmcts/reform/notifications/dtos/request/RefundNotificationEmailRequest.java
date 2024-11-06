package uk.gov.hmcts.reform.notifications.dtos.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.notifications.dtos.enums.NotificationType;
import uk.gov.hmcts.reform.notifications.model.TemplatePreviewDto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "refundNotificationEmailRequestWith")
public class RefundNotificationEmailRequest {

    @NotNull(message = "Template ID cannot be null")
    @NotEmpty(message = "Template ID cannot be blank")
    private String templateId;

    @NotNull(message = "Recipient Email Address cannot be null")
    @NotEmpty(message = "Recipient Email Address cannot be blank")
    @Email(message = "Please enter a valid Email Address")
    private String recipientEmailAddress;

    @NotNull(message = "Reference cannot be null")
    @NotEmpty(message = "Reference cannot be blank")
    private String reference;

    private String emailReplyToId;

    @Schema(example = "EMAIL")
    @Value("EMAIL")
    private NotificationType notificationType;

    @NotNull
    @Valid
    private Personalisation personalisation;

    @NotNull(message = "service Name cannot be null")
    @NotEmpty(message = "Service cannot be blank")
    private String serviceName;

    private TemplatePreviewDto templatePreview;
}
