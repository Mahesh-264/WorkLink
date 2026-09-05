package com.worklink.dto.profile;

public record ClientProfileResponse(
        Long id, String companyName, String companyDescription, String industry,
        String website, String location, String contactInformation
) {
}
