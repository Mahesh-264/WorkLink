package com.worklink.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record ClientProfileRequest(
        @NotBlank(message = "Company name is required")
        @Size(max = 150, message = "Company name must not exceed 150 characters") String companyName,
        @Size(max = 2000, message = "Company description must not exceed 2000 characters") String companyDescription,
        @Size(max = 100, message = "Industry must not exceed 100 characters") String industry,
        @URL(message = "Website must be a valid URL")
        @Size(max = 255, message = "Website must not exceed 255 characters") String website,
        @Size(max = 100, message = "Location must not exceed 100 characters") String location,
        @NotBlank(message = "Contact information is required")
        @Size(max = 255, message = "Contact information must not exceed 255 characters") String contactInformation
) {
}
