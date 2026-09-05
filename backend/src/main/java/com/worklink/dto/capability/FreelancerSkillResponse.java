package com.worklink.dto.capability;

import com.worklink.entity.enums.SkillProficiency;

public record FreelancerSkillResponse(Long skillId, String name, String category, SkillProficiency proficiency) { }
