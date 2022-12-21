package sketcher.scheduling.algorithm;

import sketcher.scheduling.domain.EstimatedNumOfCardsPerHour;

import java.util.List;

import static sketcher.scheduling.algorithm.Weight.LEVEL1;
import static sketcher.scheduling.algorithm.Weight.LEVEL2;
import static sketcher.scheduling.algorithm.Weight.LEVEL3;

public class ScheduleNodeMaker {
    private static final double MANAGER_DONE_REQUEST_AVG_PER_HOUR = 50.0;

    private int numOfCards;
    private int numOfManagers;
    private int numOfFixedManagers;
    private Weight weight;

    public void setUp(EstimatedNumOfCardsPerHour card, double totalCardValueAvg, double fixedM3Ratio) {
        numOfCards = card.getNumOfCards();
        numOfManagers = getNumberOfManagers();
        weight = getWeight(totalCardValueAvg);
        numOfFixedManagers = getNumberOfFixedManagers(fixedM3Ratio);
    }

    private int getNumberOfManagers() {
        int numOfManagers = (int) Math.ceil(numOfCards / MANAGER_DONE_REQUEST_AVG_PER_HOUR);

        if (hasNumOfManagers(numOfManagers)) {
            return numOfManagers;
        }

        return 1;
    }

    private boolean hasNumOfManagers(int numOfManagers) {
        return numOfManagers != 0 || numOfCards == 0;
    }

    private Weight getWeight(double totalCardValueAvg) {
        if (isLowCards(totalCardValueAvg)) {
            return LEVEL1;
        }

        if (isHighCards(totalCardValueAvg)) {
            return LEVEL3;
        }

        return LEVEL2;
    }

    private boolean isLowCards(double totalCardValueAvg) {
        return numOfCards < getLowBoundary(totalCardValueAvg);
    }


    private static double getLowBoundary(double totalCardValueAvg) {
        return totalCardValueAvg / 2;
    }

    private boolean isHighCards(double totalCardValueAvg) {
        return numOfCards >= getHighBoundary(totalCardValueAvg);
    }

    private static double getHighBoundary(double totalCardValueAvg) {
        return totalCardValueAvg * 2;
    }

    private int getNumberOfFixedManagers(double fixedM3Ratio) {
        if (weight == LEVEL3) {
            return (int) Math.round(numOfManagers * fixedM3Ratio);
        }

        return 0;
    }

    public void addSchedule(List<Schedule> schedulesByHopeTime, EstimatedNumOfCardsPerHour card) {
        int time = card.getTime();

        for (int id = 0; id < numOfManagers; id++) {
            Schedule schedule = new Schedule(id, time, weight, getM3(id));
            schedulesByHopeTime.add(schedule);
        }
    }

    private boolean getM3(int count) {
        return count < numOfFixedManagers;
    }
}

