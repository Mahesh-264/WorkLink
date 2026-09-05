package com.worklink.controller;

import com.worklink.dto.profile.FreelancerProfileRequest;
import com.worklink.dto.profile.FreelancerProfileResponse;
import com.worklink.service.FreelancerProfileService;
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
@RequestMapping("/api/freelancer/profile")
@PreAuthorize("hasRole('FREELANCER')")
public class FreelancerProfileController {
    private final FreelancerProfileService profileService;
    public FreelancerProfileController(FreelancerProfileService profileService) { this.profileService = profileService; }

    @GetMapping
    public FreelancerProfileResponse getCurrentProfile() { return profileService.getCurrentProfile(); }
    @PostMapping
    public ResponseEntity<FreelancerProfileResponse> createCurrentProfile(@Valid @RequestBody FreelancerProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.createCurrentProfile(request));
    }
    @PutMapping
    public FreelancerProfileResponse updateCurrentProfile(@Valid @RequestBody FreelancerProfileRequest request) {
        return profileService.updateCurrentProfile(request);
    }
}
