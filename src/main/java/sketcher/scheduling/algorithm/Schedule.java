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
    private int weight;
    private boolean M3;
    private Manager manager;

    public Schedule(int id, Integer time, Integer weight, boolean M3) {
        this.id = id;
        this.time = time;
        this.weight = weight;
        this.M3 = M3;
    }

    public boolean isM3() {
        return M3;
    }

    public boolean isEqualsTime(int time) {
        return this.time == time;
    }
}
