package school.faang.user_service.service.recommendation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import school.faang.user_service.dto.recommendation.RecommendationRequestDto;
import school.faang.user_service.dto.recommendation.RejectionDto;
import school.faang.user_service.dto.recommendation.RequestFilterDto;
import school.faang.user_service.entity.recommendation.RecommendationRequest;
import school.faang.user_service.mapper.recommendation.RecommendationRequestMapper;
import school.faang.user_service.repository.UserRepository;
import school.faang.user_service.repository.recommendation.RecommendationRequestRepository;
import school.faang.user_service.repository.recommendation.SkillRequestRepository;
import school.faang.user_service.validator.recommendation.RecommendationRequestDtoValidator;
import school.faang.user_service.validator.recommendation.RecommendationRequestIdValidator;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static school.faang.user_service.entity.RequestStatus.PENDING;

@Service
@RequiredArgsConstructor
public class RecommendationRequestService {
    public final RecommendationRequestRepository recommendationRequestRepository;
    public final SkillRequestRepository skillRequestRepository;
    public final RecommendationRequestMapper recommendationRequestMapper;
    public final UserRepository userRepository;
    public final List<RequestFilter> requestFilters;
    public final RecommendationRequestDtoValidator recommendationRequestDtoValidator;
    public final RecommendationRequestIdValidator recommendationRequestIdValidator;

    public RecommendationRequestDto create(RecommendationRequestDto recommendationRequestDto) {
        recommendationRequestDtoValidator.validateMessage(recommendationRequestDto.getMessage());
        recommendationRequestDtoValidator.validateRequesterAndReceiverIds(recommendationRequestDto.getRequesterId(), recommendationRequestDto.getReceiverId());
        recommendationRequestDtoValidator.validateRequestTimeDifference(recommendationRequestDto.getCreatedAt(), recommendationRequestDto.getRequesterId(), recommendationRequestDto.getReceiverId());
        recommendationRequestDtoValidator.validateRequestedSkills(recommendationRequestDto.getSkills());

        RecommendationRequest recommendationRequest = recommendationRequestMapper.toEntity(recommendationRequestDto, userRepository);

        Long Id = recommendationRequestRepository.create(recommendationRequest.getRequester(),
                recommendationRequest.getReceiver(),
                recommendationRequest.getMessage());

        recommendationRequestDto.getSkills().forEach(skill -> skillRequestRepository.create(Id, skill.getId()));

        return recommendationRequestMapper.toDto(recommendationRequest, userRepository);
    }

    public List<RecommendationRequestDto> getRequests(RequestFilterDto filter) {
        Stream<RecommendationRequest> recommendations = StreamSupport.stream(recommendationRequestRepository.findAll().spliterator(), false);

        requestFilters.stream()
                .filter(requestFilter -> requestFilter.isApplicable(filter))
                .forEach(requestFilter -> requestFilter.apply(recommendations, filter));

        return recommendations.map(recommendation -> recommendationRequestMapper.toDto(recommendation, userRepository)).toList();
    }

    public RecommendationRequestDto getRequest(Long id) {
        recommendationRequestIdValidator.validateId(id);

        return recommendationRequestMapper.toDto(recommendationRequestRepository.findById(id).get(), userRepository);
    }

    public RecommendationRequestDto rejectRequest(Long id, RejectionDto rejection) {
        recommendationRequestIdValidator.validateId(id);

        RecommendationRequest recommendationRequest = recommendationRequestRepository.findById(id).get();

        if (recommendationRequest.getStatus() != PENDING) {
            throw new RuntimeException("Recommendation request with id " + id
                    + " already has a status: " + recommendationRequest.getStatus());
        }
        recommendationRequest.setRejectionReason(rejection.getReason());

        return recommendationRequestMapper.toDto(recommendationRequest, userRepository);
    }
}
