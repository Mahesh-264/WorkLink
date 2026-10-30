package com.worklink.dto.capability;

import com.worklink.entity.enums.SkillProficiency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FreelancerSkillByNameRequest(
        @NotBlank(message = "Skill name is required")
        @Size(max = 100, message = "Skill name must be 100 characters or fewer")
        String name,
        @NotNull(message = "Proficiency is required")
        SkillProficiency proficiency
) {}
