package uk.gov.hmcts.reform.notifications.dtos.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.notifications.dtos.enums.NotificationType;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DocPreviewRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private Personalisation buildValidPersonalisation() {
        return Personalisation.personalisationRequestWith()
            .ccdCaseNumber("1234-5678-9012")
            .refundAmount(new BigDecimal("10.00"))
            .refundReason("Duplicate payment")
            .refundReference("RF-123")
            .customerReference("CUST-001")
            .build();
    }

    private RecipientPostalAddress buildValidAddress() {
        return RecipientPostalAddress.recipientPostalAddressWith()
            .addressLine("221B Baker Street")
            .city("London")
            .county("Greater London")
            .country("UK")
            .postalCode("NW1 6XE")
            .build();
    }

    private DocPreviewRequest.DocPreviewRequestBuilder buildBaseValidRequest() {
        return DocPreviewRequest.docPreviewRequestWith()
            .paymentReference("PR-001")
            .paymentMethod("CARD")
            .paymentChannel("ONLINE")
            .serviceName("CMC")
            .recipientPostalAddress(buildValidAddress())
            .recipientEmailAddress("user@example.com")
            .notificationType(NotificationType.LETTER)
            .personalisation(buildValidPersonalisation());
    }

    @Test
    void valid_request_without_templateId_should_pass_validation() {
        DocPreviewRequest req = buildBaseValidRequest().build();
        Set<ConstraintViolation<DocPreviewRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), () -> "Unexpected violations: " + violations);
    }

    @Test
    void valid_request_with_templateId_should_pass_validation() {
        DocPreviewRequest req = buildBaseValidRequest()
            .templateId("2f6d2f51-8b6d-4c02-9f6a-2a5a2b0c1f0d")
            .build();
        Set<ConstraintViolation<DocPreviewRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), () -> "Unexpected violations: " + violations);
    }

    @Test
    void invalid_when_paymentMethod_missing_should_fail() {
        DocPreviewRequest req = buildBaseValidRequest()
            .paymentMethod(null)
            .build();
        Set<ConstraintViolation<DocPreviewRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("paymentMethod")));
    }

    @Test
    void invalid_when_paymentChannel_blank_should_fail() {
        DocPreviewRequest req = buildBaseValidRequest()
            .paymentChannel("")
            .build();
        Set<ConstraintViolation<DocPreviewRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("paymentChannel")));
    }

    @Test
    void invalid_when_serviceName_null_should_fail() {
        DocPreviewRequest req = buildBaseValidRequest()
            .serviceName(null)
            .build();
        Set<ConstraintViolation<DocPreviewRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("serviceName")));
    }

    @Test
    void invalid_when_notificationType_null_should_fail() {
        DocPreviewRequest req = buildBaseValidRequest()
            .notificationType(null)
            .build();
        Set<ConstraintViolation<DocPreviewRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("notificationType")));
    }

    @Test
    void invalid_when_personalisation_null_should_fail() {
        DocPreviewRequest req = buildBaseValidRequest()
            .personalisation(null)
            .build();
        Set<ConstraintViolation<DocPreviewRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("personalisation")));
    }

    @Test
    void invalid_when_nested_personalisation_missing_required_fields_should_fail() {
        Personalisation invalidPersonalisation = Personalisation.personalisationRequestWith()
            .ccdCaseNumber("")
            .refundAmount(null)
            .refundReason(null)
            .build();
        DocPreviewRequest req = buildBaseValidRequest()
            .personalisation(invalidPersonalisation)
            .build();
        Set<ConstraintViolation<DocPreviewRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        // Verify nested property paths are present
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("personalisation.ccdCaseNumber")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("personalisation.refundAmount")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("personalisation.refundReason")));
    }

    @Test
    void invalid_when_address_missing_required_fields_should_fail() {
        RecipientPostalAddress invalidAddress = RecipientPostalAddress.recipientPostalAddressWith()
            .addressLine(null)
            .city("")
            .county(null)
            .country("")
            .postalCode(null)
            .build();
        DocPreviewRequest req = buildBaseValidRequest()
            .recipientPostalAddress(invalidAddress)
            .build();
        Set<ConstraintViolation<DocPreviewRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("recipientPostalAddress.addressLine")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("recipientPostalAddress.city")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("recipientPostalAddress.county")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("recipientPostalAddress.country")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("recipientPostalAddress.postalCode")));
    }
}

