package school.faang.user_service.filter.event;

import school.faang.user_service.entity.event.Event;

import java.util.stream.Stream;

public interface EventFieldFilter {

    boolean isApplicable(EventFilterDto filter);

    Stream<Event> apply(Stream<Event> eventStream, EventFilterDto eventFilterDto);
}
