package school.faang.user_service.service.recommendation;

import org.springframework.stereotype.Component;
import school.faang.user_service.dto.recommendation.RequestFilterDto;
import school.faang.user_service.entity.recommendation.RecommendationRequest;

import java.util.stream.Stream;

@Component
public interface RequestFilter {
    boolean isApplicable(RequestFilterDto filter);

    void apply(Stream<RecommendationRequest> recommendationRequests, RequestFilterDto filter);
}
