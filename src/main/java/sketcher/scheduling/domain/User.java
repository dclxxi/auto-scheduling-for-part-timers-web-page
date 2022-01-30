package sketcher.scheduling.domain;

import lombok.Builder;
import lombok.Getter;
import org.apache.catalina.Manager;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
@Getter
<<<<<<< HEAD
public class User {

    @Id
    @Column(name = "user_id")
    @NotEmpty(message = "Null 일 수 없습니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]{3,12}$", message = "아이디를 3~12자로 입력해주세요. [특수문자 X]")
    private String id;

    @NotEmpty(message = "Null 일 수 없습니다.")
    private String auth_role;

    @NotEmpty(message = "Null 일 수 없습니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]{3,12}$", message = "비밀번호를 3~12자로 입력해주세요.")
    private String password;

    @NotEmpty(message = "Null 일 수 없습니다.")
    @Pattern(regexp = "[a-zA-Z0-9]*")
    private String user_name;

    @NotEmpty(message = "Null 일 수 없습니다.")
    private String user_tel;

    @NotEmpty(message = "Null 일 수 없습니다.")
    private LocalDateTime user_joinDate;

    private Double manager_score;

    @NotEmpty(message = "Null 일 수 없습니다.")
    private Character dropout_req_check;
=======
public class User extends UserTimeEntity{


    /**
     * NotEmpty 어노테이션은 우선 필요에 따라 설정하시거나 빼시면 될 것 같아요 !
     * NotEmpty 가 들어가면 테스트시에도 무조건 들어가야 하는 값(NULL = X)이에요!
     * Column 은 DB 에 들어가는 이름입니다. id 로 사용 -> DB 에는 user_id 로 저장.
     */
    @Id
    @Column(name = "user_id")
    @NotEmpty
//    @Pattern(regexp = "^[a-zA-Z0-9]{3,12}$", message = "아이디를 3~12자로 입력해주세요. [특수문자 X]")
    private String id;

    @NotEmpty
    @Column(name = "auth_role")
    private String authRole;

    @NotEmpty
    @Column(name = "user_pw")
//    @Pattern(regexp = "^[a-zA-Z0-9]{3,12}$", message = "비밀번호를 3~12자로 입력해주세요.")
    private String password;

    @NotEmpty
    @Column(name = "user_name")
//    @Pattern(regexp = "[a-zA-Z0-9]*")
    private String username;

    @NotEmpty
    @Column(name = "user_tel")
    private String userTel;

    /**
     * UserTimeEntity 로 수정  / 생성 시간 자동 생성 . 쿼리문 없어도 자동으로 DB 에 입력되는 순간을 기점으로 생성.
     */
//    @NotEmpty
//    private LocalDateTime user_joinDate;

    @Column(name = "manager_score")
    private Double managerScore;

    @Column(name = "dropout_req_check")
    private Character dropoutReqCheck;
>>>>>>> a1b345011df227365952b78d5097406501159c5b

    @OneToMany(mappedBy = "user")
    private List<ManagerHopeTime> managerHopeTimes = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<ManagerWorkingSchedule> managerWorkingSchedules = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<ManagerAssignSchedule> managerAssignSchedules = new ArrayList<>();

    protected User() {
    }

<<<<<<< HEAD
    @Builder
    public User(String id, String auth_role, String password, String user_name, String user_tel) {
        this.id = id;
        this.auth_role = auth_role;
        this.password = password;
        this.user_name = user_name;
        this.user_tel = user_tel;
=======
    /**
     * domain 은 DB 에 저장된 데이터를 꺼내오기 위한 클래스로 설정
     * getter 만 열어두었습니다!
     * Setter 는 dto 에 오픈해두었는데 DB 에 값 입력은 Builder 를 이용해봄이?..(생성자랑 비슷한 개념이에요!)
     */
    @Builder
    public User(String id, String authRole, String password, String username, String userTel, Double managerScore, Character dropoutReqCheck) {
        this.id = id;
        this.authRole = authRole;
        this.password = password;
        this.username = username;
        this.userTel = userTel;
        this.managerScore = managerScore;
        this.dropoutReqCheck = dropoutReqCheck;
>>>>>>> a1b345011df227365952b78d5097406501159c5b
    }
}
