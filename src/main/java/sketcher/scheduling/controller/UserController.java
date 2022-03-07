package sketcher.scheduling.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sketcher.scheduling.config.CustomAuthenticationProvider;
import sketcher.scheduling.domain.ManagerHopeTime;
import sketcher.scheduling.domain.User;
import sketcher.scheduling.dto.UserDto;
import sketcher.scheduling.dto.UserSearchCondition;
import sketcher.scheduling.repository.UserRepository;
import sketcher.scheduling.service.ManagerHopeTimeService;
import sketcher.scheduling.service.UserService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final ManagerHopeTimeService managerHopeTimeService;
//
//    @NonNull
//    private final BCryptPasswordEncoder passwordEncoder;


    @GetMapping(value = "/login")
    public String loginView(Model model, @RequestParam(value = "error", required = false) String error, @RequestParam(value = "exception", required = false) String exception) {
        model.addAttribute("error", error);
        model.addAttribute("exception", exception);
        return "user/login";
    }


    @GetMapping("/logout")
    public String logoutPage(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        return "redirect:/login";
    }

    @RequestMapping(value = "/step1", method = RequestMethod.GET)
    public String step1(HttpServletRequest request) {
        return "user/step1";
    }

    @RequestMapping(value = "/step2", method = RequestMethod.GET)
    public String step2(HttpServletRequest request) {
        return "user/step2";
    }

    @RequestMapping(value = "/step3", method = RequestMethod.GET)
    public String step3(HttpServletRequest request) {
        return "user/step3";
    }

    @RequestMapping(value = "/signup", method = RequestMethod.POST)
    public String signup(UserDto joinUser, RedirectAttributes redirectAttributes) {
        joinUser.setUser_joinDate(LocalDateTime.now());
        joinUser.setManagerScore(0.0);
        joinUser.setDropoutReqCheck('N');
        userService.saveUser(joinUser);
        redirectAttributes.addAttribute("userid", joinUser.getId());

        return "redirect:/check_hopetime";
    }

    @RequestMapping(value = "/user/idCheck", method = RequestMethod.GET)
    @ResponseBody
    public boolean idCheck(@RequestParam("userid") String user_id) {
        return userService.userIdCheck(user_id);
        //true : 아이디가 존재하지 않을 때
        //false : 아이디가 이미 존재할 때
    }

    @RequestMapping(value = "/all_manager_list", method = RequestMethod.GET)
    public String all_manager_list(Model model,
//            @RequestParam(required = false, defaultValue = "") UserSearchCondition condition,
                                   @RequestParam(required = false, defaultValue = "managerScore") String align, @RequestParam(required = false, defaultValue = "") String type, @RequestParam(required = false, defaultValue = "") String keyword, @PageableDefault Pageable pageable) {

        UserSearchCondition condition = new UserSearchCondition(align, type, keyword);
        Page<UserDto> users = userService.findAllManager(condition, pageable);
        model.addAttribute("condition", condition);
        model.addAttribute("users", users);
        return "manager/all_manager_list";
    }

    @RequestMapping(value = "/manager_detail/{userId}", method = RequestMethod.GET)
    public String manager_detail(Model model, @PathVariable(value = "userId") String id) {
        Optional<User> users = userService.findById(id);
        ArrayList<String> hope = userService.findHopeTimeById(id);

        model.addAttribute("users", users);
        model.addAttribute("hope", hope);

        return "manager/manager_detail";
    }

    @RequestMapping(value = "/work_manager_list", method = RequestMethod.GET)
    public String work_manager_list(Model model,
//            @RequestParam(required = false, defaultValue = "") UserSearchCondition condition,
                                    @RequestParam(required = false, defaultValue = "managerScore") String align, @RequestParam(required = false, defaultValue = "") String type, @RequestParam(required = false, defaultValue = "") String keyword, @PageableDefault Pageable pageable) {

        UserSearchCondition condition = new UserSearchCondition(align, type, keyword);
        Page<UserDto> users = userService.findWorkManager(condition, pageable);
        model.addAttribute("users", users);
        model.addAttribute("condition", condition);
        return "manager/work_manager_list";
    }

    @RequestMapping(value = "/admin_mypage", method = RequestMethod.GET)
    public String admin_mypage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        model.addAttribute("user", user);
        return "mypage/admin_mypage";
    }


    @RequestMapping(value = "/manager_mypage", method = {RequestMethod.GET, RequestMethod.POST})
    public String manager_mypage(Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        ArrayList<String> hope = userService.findHopeTimeById(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("hope", hope);
        return "mypage/manager_mypage";
    }

    @RequestMapping(value = "/dropoutReq", method = RequestMethod.POST)
    public String updateDropoutReq(@RequestParam String userid) {
        if (userid != null) {
            User user = userService.loadUserByUsername(userid);
            userService.updateDropoutReqCheck(user);
        }
        return "redirect:/manager_mypage";
    }


}
