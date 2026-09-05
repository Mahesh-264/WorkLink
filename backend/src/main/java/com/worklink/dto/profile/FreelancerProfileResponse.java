package com.worklink.dto.profile;

import java.math.BigDecimal;

public record FreelancerProfileResponse(
        Long id, String professionalHeadline, String bio, String location, Integer yearsExperience,
        BigDecimal hourlyRate, String availability, String resumeInformation, String profilePhotoUrl
) {
}
