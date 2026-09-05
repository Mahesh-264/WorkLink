package com.worklink.controller;

import com.worklink.dto.profile.ClientProfileRequest;
import com.worklink.dto.profile.ClientProfileResponse;
import com.worklink.service.ClientProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/profile")
@PreAuthorize("hasRole('CLIENT')")
public class ClientProfileController {
    private final ClientProfileService profileService;
    public ClientProfileController(ClientProfileService profileService) { this.profileService = profileService; }

    @GetMapping
    public ClientProfileResponse getCurrentProfile() { return profileService.getCurrentProfile(); }
    @PostMapping
    public ResponseEntity<ClientProfileResponse> createCurrentProfile(@Valid @RequestBody ClientProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.createCurrentProfile(request));
    }
    @PutMapping
    public ClientProfileResponse updateCurrentProfile(@Valid @RequestBody ClientProfileRequest request) {
        return profileService.updateCurrentProfile(request);
    }
}
