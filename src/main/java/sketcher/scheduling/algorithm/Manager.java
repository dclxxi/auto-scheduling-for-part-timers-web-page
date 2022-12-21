package sketcher.scheduling.algorithm;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sketcher.scheduling.object.HopeTime;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Manager {
    private int code;
    private List<HopeTime> hopeTimes;
    private int totalAssignTime;
    private int dayAssignTime;
    private Weight weight;
    private List<Schedule> assignSchedules;

    public Manager(int code, int totalAssignTime, List<HopeTime> hopeTimes) {
        this.code = code;
        this.totalAssignTime = totalAssignTime;
        this.hopeTimes = List.copyOf(hopeTimes);
        this.assignSchedules = new ArrayList<>();
    }

    public int getHopeTimeCount() {
        return hopeTimes.size();
    }

    public Schedule findScheduleByTime(int time) {
        for (Schedule schedule : assignSchedules) {
            if (schedule.isEqualsTime(time)) {
                return schedule;
            }
        }

        return null;
    }

    public void updateAssignSchedules(Schedule currentNode, Schedule newNode) {
        if (isAlreadyAssign(currentNode)) {
            assignSchedules.remove(currentNode);
            assignSchedules.add(newNode);
            return;
        }

        assignSchedule(newNode);
    }

    private boolean isAlreadyAssign(Schedule currentNode) {
        return currentNode != null;
    }

    private void assignSchedule(Schedule newNode) {
        assignSchedules.add(newNode);
        totalAssignTime++;
        dayAssignTime++;
    }

    public boolean isExceedDayAssignTime(int dayAssignTime) {
        return this.dayAssignTime >= dayAssignTime;
    }

    public boolean isExceedToTalAssignTime(int totalAssignTime) {
        return this.totalAssignTime >= totalAssignTime;
    }

    public boolean isEqualsWeight(Weight weight) {
        return this.weight != weight;
    }
}
