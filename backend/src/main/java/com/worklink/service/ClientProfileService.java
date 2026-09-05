package com.worklink.service;

import com.worklink.dto.profile.ClientProfileRequest;
import com.worklink.dto.profile.ClientProfileResponse;
import com.worklink.entity.ClientProfile;
import com.worklink.entity.User;
import com.worklink.exception.ProfileAlreadyExistsException;
import com.worklink.exception.ProfileNotFoundException;
import com.worklink.repository.ClientProfileRepository;
import com.worklink.repository.UserRepository;
import com.worklink.security.AuthenticatedUser;
import com.worklink.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientProfileService {
    private final ClientProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public ClientProfileService(ClientProfileRepository profileRepository, UserRepository userRepository,
                                CurrentUserProvider currentUserProvider) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public ClientProfileResponse getCurrentProfile() {
        return toResponse(findCurrentProfile(currentUserProvider.requireCurrentUser()));
    }

    @Transactional
    public ClientProfileResponse createCurrentProfile(ClientProfileRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        if (profileRepository.existsByUserId(currentUser.id())) {
            throw new ProfileAlreadyExistsException();
        }
        User user = userRepository.findById(currentUser.id()).orElseThrow(ProfileNotFoundException::new);
        ClientProfile profile = new ClientProfile(user, request.companyName().trim(), trim(request.companyDescription()),
                trim(request.industry()), trim(request.location()), trim(request.website()));
        profile.setContactInformation(request.contactInformation().trim());
        return toResponse(profileRepository.save(profile));
    }

    @Transactional
    public ClientProfileResponse updateCurrentProfile(ClientProfileRequest request) {
        ClientProfile profile = findCurrentProfile(currentUserProvider.requireCurrentUser());
        profile.setCompanyName(request.companyName().trim());
        profile.setCompanyDescription(trim(request.companyDescription()));
        profile.setIndustry(trim(request.industry()));
        profile.setWebsite(trim(request.website()));
        profile.setLocation(trim(request.location()));
        profile.setContactInformation(request.contactInformation().trim());
        return toResponse(profileRepository.save(profile));
    }

    private ClientProfile findCurrentProfile(AuthenticatedUser user) {
        return profileRepository.findByUserId(user.id()).orElseThrow(ProfileNotFoundException::new);
    }

    private String trim(String value) { return value == null ? null : value.trim(); }

    private ClientProfileResponse toResponse(ClientProfile profile) {
        return new ClientProfileResponse(profile.getId(), profile.getCompanyName(), profile.getCompanyDescription(),
                profile.getIndustry(), profile.getWebsite(), profile.getLocation(), profile.getContactInformation());
    }
}
