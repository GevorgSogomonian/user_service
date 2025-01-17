package school.faang.user_service.dto.recommendation;

import lombok.Data;
import org.springframework.stereotype.Component;
import school.faang.user_service.entity.RequestStatus;
import school.faang.user_service.entity.recommendation.SkillRequest;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Data
public class RecommendationRequestDto {
    private Long id;
    private String message;
    private RequestStatus status;
    private List<SkillRequest> skills;
    private Long requesterId;
    private Long receiverId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
