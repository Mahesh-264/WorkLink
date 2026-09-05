package com.worklink.entity;

import com.worklink.entity.enums.SkillImportance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Join entity mapping Project to required/preferred Skill specifications.
 * Essential for the context-aware matching engine.
 */
@Entity
@Table(
        name = "project_skills",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_project_skill", columnNames = {"project_id", "skill_id"})
        }
)
public class ProjectSkill extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "importance", nullable = false, length = 30)
    private SkillImportance importance = SkillImportance.REQUIRED;

    public ProjectSkill() {
    }

    public ProjectSkill(Project project, Skill skill, SkillImportance importance) {
        this.project = project;
        this.skill = skill;
        this.importance = importance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public SkillImportance getImportance() {
        return importance;
    }

    public void setImportance(SkillImportance importance) {
        this.importance = importance;
    }
}
