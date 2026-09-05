package com.worklink.entity;

import com.worklink.entity.enums.SkillProficiency;
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
 * Join entity mapping FreelancerProfile to Skill with verified proficiency level.
 */
@Entity
@Table(
        name = "freelancer_skills",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_freelancer_skill", columnNames = {"freelancer_id", "skill_id"})
        }
)
public class FreelancerSkill extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private FreelancerProfile freelancer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency_level", nullable = false, length = 30)
    private SkillProficiency proficiencyLevel = SkillProficiency.INTERMEDIATE;

    public FreelancerSkill() {
    }

    public FreelancerSkill(FreelancerProfile freelancer, Skill skill, SkillProficiency proficiencyLevel) {
        this.freelancer = freelancer;
        this.skill = skill;
        this.proficiencyLevel = proficiencyLevel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FreelancerProfile getFreelancer() {
        return freelancer;
    }

    public void setFreelancer(FreelancerProfile freelancer) {
        this.freelancer = freelancer;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public SkillProficiency getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(SkillProficiency proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }
}
