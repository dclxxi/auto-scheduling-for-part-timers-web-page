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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static sketcher.scheduling.algorithm.Weight.LEVEL1;
import static sketcher.scheduling.algorithm.Weight.LEVEL2;
import static sketcher.scheduling.algorithm.Weight.LEVEL3;


@Component
@RequiredArgsConstructor
public class AutoScheduling {
    private static final int DAY_ASSIGN_TIME = 3;
    private static final int TOTAL_ASSIGN_TIME = 10;
    private static final double MANAGER_DONE_REQUEST_AVG_PER_HOUR = 50.0;

    private final UserService userService;
    private final EstimatedNumOfCardsPerHourRepository estimatedNumOfCardsPerHourRepository;
    private final PercentageOfManagerWeightsRepository percentageOfManagerWeightsRepository;

    private List<Manager> managers = new ArrayList<>();
    private Map<HopeTime, List<Schedule>> schedules = new EnumMap<>(HopeTime.class);
    private double fixedM3Ratio;
    private Map<Integer, Manager> managerNodes;

    public ArrayList<ResultScheduling> runAlgorithm(List<Integer> userCodes, List<Integer> totalAssignTimes, List<List<Integer>> startTimesOfHopeTime) {
        Map<HopeTime, List<EstimatedNumOfCardsPerHour>> cardsByHopeTime = getCardsByHopeTime();
        managerNodes = makeManagerNodes(userCodes, totalAssignTimes, startTimesOfHopeTime);

        scheduleAssignByHopeTime(cardsByHopeTime);
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


    private Map<Integer, Manager> makeManagerNodes(List<Integer> userCodes, List<Integer> totalAssignTimes, List<List<Integer>> startTimesOfHopeTime) {
        Map<Integer, Manager> managerNode = new LinkedHashMap<>();

        for (int index = 0; index < userCodes.size(); index++) {
            List<HopeTime> hopeTimes = getHopeTimes(startTimesOfHopeTime.get(index));
            Manager manager = new Manager(userCodes.get(index), totalAssignTimes.get(index), hopeTimes);
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

    private void scheduleAssignByHopeTime(Map<HopeTime, List<EstimatedNumOfCardsPerHour>> cardsByHopeTime) {
        for (HopeTime hopeTime : HopeTime.values()) {
            setManagerWeight(hopeTime);
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

    private void setManagerWeight(HopeTime hopeTime) {
        List<Integer> codesOrderByJoinDate = userService.findJoinDateByHopeTime(hopeTime.getStartTime());
        Map<Weight, Integer> percentage = getPercentageOfManagerWeight();
        fixedM3Ratio = getM3Ratio(percentage.get(LEVEL3));

        setManagerWeight(codesOrderByJoinDate, percentage);
    }

    private Map<Weight, Integer> getPercentageOfManagerWeight() {
        PercentageOfManagerWeights percentageOfManagerWeights = percentageOfManagerWeightsRepository.findAll().get(0);

        Map<Weight, Integer> percentage = new HashMap<>();
        percentage.put(LEVEL3, percentageOfManagerWeights.getHigh());
        percentage.put(LEVEL2, percentageOfManagerWeights.getMiddle());

        return Map.copyOf(percentage);
    }

    private double getM3Ratio(int high) {
        return high * 0.02;
    }

    private void setManagerWeight(List<Integer> codesOrderByJoinDate, Map<Weight, Integer> percentage) {
        managers.clear();

        int totalManagerCount = codesOrderByJoinDate.size();
        Map<Weight, Integer> managerCounts = getManagerCounts(percentage, totalManagerCount);

        for (Weight weight : Weight.values()) {
            List<Integer> codes = getCodes(codesOrderByJoinDate, managerCounts.get(weight));
            setManagerWeight(codes, weight);
        }
    }

    private Map<Weight, Integer> getManagerCounts(Map<Weight, Integer> percentage, int totalCount) {
        Map<Weight, Integer> managerCounts = new HashMap<>();

        int currentCount = 0;
        for (Weight weight : percentage.keySet()) {
            int managerCount = getManagerCount(percentage.get(weight), totalCount);
            managerCounts.put(weight, managerCount);
            currentCount += managerCount;
        }
        managerCounts.put(Weight.getLast(), totalCount - currentCount);

        return Map.copyOf(managerCounts);
    }

    private int getManagerCount(int percentage, int totalManagerCount) {
        return (int) Math.round(totalManagerCount * percentage * 0.01);
    }

    private List<Integer> getCodes(List<Integer> codesOrderByJoinDate, int managerCount) {
        List<Integer> codesByWeight = codesOrderByJoinDate.subList(0, managerCount);
        List<Integer> codes = new ArrayList<>(codesByWeight);
        codesByWeight.clear();
        return codes;
    }

    private void setManagerWeight(List<Integer> codes, Weight weight) {
        for (int code : codes) {
            Manager manager = managerNodes.get(code);
            manager.setWeight(weight);
            managers.add(manager);
        }
    }

    private void makeScheduleNodes(HopeTime hopeTime, List<EstimatedNumOfCardsPerHour> cards) {
        double totalCardValueAvg = estimatedNumOfCardsPerHourRepository.totalCardValueAvg();
        List<Schedule> schedulesByHopeTime = schedules.getOrDefault(hopeTime, new ArrayList<>());

        for (EstimatedNumOfCardsPerHour card : cards) {
            int numOfCards = card.getNumOfCards();
            int numberOfManagers = getNumberOfManagers(numOfCards);

            if (numOfCards != 0 && numberOfManagers == 0) {
                numberOfManagers = 1;
            }

            Weight weight = getWeight(totalCardValueAvg, numOfCards);
            int numOfFixedManager = getNumberOfFixedManagers(weight, numberOfManagers);

            int scheduleNodeId = 0;
            int countOfFixedManager = 0;
            for (int i = 0; i < numberOfManagers; i++) {
                boolean M3 = false;

                if (countOfFixedManager < numOfFixedManager) {
                    M3 = true;
                    countOfFixedManager++;
                }

                schedulesByHopeTime.add(new Schedule(++scheduleNodeId, card.getTime(), M3, weight));
            }
        }

        schedules.put(hopeTime, schedulesByHopeTime);
    }

    private int getNumberOfManagers(int numOfCards) {
        return (int) Math.ceil(numOfCards / MANAGER_DONE_REQUEST_AVG_PER_HOUR);
    }

    private Weight getWeight(double totalCardValueAvg, int numOfCards) {
        if (numOfCards < totalCardValueAvg / 2) {
            return LEVEL1;
        }

        if (numOfCards < totalCardValueAvg * 2) {
            return LEVEL2;
        }

        return LEVEL3;
    }

    private int getNumberOfFixedManagers(Weight weight, int numberOfManagers) {
        if (weight == LEVEL3) {
            return (int) Math.round(numberOfManagers * fixedM3Ratio);
        }

        return 0;
    }

    private List<Manager> sortToPriority(List<Manager> managers, Weight scheduleWeight) {
        Comparator<Manager> comparingTotalAssignTime = Comparator.comparing(Manager::getTotalAssignTime);
        Comparator<Manager> comparingWeight = Comparator.comparing(Manager::getWeight);
        Comparator<Manager> comparingHopeTimeCount = Comparator.comparing(Manager::getHopeTimeCount);

        if (LEVEL1 == scheduleWeight) {
            return managers.stream().sorted(comparingHopeTimeCount
                    .thenComparing(comparingTotalAssignTime)
                    .thenComparing(comparingWeight))
                    .collect(Collectors.toList());
        }

        if (LEVEL2 == scheduleWeight) {
            return managers.stream().sorted(comparingTotalAssignTime
                    .thenComparing(comparingHopeTimeCount)
                    .thenComparing(comparingWeight.reversed()))
                    .collect(Collectors.toList());
        }

        if (LEVEL3 == scheduleWeight) {
            return managers.stream().sorted(comparingWeight.reversed()
                    .thenComparing(comparingHopeTimeCount)
                    .thenComparing(comparingTotalAssignTime))
                    .collect(Collectors.toList());
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

