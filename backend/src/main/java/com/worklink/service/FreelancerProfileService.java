package com.worklink.service;

import com.worklink.dto.profile.FreelancerProfileRequest;
import com.worklink.dto.profile.FreelancerProfileResponse;
import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.User;
import com.worklink.exception.ProfileAlreadyExistsException;
import com.worklink.exception.ProfileNotFoundException;
import com.worklink.repository.FreelancerProfileRepository;
import com.worklink.repository.UserRepository;
import com.worklink.security.AuthenticatedUser;
import com.worklink.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FreelancerProfileService {
    private final FreelancerProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public FreelancerProfileService(FreelancerProfileRepository profileRepository, UserRepository userRepository,
                                    CurrentUserProvider currentUserProvider) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public FreelancerProfileResponse getCurrentProfile() {
        return toResponse(findCurrentProfile(currentUserProvider.requireCurrentUser()));
    }

    @Transactional
    public FreelancerProfileResponse createCurrentProfile(FreelancerProfileRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        if (profileRepository.existsByUserId(currentUser.id())) {
            throw new ProfileAlreadyExistsException();
        }
        User user = userRepository.findById(currentUser.id()).orElseThrow(ProfileNotFoundException::new);
        FreelancerProfile profile = new FreelancerProfile(user, request.professionalHeadline().trim(), trim(request.bio()),
                request.location().trim(), request.yearsExperience(), request.hourlyRate(), request.availability().trim());
        profile.setResumeUrl(trim(request.resumeInformation()));
        profile.setProfileImageUrl(trim(request.profilePhotoUrl()));
        return toResponse(profileRepository.save(profile));
    }

    @Transactional
    public FreelancerProfileResponse updateCurrentProfile(FreelancerProfileRequest request) {
        FreelancerProfile profile = findCurrentProfile(currentUserProvider.requireCurrentUser());
        profile.setHeadline(request.professionalHeadline().trim());
        profile.setBio(trim(request.bio()));
        profile.setLocation(request.location().trim());
        profile.setYearsOfExperience(request.yearsExperience());
        profile.setHourlyRate(request.hourlyRate());
        profile.setAvailability(request.availability().trim());
        profile.setResumeUrl(trim(request.resumeInformation()));
        profile.setProfileImageUrl(trim(request.profilePhotoUrl()));
        return toResponse(profileRepository.save(profile));
    }

    private FreelancerProfile findCurrentProfile(AuthenticatedUser user) {
        return profileRepository.findByUserId(user.id()).orElseThrow(ProfileNotFoundException::new);
    }

    private String trim(String value) { return value == null ? null : value.trim(); }

    private FreelancerProfileResponse toResponse(FreelancerProfile profile) {
        return new FreelancerProfileResponse(profile.getId(), profile.getHeadline(), profile.getBio(), profile.getLocation(),
                profile.getYearsOfExperience(), profile.getHourlyRate(), profile.getAvailability(), profile.getResumeUrl(),
                profile.getProfileImageUrl());
    }
}
