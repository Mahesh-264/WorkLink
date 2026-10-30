package com.worklink.service;

import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.FreelancerSkill;
import com.worklink.entity.PortfolioProject;
import com.worklink.entity.Project;
import com.worklink.entity.ProjectSkill;
import com.worklink.entity.Skill;
import com.worklink.entity.enums.SkillImportance;
import com.worklink.entity.enums.SkillProficiency;
import com.worklink.repository.FreelancerSkillRepository;
import com.worklink.repository.PortfolioProjectRepository;
import com.worklink.repository.ProjectSkillRepository;
import com.worklink.service.semantic.TfidfCosineSemanticRelevanceEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SemanticRelevanceServiceTest {
    private final ProjectSkillRepository projectSkills = mock(ProjectSkillRepository.class);
    private final FreelancerSkillRepository freelancerSkills = mock(FreelancerSkillRepository.class);
    private final PortfolioProjectRepository portfolioProjects = mock(PortfolioProjectRepository.class);
    private final SemanticRelevanceService service = new SemanticRelevanceService(projectSkills,
            freelancerSkills, portfolioProjects, new TfidfCosineSemanticRelevanceEngine());

    @Test void requiredProjectSkillsContributeToProjectRepresentation() {
        Project project = new Project(); project.setId(11L);
        ProjectSkill required = new ProjectSkill(project, new Skill("React", null), SkillImportance.REQUIRED);
        when(projectSkills.findByProjectId(11L)).thenReturn(List.of(required));
        FreelancerProfile freelancer = new FreelancerProfile(); freelancer.setId(22L); freelancer.setHeadline("React engineer");
        when(freelancerSkills.findByFreelancerId(22L)).thenReturn(List.of());
        when(portfolioProjects.findByFreelancerId(22L)).thenReturn(List.of());

        var result = service.evaluate(service.prepareProject(project), freelancer);

        assertThat(result.semanticAvailability()).isEqualTo("AVAILABLE");
        assertThat(result.semanticRelevanceScore()).isGreaterThan(0.0);
        verify(projectSkills).findByProjectId(11L);
    }

    @Test void portfolioTitleDescriptionAndTechnologiesContributeToFreelancerText() {
        Project project = new Project(); project.setId(12L); project.setTitle("E-commerce React storefront");
        when(projectSkills.findByProjectId(12L)).thenReturn(List.of());
        FreelancerProfile freelancer = new FreelancerProfile(); freelancer.setId(23L);
        when(freelancerSkills.findByFreelancerId(23L)).thenReturn(List.of());
        PortfolioProject portfolio = new PortfolioProject();
        portfolio.setTitle("Retail checkout"); portfolio.setDescription("Created commerce storefronts");
        portfolio.setTechnologies("React, e-commerce"); portfolio.setRole("Frontend engineer");
        when(portfolioProjects.findByFreelancerId(23L)).thenReturn(List.of(portfolio));

        var result = service.evaluate(service.prepareProject(project), freelancer);

        assertThat(result.semanticAvailability()).isEqualTo("AVAILABLE");
        assertThat(result.semanticRelevanceScore()).isGreaterThan(0.0);
        assertThat(result.freelancerTermsUsed()).contains("retail", "checkout", "commerce", "react", "frontend");
    }

    @Test void missingOptionalFieldsAndEmptyProfilesReturnUnavailable() {
        Project project = new Project(); project.setId(13L);
        when(projectSkills.findByProjectId(13L)).thenReturn(List.of());
        FreelancerProfile freelancer = new FreelancerProfile(); freelancer.setId(24L);
        when(freelancerSkills.findByFreelancerId(24L)).thenReturn(List.of());
        when(portfolioProjects.findByFreelancerId(24L)).thenReturn(List.of());

        var result = service.evaluate(service.prepareProject(project), freelancer);

        assertThat(result.semanticAvailability()).isEqualTo("UNAVAILABLE");
        assertThat(result.semanticSimilarity()).isNull();
        assertThat(result.semanticRelevanceScore()).isNull();
    }

    @Test void projectContextIsLoadedOnceAndReusedForFreelancers() {
        Project project = new Project(); project.setId(14L); project.setTitle("React work");
        when(projectSkills.findByProjectId(14L)).thenReturn(List.of());
        var context = service.prepareProject(project);
        FreelancerProfile one = new FreelancerProfile(); one.setId(25L); one.setHeadline("React developer");
        FreelancerProfile two = new FreelancerProfile(); two.setId(26L); two.setHeadline("React engineer");
        when(freelancerSkills.findByFreelancerId(anyLong())).thenReturn(List.of());
        when(portfolioProjects.findByFreelancerId(anyLong())).thenReturn(List.of());
        service.evaluate(context, one); service.evaluate(context, two);
        verify(projectSkills, times(1)).findByProjectId(14L);
    }
}
