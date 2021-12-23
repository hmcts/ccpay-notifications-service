package uk.gov.hmcts.reform.notifications.controllers;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.notifications.dtos.request.EmailNotificationRequest;
import uk.gov.hmcts.reform.notifications.service.NotificationService;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@Api(tags = {"Notification Journey "})
@SuppressWarnings({"PMD.AvoidUncheckedExceptionsInSignatures", "PMD.AvoidDuplicateLiterals"})
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @PostMapping("/emailNotification")
    public SendEmailResponse emailNotification(
         @RequestHeader("Authorization") String authorization,
         @RequestHeader(required = false) MultiValueMap<String, String> headers,
         @RequestParam(required = true) String notficationTemplateId,
         @Valid @RequestBody EmailNotificationRequest request) throws NotificationClientException {
        SendEmailResponse sendEmailResponse = notificationService.sendEmailNotification(request, notficationTemplateId);
        return sendEmailResponse;
    }

    @GetMapping("/letterNotification")
    public ResponseEntity<String> letterNotification(
      //  @RequestHeader("Authorization") String authorization,
        @RequestHeader(required = false) MultiValueMap<String, String> headers,
        @RequestParam(required = false) String ccdCaseNumber) throws NotificationClientException {
        notificationService .sendLetterNotification();
        return new ResponseEntity<>(
            HttpStatus.OK
        );
    }

//    @PostMapping("/email-notification")
//    public ResponseEntity<>
}

