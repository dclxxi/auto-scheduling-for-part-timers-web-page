package sketcher.scheduling.algorithm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Weight {
    LEVEL3,
    LEVEL2,
    LEVEL1;

    public static Weight getLast() {
        Weight[] weights = values();
        return weights[weights.length - 1];
    }
}
