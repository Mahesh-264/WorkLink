package com.worklink.service;

import com.worklink.dto.capability.FreelancerSkillByNameRequest;
import com.worklink.dto.capability.FreelancerSkillRequest;
import com.worklink.dto.capability.FreelancerSkillResponse;
import com.worklink.dto.capability.PortfolioProjectRequest;
import com.worklink.dto.capability.PortfolioProjectResponse;
import com.worklink.dto.capability.SkillResponse;
import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.FreelancerSkill;
import com.worklink.entity.PortfolioProject;
import com.worklink.entity.Skill;
import com.worklink.exception.DuplicateResourceException;
import com.worklink.exception.ProfileNotFoundException;
import com.worklink.exception.ResourceNotFoundException;
import com.worklink.repository.FreelancerProfileRepository;
import com.worklink.repository.FreelancerSkillRepository;
import com.worklink.repository.PortfolioProjectRepository;
import com.worklink.repository.SkillRepository;
import com.worklink.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FreelancerCapabilityService {
    private final SkillRepository skillRepository;
    private final FreelancerSkillRepository freelancerSkillRepository;
    private final PortfolioProjectRepository portfolioRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final CurrentUserProvider currentUserProvider;

    public FreelancerCapabilityService(SkillRepository skillRepository, FreelancerSkillRepository freelancerSkillRepository,
                                       PortfolioProjectRepository portfolioRepository, FreelancerProfileRepository freelancerProfileRepository,
                                       CurrentUserProvider currentUserProvider) {
        this.skillRepository = skillRepository;
        this.freelancerSkillRepository = freelancerSkillRepository;
        this.portfolioRepository = portfolioRepository;
        this.freelancerProfileRepository = freelancerProfileRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getSkills() {
        return skillRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "name"))
                .stream().map(this::toSkillResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FreelancerSkillResponse> getCurrentSkills() {
        return freelancerSkillRepository.findByFreelancerId(currentProfile().getId()).stream().map(this::toFreelancerSkillResponse).toList();
    }

    @Transactional
    public FreelancerSkillResponse addSkill(FreelancerSkillRequest request) {
        FreelancerProfile profile = currentProfile();
        if (freelancerSkillRepository.existsByFreelancerIdAndSkillId(profile.getId(), request.skillId())) {
            throw new DuplicateResourceException("This skill is already on your profile");
        }
        Skill skill = skillRepository.findById(request.skillId()).orElseThrow(() -> new ResourceNotFoundException("Skill not found"));
        return toFreelancerSkillResponse(freelancerSkillRepository.save(new FreelancerSkill(profile, skill, request.proficiency())));
    }

    @Transactional
    public FreelancerSkillResponse addSkillByName(FreelancerSkillByNameRequest request) {
        FreelancerProfile profile = currentProfile();
        String trimmed = request.name().trim();
        // Find an existing catalog skill (case-insensitive) or create a new one on the fly
        Skill skill = skillRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> skillRepository.save(new Skill(trimmed, null)));
        if (freelancerSkillRepository.existsByFreelancerIdAndSkillId(profile.getId(), skill.getId())) {
            throw new DuplicateResourceException("This skill is already on your profile");
        }
        return toFreelancerSkillResponse(freelancerSkillRepository.save(new FreelancerSkill(profile, skill, request.proficiency())));
    }

    @Transactional
    public FreelancerSkillResponse updateSkill(Long skillId, FreelancerSkillRequest request) {
        if (!skillId.equals(request.skillId())) throw new IllegalArgumentException("Skill ID in the path must match the request");
        FreelancerSkill assignment = freelancerSkillRepository.findByFreelancerIdAndSkillId(currentProfile().getId(), skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill is not on your profile"));
        assignment.setProficiencyLevel(request.proficiency());
        return toFreelancerSkillResponse(freelancerSkillRepository.save(assignment));
    }

    @Transactional
    public void deleteSkill(Long skillId) {
        FreelancerSkill assignment = freelancerSkillRepository.findByFreelancerIdAndSkillId(currentProfile().getId(), skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill is not on your profile"));
        freelancerSkillRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public List<PortfolioProjectResponse> getCurrentPortfolio() {
        return portfolioRepository.findByFreelancerId(currentProfile().getId()).stream().map(this::toPortfolioResponse).toList();
    }

    @Transactional
    public PortfolioProjectResponse createPortfolioItem(PortfolioProjectRequest request) {
        PortfolioProject project = new PortfolioProject();
        project.setFreelancer(currentProfile());
        apply(project, request);
        return toPortfolioResponse(portfolioRepository.save(project));
    }

    @Transactional
    public PortfolioProjectResponse updatePortfolioItem(Long portfolioId, PortfolioProjectRequest request) {
        PortfolioProject project = portfolioRepository.findByIdAndFreelancerId(portfolioId, currentProfile().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio project not found"));
        apply(project, request);
        return toPortfolioResponse(portfolioRepository.save(project));
    }

    @Transactional
    public void deletePortfolioItem(Long portfolioId) {
        PortfolioProject project = portfolioRepository.findByIdAndFreelancerId(portfolioId, currentProfile().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio project not found"));
        portfolioRepository.delete(project);
    }

    private FreelancerProfile currentProfile() {
        return freelancerProfileRepository.findByUserId(currentUserProvider.requireCurrentUser().id())
                .orElseThrow(ProfileNotFoundException::new);
    }

    private void apply(PortfolioProject project, PortfolioProjectRequest request) {
        project.setTitle(request.title().trim()); project.setDescription(trim(request.description()));
        project.setProjectUrl(trim(request.projectUrl())); project.setRepositoryUrl(trim(request.repositoryUrl()));
        project.setTechnologies(trim(request.technologiesUsed())); project.setRole(trim(request.role()));
        project.setCompletionDate(request.completionDate()); project.setImageUrl(trim(request.imageUrl()));
    }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private SkillResponse toSkillResponse(Skill skill) { return new SkillResponse(skill.getId(), skill.getName(), skill.getCategory()); }
    private FreelancerSkillResponse toFreelancerSkillResponse(FreelancerSkill value) { return new FreelancerSkillResponse(value.getSkill().getId(), value.getSkill().getName(), value.getSkill().getCategory(), value.getProficiencyLevel()); }
    private PortfolioProjectResponse toPortfolioResponse(PortfolioProject value) { return new PortfolioProjectResponse(value.getId(), value.getTitle(), value.getDescription(), value.getProjectUrl(), value.getRepositoryUrl(), value.getTechnologies(), value.getRole(), value.getCompletionDate(), value.getImageUrl()); }
}
