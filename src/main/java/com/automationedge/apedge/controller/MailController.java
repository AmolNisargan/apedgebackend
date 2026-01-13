package com.automationedge.apedge.controller;

import com.automationedge.apedge.dto.MailRequest;
import com.automationedge.apedge.service.GenericMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailController {

    private final GenericMailService mailService;

    @PostMapping("/send")
    public ResponseEntity<String> sendMail(@RequestBody MailRequest request) {
        mailService.sendEmail(
                request.getTo(),
                request.getSubject(),
                request.getTemplateName(),
                request.getVariables()
        );
        return ResponseEntity.ok("Mail sent successfully to " + request.getTo());
    }
}
