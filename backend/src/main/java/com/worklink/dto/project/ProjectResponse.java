package com.worklink.dto.project;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime; import java.util.List;
public record ProjectResponse(Long id,String title,String description,Long categoryId,String category,String clientCompany,Integer requiredExperience,BigDecimal budgetMin,BigDecimal budgetMax,LocalDate deadline,String complexity,String deliverables,String status,LocalDateTime createdAt,LocalDateTime updatedAt,List<Skill> skills) { public record Skill(Long skillId,String name,String importance) {} }
