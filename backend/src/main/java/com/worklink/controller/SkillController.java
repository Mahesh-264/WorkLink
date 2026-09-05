package com.worklink.controller;

import com.worklink.dto.capability.SkillResponse;
import com.worklink.service.FreelancerCapabilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {
    private final FreelancerCapabilityService service;
    public SkillController(FreelancerCapabilityService service) { this.service = service; }
    @GetMapping public List<SkillResponse> getSkills() { return service.getSkills(); }
}
