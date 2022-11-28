package sketcher.scheduling.algorithm;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Schedule {
    private int id;
    private int time;
    private boolean M3;
    private Weight weight;
    private Manager manager;

    public Schedule(int id, int time, boolean M3, Weight weight) {
        this.id = id;
        this.time = time;
        this.M3 = M3;
        this.weight = weight;
    }

    public boolean isM3() {
        return M3;
    }

    public boolean isEqualsTime(int time) {
        return this.time == time;
    }
}
