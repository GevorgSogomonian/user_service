package school.faang.user_service.dto.mentorship;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectionDto {
    @NotBlank(message = "Description should not be null value")
    @Size(min = 1, max = 4096, message = "Rejection reason should be between 1 and 4096 characters")
    private String rejectionReason;
}
