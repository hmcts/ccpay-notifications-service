package uk.gov.hmcts.reform.notifications.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.notifications.service.NotificationDeletionService;

@RestController
@Tag(name = "Notification deletion", description = "Delete refund notifications for retain and dispose")
public class NotificationDeletionController {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationDeletionController.class);

    private final NotificationDeletionService notificationDeletionService;

    public NotificationDeletionController(NotificationDeletionService notificationDeletionService) {
        this.notificationDeletionService = notificationDeletionService;
    }

    @Operation(
        summary = "Delete notifications for a refund reference",
        description = "Deletes contact details and notifications for the refund reference. "
            + "Requires a service token for an authorised service, including disposer-fee-and-pay, "
            + "and the payments role or the disposer-payment-user role. "
            + "Returns 204 when the reference has already been deleted."
    )
    @ApiResponse(responseCode = "204", description = "Notifications deleted, or none found for the reference")
    @ApiResponse(responseCode = "403", description = "Service or user authorisation failed")
    @ApiResponse(responseCode = "500", description = "Unexpected error occurred during deletion")
    @DeleteMapping("/notifications/v2/{reference}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotification(@PathVariable("reference") String reference) {
        LOG.info("Delete notifications requested for reference {}", reference);
        notificationDeletionService.deleteByReference(reference);
    }
}
