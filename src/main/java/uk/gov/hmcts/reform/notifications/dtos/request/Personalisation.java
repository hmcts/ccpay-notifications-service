package uk.gov.hmcts.reform.notifications.dtos.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "personalisationRequestWith")
public class Personalisation {

    @NotNull(message = "ccdCaseNumber cannot be null")
    @NotEmpty(message = "ccdCaseNumber cannot be blank")
    private String ccdCaseNumber;

    private String refundReference;

    @NotNull(message = "Refund amount cannot be null")
    private BigDecimal refundAmount;

    @NotNull(message = "Refund reason cannot be null")
    @NotEmpty(message = "Refund reason cannot be blank")
    private String refundReason;

    private String customerReference;
}
