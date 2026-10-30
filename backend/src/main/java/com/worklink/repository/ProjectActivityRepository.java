package com.worklink.repository;

import com.worklink.entity.ProjectActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectActivityRepository extends JpaRepository<ProjectActivity, Long> {
    List<ProjectActivity> findByProjectIdOrderByCreatedAtAsc(Long projectId);
}
