package com.worklink.dto.capability;

import com.worklink.entity.enums.SkillProficiency;
import jakarta.validation.constraints.NotNull;

public record FreelancerSkillRequest(@NotNull(message = "Skill ID is required") Long skillId,
                                     @NotNull(message = "Proficiency is required") SkillProficiency proficiency) { }
