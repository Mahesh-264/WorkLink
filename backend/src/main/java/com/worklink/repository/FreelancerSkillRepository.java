package com.worklink.repository;

import com.worklink.entity.FreelancerSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreelancerSkillRepository extends JpaRepository<FreelancerSkill, Long> {
    List<FreelancerSkill> findByFreelancerId(Long freelancerId);
    Optional<FreelancerSkill> findByFreelancerIdAndSkillId(Long freelancerId, Long skillId);
    boolean existsByFreelancerIdAndSkillId(Long freelancerId, Long skillId);
}
