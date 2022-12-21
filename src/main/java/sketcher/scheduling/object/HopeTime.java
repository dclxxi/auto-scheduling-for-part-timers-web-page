package sketcher.scheduling.object;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum HopeTime {
    MORNING("오전", 6, 12),
    AFTERNOON("오후", 12, 18),
    EVENING("저녁", 18, 24),
    DAWN("새벽", 0, 6);

    private final String kor;
    private final int startTime;
    private final int finishTime;

    public static HopeTime getByTime(int time) {
        return Arrays.stream(values())
                .filter(hopeTime -> hopeTime.startTime <= time)
                .filter(hopeTime -> hopeTime.finishTime > time)
                .findAny().get();
    }

    private static final Map<Integer, HopeTime> BY_START_TIME =
            Stream.of(values()).collect(Collectors.toMap(HopeTime::getStartTime, Function.identity()));

    private static final Map<Integer, HopeTime> BY_FINISH_TIME =
            Stream.of(values()).collect(Collectors.toMap(HopeTime::getFinishTime, Function.identity()));

    public static Optional<HopeTime> valueOfStarTime(Integer start_time) {
        return Optional.ofNullable(BY_START_TIME.get(start_time));
    }

    public static Optional<HopeTime> valueOfFinishTime(Integer finish_time) {
        return Optional.ofNullable(BY_FINISH_TIME.get(finish_time));
    }
}