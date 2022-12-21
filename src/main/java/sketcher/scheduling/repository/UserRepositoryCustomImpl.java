package sketcher.scheduling.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sketcher.scheduling.domain.User;
import sketcher.scheduling.dto.UserDto;
import sketcher.scheduling.dto.UserSearchCondition;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static sketcher.scheduling.domain.QManagerAssignSchedule.managerAssignSchedule;
import static sketcher.scheduling.domain.QManagerHopeTime.managerHopeTime;
import static sketcher.scheduling.domain.QUser.user;

@Repository
@Transactional
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final UserRepository userRepository;
    private final JPAQueryFactory queryFactory;
//    private final AuthenticationManager authenticationManager;


    @Override
    public Page<UserDto> findAllManager(UserSearchCondition condition, Pageable pageable) {
        pageable = pageableSetting(condition, pageable);

        // 데이터 조회 쿼리와 전체 카운트 쿼리 분리 (최적화)
        // content 쿼리는 복잡하지만, count쿼리는 깔끔하게 나올 수 있을 때 활용
        List<UserDto> content = queryFactory
                .select(Projections.bean(UserDto.class, // 조회할 데이터만 가져옴
                        user.id,
                        user.authRole,
                        user.username,
                        user.userTel,
                        user.user_joinDate,
                        user.managerScore
                ))
                .from(user)
                .where(
                        managerList(condition.getType(), condition.getKeyword())
                )
                .orderBy(userSort(condition.getAlign(), pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch(); // count 쿼리 제외하고 content 쿼리만 날림

        long total = queryFactory
                .select(Projections.bean(UserDto.class,
                        user.id,
                        user.username,
                        user.userTel,
                        user.user_joinDate,
                        user.managerScore
                ))
                .from(user)
                .where(
                        managerList(condition.getType(), condition.getKeyword())
                )
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<UserDto> findWorkManager(UserSearchCondition condition, Pageable pageable) {
        pageable = pageableSetting(condition, pageable);

        List<UserDto> content = queryFactory
                .select(Projections.bean(UserDto.class,
                        user.id,
                        user.authRole,
                        user.username,
                        user.userTel,
                        user.user_joinDate,
                        user.managerScore
                ))
                .from(managerAssignSchedule)
                .join(managerAssignSchedule.user, user)
                .where(
                        managerList(condition.getType(), condition.getKeyword()),
                        workTime()
                )
                .groupBy(user.code)
                .orderBy(userSort(condition.getAlign(), pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch(); // count쿼리는 제외하고 content 쿼리만 날린다.

        long total = queryFactory
                .select(Projections.fields(UserDto.class,
                        user.id,
                        user.authRole,
                        user.username,
                        user.userTel,
                        user.user_joinDate,
                        user.managerScore
                ))
                .from(managerAssignSchedule)
                .join(managerAssignSchedule.user, user)
                .where(
                        managerList(condition.getType(), condition.getKeyword()),
                        workTime()
                )
                .groupBy(user.code)

                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<UserDto> findLeaveManager(UserSearchCondition condition, Pageable pageable) {
        pageable = pageableSetting(condition, pageable);

        // 데이터 조회 쿼리와 전체 카운트 쿼리 분리 (최적화)
        // content 쿼리는 복잡하지만, count쿼리는 깔끔하게 나올 수 있을 때 활용
        List<UserDto> content = queryFactory
                .select(Projections.bean(UserDto.class, // 조회할 데이터만 가져옴
                        user.id,
                        user.authRole,
                        user.username,
                        user.userTel,
                        user.user_joinDate,
                        user.managerScore
                ))
                .from(user)
                .where(
                        leaveManagerList(condition.getType(), condition.getKeyword())
                )
                .orderBy(userSort(condition.getAlign(), pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch(); // count 쿼리 제외하고 content 쿼리만 날림

        long total = queryFactory
                .select(Projections.bean(UserDto.class,
                        user.id,
                        user.username,
                        user.userTel,
                        user.user_joinDate,
                        user.managerScore
                ))
                .from(user)
                .where(
                        leaveManagerList(condition.getType(), condition.getKeyword())
                )
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<UserDto> findVacationManagers(UserSearchCondition condition, Pageable pageable) {
        pageable = pageableSetting(condition, pageable);

        // 데이터 조회 쿼리와 전체 카운트 쿼리 분리 (최적화)
        // content 쿼리는 복잡하지만, count쿼리는 깔끔하게 나올 수 있을 때 활용
        List<UserDto> content = queryFactory
                .select(Projections.bean(UserDto.class, // 조회할 데이터만 가져옴
                        user.id,
                        user.authRole,
                        user.username,
                        user.userTel,
                        user.user_joinDate,
                        user.managerScore,
                        user.vacationReqCheck
                ))
                .from(user)
                .where(
                        vacationManagerList(condition.getType(), condition.getKeyword())
                )
                .orderBy(userSort(condition.getAlign(), pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch(); // count 쿼리 제외하고 content 쿼리만 날림

        long total = queryFactory
                .select(Projections.bean(UserDto.class,
                        user.id,
                        user.username,
                        user.userTel,
                        user.user_joinDate,
                        user.managerScore,
                        user.vacationReqCheck
                ))
                .from(user)
                .where(
                        vacationManagerList(condition.getType(), condition.getKeyword())
                )
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public long countByTodayWorkManager() {
        return queryFactory
                .select(user.code)
                .from(managerAssignSchedule)
                .join(managerAssignSchedule.user, user)
                .where(workTime())
                .groupBy(user.code)
                .fetchCount();
    }

    @Override
    public List<Integer> findJoinDateByHopeTime(int startTime) {
        return queryFactory
                .select(user.code)
                .from(user)
                .join(user.managerHopeTimeList, managerHopeTime)
                .where(startTimeEq(startTime),
                        authRoleEq("MANAGER"))
                .orderBy(user.user_joinDate.asc())
                .fetch();
    }

    private Pageable pageableSetting(UserSearchCondition condition, Pageable pageable) {
        String align = condition.getAlign();
        Sort sort = Sort.by(align).ascending();

        switch (align) {
            case "username":
                sort = Sort.by(align).ascending();
                break;

            case "joindate_desc":
                sort = Sort.by("user_joindate").descending();
                break;

            case "joindate_asc":
                sort = Sort.by("user_joindate").ascending();
                break;
        }

        int page = (pageable.getPageNumber() == 0) ? 0 : (pageable.getPageNumber() - 1); // page는 index 처럼 0부터 시작 -> 페이지에서 -1 처리

        return PageRequest.of(page, 10, sort);
    }

    private BooleanExpression startTimeEq(Integer startTime) {
        return hasText(String.valueOf(startTime)) ? managerHopeTime.start_time.eq(startTime) : null;
    }

    private BooleanExpression authRoleEq(String authRole) {
        return hasText(authRole) ? user.authRole.eq(authRole) : null;
    }

    private BooleanExpression vacationEq(Character vacation_req_check) {
        return hasText(String.valueOf(vacation_req_check)) ? user.vacationReqCheck.eq(vacation_req_check) : null;
    }

    private BooleanExpression keywordContains(String type, String keyword) {
        switch (type) {
            case "username":
                return hasText(keyword) ? user.username.contains(keyword) : null;

            case "id":
                return hasText(keyword) ? user.id.contains(keyword) : null;
        }
        return null;
    }

    private BooleanBuilder managerList(String type, String keyword) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(authRoleEq("MANAGER"));
        builder.and(keywordContains(type, keyword));

        return builder;
    }

    private BooleanBuilder leaveManagerList(String type, String keyword) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(authRoleEq("LEAVE"));
        builder.and(keywordContains(type, keyword));

        return builder;
    }

    private BooleanBuilder vacationManagerList(String type, String keyword) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(vacationEq('Y'));
        builder.and(keywordContains(type, keyword));

        return builder;
    }

    private OrderSpecifier<?> userSort(String list_align, Pageable page) {
        //서비스에서 보내준 Pageable 객체에 정렬조건 null 값 체크
        if (!page.getSort().isEmpty()) { //정렬값이 들어 있으면 값을 가져온다
            return listSort(list_align);
        }
        return null;
    }

    private BooleanExpression workTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime date = LocalDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour(), now.getMinute());

        return managerAssignSchedule.scheduleDateTimeStart.before(date)
                .and(managerAssignSchedule.scheduleDateTimeEnd.after(date));
    }

    @Override
    public List<User> withdrawalManagers(UserSearchCondition condition) {
        List<User> content = queryFactory
                .selectFrom(user)
                .where(user.dropoutReqCheck.eq('Y'),
                        managerList(condition.getType(), condition.getKeyword())
                )
                .orderBy(listSort(condition.getAlign()))
                .fetch(); // count 쿼리 제외하고 content 쿼리만 날림

        return content;
    }


    private OrderSpecifier<?> listSort(String list_align) {
        switch (list_align) {
            case "id":
                return new OrderSpecifier(Order.ASC, user.id);

            case "username":
                return new OrderSpecifier(Order.ASC, user.username);

            case "joindate_asc":
                return new OrderSpecifier(Order.ASC, user.user_joinDate);

            case "joindate_desc":
                return new OrderSpecifier(Order.DESC, user.user_joinDate);
        }
        return null;
    }
}