package sketcher.scheduling.controller;

import lombok.RequiredArgsConstructor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import sketcher.scheduling.algorithm.AutoScheduling;
import sketcher.scheduling.algorithm.ResultScheduling;
import sketcher.scheduling.domain.ManagerHopeTime;
import sketcher.scheduling.domain.User;
import sketcher.scheduling.dto.EstimatedNumOfCardsPerHourDto;
import sketcher.scheduling.dto.ManagerAssignScheduleDto;
import sketcher.scheduling.repository.EstimatedNumOfCardsPerHourRepository;
import sketcher.scheduling.repository.PercentageOfManagerWeightsRepository;
import sketcher.scheduling.repository.UserRepository;
import sketcher.scheduling.service.KakaoService;
import sketcher.scheduling.service.ManagerAssignScheduleService;
import sketcher.scheduling.service.ManagerHopeTimeService;
import sketcher.scheduling.service.UserService;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@org.springframework.web.bind.annotation.RestController
@RequiredArgsConstructor
public class RestController {
    private static final String USER_CODE = "userCode";
    private static final String TOTAL_ASSIGN_TIME = "userCurrentTime";
    private static final String HOPE_TIME = "hopetime";
    private static final String DATE = "date";
    private static final String DAY_OF_THE_WEEK = "day";
    public static final String SCHEDULE_START_TIME = "scheduleStartTime";
    public static final String SCHEDULE_RESULTS = "scheduleResults";
    public static final String USER_RESULTS = "userResults";
    private final UserRepository userRepository;
    private final UserService userService;
    private final ManagerAssignScheduleService assignScheduleService;
    private final KakaoService kakaoService;
    private final ManagerHopeTimeService hopeTimeService;
    private final EstimatedNumOfCardsPerHourRepository estimatedNumOfCardsPerHourRepository;
    private final PercentageOfManagerWeightsRepository percentageOfManagerWeightsRepository;

    @GetMapping(value = "/find_All_Manager")
    public List<User> findAllManager() {
        return userRepository.findAllManager();
    }

    @GetMapping(value = "/find_All_Manager_Hope_Time")
    public List<ManagerHopeTime> findAllManagerHopeTime() {
        return hopeTimeService.findAll();
    }

    @RequestMapping(value = "/create_assign_schedule", produces = "application/json;charset=UTF-8", method = RequestMethod.POST)
    public int createAssignSchedule(@RequestBody List<Map<String, Object>> param) throws ParseException, IOException {
        for (Map<String, Object> stringObjectMap : param) {
            String startDateString = (String) stringObjectMap.get("startTime"); //2022-07-24T22:00:00.000Z
            String endDateString = (String) stringObjectMap.get("endTime"); //2022-07-24T22:00:00.000Z
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.KOREA);

            LocalDateTime startDateUTC = LocalDateTime.parse(startDateString, dateTimeFormatter);
            LocalDateTime endDateUTC = LocalDateTime.parse(endDateString, dateTimeFormatter);

            LocalDateTime startDate = startDateUTC.plusHours(9);
            LocalDateTime endDate = endDateUTC.plusHours(9);

            ManagerAssignScheduleDto dto = ManagerAssignScheduleDto.builder()
                    .user(userService.findByCode((Integer) stringObjectMap.get("usercode")).get())
                    .scheduleDateTimeStart(startDate)
                    .scheduleDateTimeEnd(endDate)
                    .build();
            assignScheduleService.saveManagerAssignSchedule(dto);
        }

        sendKakaoMessage();

