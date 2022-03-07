package sketcher.scheduling.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sketcher.scheduling.domain.User;
import sketcher.scheduling.dto.UserDto;
import sketcher.scheduling.dto.UserSearchCondition;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;


public interface UserRepositoryCustom {
    Page<UserDto> findAllManager(UserSearchCondition condition, Pageable pageable);
    Page<UserDto> findWorkManager(UserSearchCondition condition, Pageable pageable);

//    ArrayList<String> findHopeTimeById(String id);
//    String updateUser(UserDto user);
}
