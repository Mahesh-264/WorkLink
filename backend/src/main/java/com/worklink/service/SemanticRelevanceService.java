package com.worklink.service;

import com.worklink.dto.matching.SemanticMatchResult;
import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.PortfolioProject;
import com.worklink.entity.Project;
import com.worklink.entity.enums.SkillImportance;
import com.worklink.repository.FreelancerSkillRepository;
import com.worklink.repository.PortfolioProjectRepository;
import com.worklink.repository.ProjectSkillRepository;
import com.worklink.service.semantic.SemanticProjectContext;
import com.worklink.service.semantic.SemanticRelevanceEngine;
import com.worklink.service.semantic.SemanticTextField;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Prepares persisted capability text and delegates scoring to the replaceable semantic engine. */
@Service
public class SemanticRelevanceService {
    private final ProjectSkillRepository projectSkills;
    private final FreelancerSkillRepository freelancerSkills;
    private final PortfolioProjectRepository portfolioProjects;
    private final SemanticRelevanceEngine engine;

    public SemanticRelevanceService(ProjectSkillRepository projectSkills,
            FreelancerSkillRepository freelancerSkills, PortfolioProjectRepository portfolioProjects,
            SemanticRelevanceEngine engine) {
        this.projectSkills = projectSkills;
        this.freelancerSkills = freelancerSkills;
        this.portfolioProjects = portfolioProjects;
        this.engine = engine;
    }

    public SemanticProjectContext prepareProject(Project project) {
        List<SemanticTextField> fields = new ArrayList<>();
        add(fields, project.getTitle(), 3.0);
        add(fields, project.getDescription(), 1.0);
        add(fields, project.getDeliverables(), 2.0);
        projectSkills.findByProjectId(project.getId()).forEach(skill ->
                add(fields, skill.getSkill().getName(), skillImportanceWeight(skill.getImportance())));
        return new SemanticProjectContext(project.getId(), List.copyOf(fields));
    }

    public SemanticMatchResult evaluate(SemanticProjectContext projectContext, FreelancerProfile freelancer) {
        List<SemanticTextField> fields = new ArrayList<>();
        add(fields, freelancer.getHeadline(), 3.0);
        add(fields, freelancer.getBio(), 1.0);
        freelancerSkills.findByFreelancerId(freelancer.getId()).forEach(skill ->
                add(fields, skill.getSkill().getName(), 3.0));
        List<PortfolioProject> portfolio = portfolioProjects.findByFreelancerId(freelancer.getId());
        for (PortfolioProject item : portfolio) {
            add(fields, item.getTitle(), 2.0);
            add(fields, item.getDescription(), 1.0);
            add(fields, item.getTechnologies(), 2.0);
            add(fields, item.getRole(), 1.5);
        }
        return engine.evaluate(projectContext.projectId(), freelancer.getId(), projectContext.fields(), List.copyOf(fields));
    }

    private static double skillImportanceWeight(SkillImportance importance) {
        if (importance == SkillImportance.REQUIRED) return 3.0;
        if (importance == SkillImportance.IMPORTANT) return 2.0;
        return 1.0;
    }

    private static void add(List<SemanticTextField> fields, String text, double weight) {
        if (text != null && !text.isBlank()) fields.add(new SemanticTextField(text, weight));
    }
}
