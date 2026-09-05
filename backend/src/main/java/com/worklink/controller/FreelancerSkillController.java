package com.worklink.controller;

import com.worklink.dto.capability.FreelancerSkillRequest;
import com.worklink.dto.capability.FreelancerSkillResponse;
import com.worklink.service.FreelancerCapabilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController @RequestMapping("/api/freelancer/skills") @PreAuthorize("hasRole('FREELANCER')")
public class FreelancerSkillController {
    private final FreelancerCapabilityService service;
    public FreelancerSkillController(FreelancerCapabilityService service) { this.service = service; }
    @GetMapping public List<FreelancerSkillResponse> getSkills() { return service.getCurrentSkills(); }
    @PostMapping public ResponseEntity<FreelancerSkillResponse> addSkill(@Valid @RequestBody FreelancerSkillRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.addSkill(request)); }
    @PutMapping("/{skillId}") public FreelancerSkillResponse updateSkill(@PathVariable Long skillId, @Valid @RequestBody FreelancerSkillRequest request) { return service.updateSkill(skillId, request); }
    @DeleteMapping("/{skillId}") public ResponseEntity<Void> deleteSkill(@PathVariable Long skillId) { service.deleteSkill(skillId); return ResponseEntity.noContent().build(); }
}
