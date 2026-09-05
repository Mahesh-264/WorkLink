package com.worklink.repository;

import com.worklink.entity.Project;
import com.worklink.entity.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByClientId(Long clientId);
    List<Project> findByStatus(ProjectStatus status);
    java.util.Optional<Project> findByIdAndClientId(Long id, Long clientId);
}
