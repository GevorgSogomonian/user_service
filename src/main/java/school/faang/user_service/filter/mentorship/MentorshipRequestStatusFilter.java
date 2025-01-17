package school.faang.user_service.filter.mentorship;

import org.springframework.stereotype.Component;
import school.faang.user_service.dto.mentorship.MentorshipRequestFilterDto;
import school.faang.user_service.entity.MentorshipRequest;

import java.util.stream.Stream;

@Component
public class MentorshipRequestStatusFilter implements MentorshipRequestFilter {
    @Override
    public boolean isApplicable(MentorshipRequestFilterDto mentorshipRequestFilterDto) {
        return mentorshipRequestFilterDto.getStatus() != null;
    }

    @Override
    public Stream<MentorshipRequest> filter(Stream<MentorshipRequest> mentorshipRequestStream,
                                            MentorshipRequestFilterDto mentorshipRequestFilterDto) {
        return mentorshipRequestStream.filter(
                mentorshipRequest -> mentorshipRequest.getStatus() == mentorshipRequestFilterDto.getStatus());
    }
}
