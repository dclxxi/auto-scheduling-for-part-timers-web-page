package sketcher.scheduling.algorithm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sketcher.scheduling.domain.EstimatedNumOfCardsPerHour;
import sketcher.scheduling.domain.PercentageOfManagerWeights;
import sketcher.scheduling.object.HopeTime;
import sketcher.scheduling.repository.EstimatedNumOfCardsPerHourRepository;
import sketcher.scheduling.repository.PercentageOfManagerWeightsRepository;
import sketcher.scheduling.service.UserService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class AutoScheduling {
    private static final int LEVEL1 = 1;
    private static final int LEVEL2 = 2;
    private static final int LEVEL3 = 3;
    private static final int HIGH_INDEX = 0;
    private static final int MIDDLE_INDEX = 1;
    private static final int DAY_ASSIGN_TIME = 3;
    private static final int TOTAL_ASSIGN_TIME = 10;
    private static final double MANAGER_DONE_REQUEST_AVG_PER_HOUR = 50.0;

    private final UserService userService;
    private final EstimatedNumOfCardsPerHourRepository estimatedNumOfCardsPerHourRepository;
    private final PercentageOfManagerWeightsRepository percentageOfManagerWeightsRepository;

    private List<Manager> managers = new ArrayList<>();
    private Map<HopeTime, List<Schedule>> schedules = new EnumMap<>(HopeTime.class);
    private double fixedM3Ratio;
    private double totalCardValueAvg;

    public ArrayList<ResultScheduling> runAlgorithm(List<Integer> userCodes, List<Integer> userCurrentTimes, List<List<Integer>> startTimesOfHopeTime) {
        totalCardValueAvg = estimatedNumOfCardsPerHourRepository.totalCardValueAvg();

        Map<HopeTime, List<EstimatedNumOfCardsPerHour>> cardsByHopeTime = getCardsByHopeTime();
        Map<Integer, Manager> managerNodes = makeManagerNodes(userCodes, userCurrentTimes, startTimesOfHopeTime);

        scheduleAssignByHopeTime(cardsByHopeTime, managerNodes);
        return getResultSchedulings();
    }

    private Map<HopeTime, List<EstimatedNumOfCardsPerHour>> getCardsByHopeTime() {
        List<EstimatedNumOfCardsPerHour> cards = estimatedNumOfCardsPerHourRepository.findAll();
        Map<HopeTime, List<EstimatedNumOfCardsPerHour>> cardsByHopeTime = new EnumMap<>(HopeTime.class);

        for (EstimatedNumOfCardsPerHour card : cards) {
            putCardsByHopeTime(cardsByHopeTime, card);
        }

        return Map.copyOf(cardsByHopeTime);
    }

    private void putCardsByHopeTime(Map<HopeTime, List<EstimatedNumOfCardsPerHour>> cardsByHopeTime, EstimatedNumOfCardsPerHour card) {
        HopeTime hopeTime = HopeTime.getByTime(card.getTime());

        List<EstimatedNumOfCardsPerHour> cards = cardsByHopeTime.getOrDefault(hopeTime, new ArrayList<>());
        cards.add(card);

        cardsByHopeTime.put(hopeTime, cards);
    }


    private Map<Integer, Manager> makeManagerNodes(List<Integer> userCodes, List<Integer> userCurrentTimes, List<List<Integer>> startTimesOfHopeTime) {
        Map<Integer, Manager> managerNode = new LinkedHashMap<>();

        for (int index = 0; index < userCodes.size(); index++) {
            List<HopeTime> hopeTimes = getHopeTimes(startTimesOfHopeTime.get(index));
            Manager manager = new Manager(userCodes.get(index), userCurrentTimes.get(index), hopeTimes);
            managerNode.put(userCodes.get(index), manager);
        }

        return managerNode;
    }

    private List<HopeTime> getHopeTimes(List<Integer> startTimesOfHopeTime) {
        List<HopeTime> hopeTimes = new ArrayList<>();
        for (int startTime : startTimesOfHopeTime) {
            HopeTime hopeTime = HopeTime.getByTime(startTime);
            hopeTimes.add(hopeTime);
        }

        return List.copyOf(hopeTimes);
    }

    private void scheduleAssignByHopeTime(Map<HopeTime, List<EstimatedNumOfCardsPerHour>> cardsByHopeTime, Map<Integer, Manager> managerNodes) {
        for (HopeTime hopeTime : HopeTime.values()) {
            setManagerWeight(hopeTime, managerNodes);
            makeScheduleNodes(hopeTime, cardsByHopeTime.get(hopeTime));
            bipartiteMatching(schedules.get(hopeTime));
        }
    }

    private void bipartiteMatching(List<Schedule> schedules) {
        for (Schedule schedule : schedules) {
            dfs(schedule);
        }
    }

    private ArrayList<ResultScheduling> getResultSchedulings() {
        ArrayList<ResultScheduling> schedulingResults = new ArrayList<>();
        for (HopeTime hopeTime : HopeTime.values()) {
            createResultSchedulings(schedulingResults, schedules.get(hopeTime));
        }
        return schedulingResults;
    }

    private void createResultSchedulings(ArrayList<ResultScheduling> schedulingResults, List<Schedule> schedulesByHopeTime) {
        for (Schedule schedule : schedulesByHopeTime) {
            if (schedule.getManager() != null) {
                schedulingResults.add(new ResultScheduling(schedule.getTime(), schedule.getManager().getCode(), schedule.getManager().getTotalAssignTime()));
            }
        }
    }

    private void setManagerWeight(HopeTime hopeTime, Map<Integer, Manager> managerNodes) {
        List<Integer> codesOrderByJoinDate = userService.findJoinDateByHopeTime(hopeTime.getStartTime());
        List<Integer> percentage = getPercentageOfManagerWeight();

        fixedM3Ratio = getM3Ratio(percentage.get(HIGH_INDEX));

        managers.clear();
        List<List<Integer>> managerRatios = getManagerRatios(percentage, codesOrderByJoinDate.size());
        for (List<Integer> managerRatio : managerRatios) {
            setWeightByJoinDate(codesOrderByJoinDate, managerRatio, managerNodes);
        }
    }

    private List<Integer> getPercentageOfManagerWeight() {
        PercentageOfManagerWeights percentageOfManagerWeights = percentageOfManagerWeightsRepository.findAll().get(0);

        int high = percentageOfManagerWeights.getHigh();
        int middle = percentageOfManagerWeights.getMiddle();

        return List.of(high, middle);
    }

    private List<List<Integer>> getManagerRatios(List<Integer> percentage, int totalCount) {
        List<List<Integer>> managerRatio = new ArrayList<>();

        int highManagerCount = getManagerRatio(percentage.get(HIGH_INDEX));
        int middleManagerCount = getManagerRatio(percentage.get(MIDDLE_INDEX));

        managerRatio.add(List.of(0, highManagerCount));
        managerRatio.add(List.of(highManagerCount, middleManagerCount));
        managerRatio.add(List.of(middleManagerCount, totalCount));

        return managerRatio;
    }

    private int getManagerRatio(int level) {
        return (int) Math.round(managers.size() * level * 0.01);
    }

    private double getM3Ratio(int high) {
        return high * 0.02;
    }

    private void setWeightByJoinDate(List<Integer> codes, List<Integer> managerRatio, Map<Integer, Manager> managerNodes) {
        int startIndex = managerRatio.get(0);
        int endIndex = managerRatio.get(1);

        for (int i = startIndex; i < endIndex; i++) {
            int code = codes.get(i);
            Manager manager = managerNodes.get(code);
            manager.setWeight(LEVEL3);
            managers.add(manager);
        }
    }

    private void makeScheduleNodes(HopeTime hopeTime, List<EstimatedNumOfCardsPerHour> cards) {
        List<Schedule> schedulesByHopeTime = schedules.getOrDefault(hopeTime, new ArrayList<>());

        for (EstimatedNumOfCardsPerHour card : cards) {
            int numOfCards = card.getNumOfCards();
            int numberOfManagers = getNumberOfManagers(numOfCards);

            if (numOfCards != 0 && numberOfManagers == 0) {
                numberOfManagers = 1;
            }

            int weight = getWeight(totalCardValueAvg, numOfCards);
            int numOfFixedManager = getNumberOfFixedManagers(numberOfManagers, weight);

            int scheduleNodeId = 0;
            int countOfFixedManager = 0;
            for (int i = 0; i < numberOfManagers; i++) {
                boolean M3 = false;

                if (countOfFixedManager < numOfFixedManager) {
                    M3 = true;
                    countOfFixedManager++;
                }

                schedulesByHopeTime.add(new Schedule(++scheduleNodeId, card.getTime(), weight, M3));
            }

            schedules.put(hopeTime, schedulesByHopeTime);
        }
    }

    private int getNumberOfManagers(int numOfCards) {
        return (int) Math.ceil(numOfCards / MANAGER_DONE_REQUEST_AVG_PER_HOUR);
    }

    private int getWeight(double totalCardValueAvg, int numOfCards) {
        if (numOfCards < totalCardValueAvg / 2) {
            return LEVEL1;
        }

        if (numOfCards < totalCardValueAvg * 2) {
            return LEVEL2;
        }

        return LEVEL3;
    }

    private int getNumberOfFixedManagers(int weight, int numberOfManagers) {
        if (weight == LEVEL3) {
            return (int) Math.round(numberOfManagers * fixedM3Ratio);
        }

        return 0;
    }

    private List<Manager> sortToPriority(List<Manager> managers, int scheduleWeight) {
        Comparator<Manager> comparingTotalAssignTime = Comparator.comparing(Manager::getTotalAssignTime);
        Comparator<Manager> comparingWeight = Comparator.comparing(Manager::getWeight);
        Comparator<Manager> comparingHopeTimeCount = Comparator.comparing(Manager::getHopeTimeCount);

        switch (scheduleWeight) {
            case LEVEL1:
                managers.sort(comparingHopeTimeCount
                        .thenComparing(comparingTotalAssignTime)
                        .thenComparing(comparingWeight));
                break;

            case LEVEL2:
                managers.sort(comparingTotalAssignTime
                        .thenComparing(comparingHopeTimeCount)
                        .thenComparing(comparingWeight.reversed()));
                break;

            case LEVEL3:
                managers.sort(comparingWeight.reversed()
                        .thenComparing(comparingHopeTimeCount)
                        .thenComparing(comparingTotalAssignTime));
                break;
        }

        return managers;
    }

    private boolean dfs(Schedule scheduleNode) {
        managers = sortToPriority(managers, scheduleNode.getWeight());

        for (Manager manager : managers) {
            Schedule assignScheduleNode = manager.findScheduleByTime(scheduleNode.getTime());

            if (!canAssignSchedule(scheduleNode, manager, assignScheduleNode)) {
                continue;
            }

            if (assignScheduleNode == null || dfs(assignScheduleNode)) {
                manager.updateAssignScheduleList(assignScheduleNode, scheduleNode);
                scheduleNode.setManager(manager);
                return true;
            }
        }

        return false;
    }

    private boolean canAssignSchedule(Schedule scheduleNode, Manager manager, Schedule assignScheduleNode) {
        if (isAssignScheduleNode(assignScheduleNode)) {
            return false;
        }

        if (isExceedDayAssignTime(manager)) {
            return false;
        }

        if (isExceedToTalAssignTime(manager)) {
            return false;
        }

        if (isMatchWeight(scheduleNode, manager)) {
            return false;
        }

        return true;
    }

    private boolean isAssignScheduleNode(Schedule assignScheduleNode) {
        return assignScheduleNode != null;
    }

    private boolean isExceedDayAssignTime(Manager manager) {
        return manager.isExceedDayAssignTime(DAY_ASSIGN_TIME);
    }

    private boolean isExceedToTalAssignTime(Manager manager) {
        return manager.isExceedToTalAssignTime(TOTAL_ASSIGN_TIME);
    }

    private boolean isMatchWeight(Schedule scheduleNode, Manager manager) {
        return scheduleNode.isM3() && !manager.isEqualsWeight(LEVEL3);
    }
}

