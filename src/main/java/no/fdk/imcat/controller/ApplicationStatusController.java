package no.fdk.imcat.controller;

import lombok.RequiredArgsConstructor;
import no.fdk.imcat.service.InformationmodelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class ApplicationStatusController {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationStatusController.class);

    private final InformationmodelRepository informationmodelRepository;

    @GetMapping("/ping")
    public ResponseEntity<Void> ping() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ready")
    public ResponseEntity<Void> ready() {
        try {
            informationmodelRepository.count();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
}