        return param.size();
    }

    @RequestMapping(value = "/current_status_info", produces = "application/json;charset=UTF-8", method = RequestMethod.POST)
    public JSONObject currentStatusInfo(@RequestBody List<Map<String, Object>> param) throws ParseException {
        List<Integer> userCodes = new ArrayList<>();
        List<Integer> totalAssignTimes = new ArrayList<>();
        List<List<Integer>> startTimesOfHopeTime = new ArrayList<>();

        Map<String, Object> dateAndDay = param.remove(0);
        String date = dateAndDay.get(DATE).toString();
        String dayOfTheWeek = dateAndDay.get(DAY_OF_THE_WEEK).toString();

        for (Map<String, Object> map : param) {
            userCodes.add((int) map.get(USER_CODE));
            totalAssignTimes.add((int) map.get(TOTAL_ASSIGN_TIME));
            startTimesOfHopeTime.add(getStartTimesOfHopeTime(map.get(HOPE_TIME).toString()));
        }

        AutoScheduling autoScheduling = new AutoScheduling(userService, estimatedNumOfCardsPerHourRepository, percentageOfManagerWeightsRepository);
        ArrayList<ResultScheduling> schedulings = autoScheduling.runAlgorithm(userCodes, totalAssignTimes, startTimesOfHopeTime);
        return schedulingResultsToJson(date, dayOfTheWeek, schedulings);
    }

    private List<Integer> getStartTimesOfHopeTime(String startTimeOfHopeTime) {
        String[] startTimes = toStringArray(startTimeOfHopeTime);
        return toIntegerList(startTimes);
    }

    private String[] toStringArray(String startTimeOfHopeTime) {
        return startTimeOfHopeTime
                .replace("[", "")
                .replace("]", "")
                .split(", ");
    }

    private List<Integer> toIntegerList(String[] startTimes) {
        List<Integer> startTimesOfHopeTime = new ArrayList<>();
        for (String startTime : startTimes) {
            startTimesOfHopeTime.add(Integer.parseInt(startTime));
        }

        return startTimesOfHopeTime;
    }

    private JSONObject schedulingResultsToJson(String date, String dayOfTheWeek, ArrayList<ResultScheduling> schedulings) {
        JSONObject schedulingJsonObj = new JSONObject();

        JSONArray selectedDate = new JSONArray();
        JSONArray scheduleJsonList = new JSONArray();
        JSONArray userJsonList = new JSONArray();

        HashMap<Integer, Integer> userList = new HashMap<>();

        JSONObject dateInfo = new JSONObject();
        dateInfo.put(DATE, date);
        dateInfo.put(DAY_OF_THE_WEEK, dayOfTheWeek);
        selectedDate.add(dateInfo);
        schedulingJsonObj.put(DATE, selectedDate);

        for (ResultScheduling scheduling : schedulings) {
            JSONObject scheduleItem = new JSONObject();
            scheduleItem.put(SCHEDULE_START_TIME, scheduling.startTime);
            scheduleItem.put(USER_CODE, scheduling.userCode);
            scheduleJsonList.add(scheduleItem);

            if (!userList.containsKey(scheduling.userCode)) {
                userList.put(scheduling.userCode, scheduling.currentTime);
            }
        }

        schedulingJsonObj.put(SCHEDULE_RESULTS, scheduleJsonList);

        for (Map.Entry<Integer, Integer> userStatus : userList.entrySet()) {
            JSONObject scheduleItem = new JSONObject();
            scheduleItem.put(USER_CODE, userStatus.getKey());
            scheduleItem.put(TOTAL_ASSIGN_TIME, userStatus.getValue());
            userJsonList.add(scheduleItem);
        }

        schedulingJsonObj.put(USER_RESULTS, userJsonList);
        return schedulingJsonObj;
    }

    public void sendKakaoMessage() throws IOException {
//            String refresh_Token = kakaoService.getRefreshToken(code);
        String refresh_Token = "uIYs7FKmV4Y-s5EAb8OjEpHvvLtZN3zDoD6p2i_HCilwUAAAAYIu4OjU";
        String access_Token = kakaoService.refreshAccessToken(refresh_Token);
        HashMap<String, Object> userInfo = kakaoService.getUserInfo(access_Token);
        boolean isSendMessage = kakaoService.isSendMessage(access_Token);
        HashMap<String, Object> friendsId = kakaoService.getFriendsList(access_Token);
        boolean isSendMessageToFriends = kakaoService.isSendMessageToFriends(access_Token, friendsId);
        // 친구에게 메시지 보내기는 월 전송 제한이 있음 -> 주석 처리

//        session.setAttribute("refresh_Token", refresh_Token);
//        session.setAttribute("access_Token", access_Token);

    }

    @RequestMapping(value = "/update_est_cards", produces = "application/json;charset=UTF-8", method = RequestMethod.POST)
    public int updateEstCards(@RequestBody List<Map<String, Object>> param) throws ParseException, IOException {
        for (Map<String, Object> stringObjectMap : param) {
            Integer time = Integer.parseInt(stringObjectMap.get("time").toString());
            Integer value = Integer.parseInt(stringObjectMap.get("value").toString());
            EstimatedNumOfCardsPerHourDto dto = EstimatedNumOfCardsPerHourDto.builder()
                    .time(time)
                    .numOfCards(value)
                    .build();

            estimatedNumOfCardsPerHourRepository.save(dto.toEntity());
        }
        return param.size();
    }
}