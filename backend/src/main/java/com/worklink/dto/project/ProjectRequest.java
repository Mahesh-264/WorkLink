package com.worklink.dto.project;
import com.worklink.entity.enums.ProjectComplexity;
import com.worklink.entity.enums.SkillImportance;
import jakarta.validation.constraints.*;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.List;
public record ProjectRequest(@NotBlank @Size(max=200) String title, @NotBlank @Size(max=5000) String description,
 @NotNull Long categoryId, @NotNull @Min(0) Integer requiredExperience, @NotNull @DecimalMin("0.00") BigDecimal budgetMin,
 @NotNull @DecimalMin("0.00") BigDecimal budgetMax, @NotNull @Future LocalDate deadline, @NotNull ProjectComplexity complexity,
 @Size(max=3000) String deliverables, @NotNull List<ProjectSkillRequest> skills) {
 public record ProjectSkillRequest(@NotNull Long skillId, @NotNull SkillImportance importance) {}
}
