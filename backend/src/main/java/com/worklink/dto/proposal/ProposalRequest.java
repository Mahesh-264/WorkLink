package com.worklink.dto.proposal; import jakarta.validation.constraints.*; import java.math.BigDecimal;
public record ProposalRequest(@NotBlank @Size(max=3000) String coverLetter,@NotNull @DecimalMin("0.01") BigDecimal proposedBudget,@NotNull @Min(1) @Max(3650) Integer estimatedDuration){}
