package com.worklink.dto.profile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

public record FreelancerProfileRequest(
        @NotBlank(message = "Professional headline is required")
        @Size(max = 200, message = "Professional headline must not exceed 200 characters") String professionalHeadline,
        @Size(max = 3000, message = "Bio must not exceed 3000 characters") String bio,
        @NotBlank(message = "Location is required")
        @Size(max = 100, message = "Location must not exceed 100 characters") String location,
        @NotNull(message = "Years of experience is required")
        @Min(value = 0, message = "Years of experience cannot be negative") Integer yearsExperience,
        @NotNull(message = "Hourly rate is required")
        @DecimalMin(value = "0.00", inclusive = true, message = "Hourly rate cannot be negative") BigDecimal hourlyRate,
        @NotBlank(message = "Availability is required")
        @Size(max = 50, message = "Availability must not exceed 50 characters") String availability,
        @URL(message = "Resume information must be a valid URL")
        @Size(max = 500, message = "Resume information must not exceed 500 characters") String resumeInformation,
        @URL(message = "Profile photo URL must be a valid URL")
        @Size(max = 500, message = "Profile photo URL must not exceed 500 characters") String profilePhotoUrl
) {
}
