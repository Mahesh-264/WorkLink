package com.worklink.repository;

import com.worklink.entity.PortfolioProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioProjectRepository extends JpaRepository<PortfolioProject, Long> {
    List<PortfolioProject> findByFreelancerId(Long freelancerId);
    Optional<PortfolioProject> findByIdAndFreelancerId(Long id, Long freelancerId);
}
