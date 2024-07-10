package school.faang.user_service.service.mentorship;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import school.faang.user_service.dto.mentorship.MentorshipRequestDto;
import school.faang.user_service.dto.mentorship.MentorshipRequestFilterDto;
import school.faang.user_service.dto.mentorship.RejectionDto;
import school.faang.user_service.entity.MentorshipRequest;
import school.faang.user_service.entity.RequestStatus;
import school.faang.user_service.entity.User;
import school.faang.user_service.filter.mentorship.MentorshipRequestFilter;
import school.faang.user_service.mapper.mentorship.MentorshipRequestMapper;
import school.faang.user_service.repository.mentorship.MentorshipRequestRepository;
import school.faang.user_service.validator.MentorshipRequestValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MentorshipRequestService {
    private final MentorshipRequestMapper mentorshipRequestMapper;
    private final MentorshipRequestValidator mentorshipRequestValidator;
    private final MentorshipRequestRepository mentorshipRequestRepository;
    private final List<MentorshipRequestFilter> mentorshipRequestFilterList;

    @Transactional
    public MentorshipRequestDto requestMentorship(MentorshipRequestDto mentorshipRequestDto) {
        long requesterId = mentorshipRequestDto.getRequesterId();
        long receiverId = mentorshipRequestDto.getReceiverId();
        String description = mentorshipRequestDto.getDescription();
        LocalDateTime mentorshipCreationDate = mentorshipRequestDto.getCreatedAt();

        mentorshipRequestValidator.validateParticipantsAndRequestFrequency(requesterId, receiverId,
                mentorshipCreationDate);
        MentorshipRequest mentorshipRequest = mentorshipRequestRepository.create(requesterId, receiverId, description);
        return mentorshipRequestMapper.toDto(mentorshipRequest);
    }

    @Transactional(readOnly = true)
    public List<MentorshipRequestDto> getRequests(MentorshipRequestFilterDto filtersDto) {
        Stream<MentorshipRequest> allMatchedMentorshipRequests = mentorshipRequestRepository.findAll().stream();
        List<MentorshipRequestFilter> applicableMentorshipRequestFilters =
                mentorshipRequestFilterList.stream().filter(filter -> filter.isApplicable(filtersDto)).toList();

        for (MentorshipRequestFilter mentorshipRequestFilter : applicableMentorshipRequestFilters) {
            allMatchedMentorshipRequests = mentorshipRequestFilter.filter(allMatchedMentorshipRequests, filtersDto);
        }

        return mentorshipRequestMapper.toDtoList(allMatchedMentorshipRequests.toList());
    }

    @Transactional
    public MentorshipRequestDto acceptRequest(long requestId) {
        MentorshipRequest processedMentorshipRequest = processRequest(requestId, (mentorshipRequest) -> {
            User requester = mentorshipRequest.getRequester();
            User receiver = mentorshipRequest.getReceiver();
            mentorshipRequestValidator.validateReceiverIsNotMentorOfRequester(requester, receiver);
            requester.getMentors().add(receiver);
            receiver.getMentees().add(requester);
            mentorshipRequest.setStatus(RequestStatus.ACCEPTED);
        });

        return mentorshipRequestMapper.toDto(processedMentorshipRequest);
    }

    @Transactional
    public MentorshipRequestDto rejectRequest(long requestId, RejectionDto rejectionDto) {
        MentorshipRequest processedMentorshipRequest = processRequest(requestId, (mentorshipRequest) -> {
            mentorshipRequest.setRejectionReason(rejectionDto.getRejectionReason());
            mentorshipRequest.setStatus(RequestStatus.REJECTED);
        });

        return mentorshipRequestMapper.toDto(processedMentorshipRequest);
    }

    private MentorshipRequest processRequest(long requestId, Consumer<MentorshipRequest> requestStatusConsumer) {
        MentorshipRequest mentorshipRequest = mentorshipRequestRepository.findById(requestId).orElseThrow(() ->
                new IllegalArgumentException("Could not find Mentorship Request in database by id: " + requestId));
        mentorshipRequestValidator.validateRequestStatusIsPending(mentorshipRequest.getStatus());

        requestStatusConsumer.accept(mentorshipRequest);

        mentorshipRequest.setUpdatedAt(LocalDateTime.now());
        return mentorshipRequestRepository.save(mentorshipRequest);
    }
}