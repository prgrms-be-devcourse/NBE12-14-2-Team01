
package com.merge.backend.domain.workplace.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.user.service.UserService;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.exception.WorkplaceErrorCode;
import com.merge.backend.domain.workplace.service.WorkplaceService;
import com.merge.backend.global.exception.BusinessException;
import com.merge.backend.global.exception.GlobalExceptionHandler;
import com.merge.backend.global.rq.Rq;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ContextConfiguration;

@WebMvcTest(WorkplaceController.class)
@ContextConfiguration(classes = {
    WorkplaceController.class,
    GlobalExceptionHandler.class
})
@AutoConfigureMockMvc(addFilters = false)
class WorkplaceControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private WorkplaceService workplaceService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private Rq rq;

    @Test
    @DisplayName("근무지 생성 시 201과 관리자 정보를 반환한다")
    void createWorkplace() throws Exception {
        User user = mock(User.class);
        Workplace workplace = mock(Workplace.class);
        WorkplaceMember member = mock(WorkplaceMember.class);

        when(rq.getActorId()).thenReturn(1L);
        when(userService.getById(1L)).thenReturn(user);

        when(workplaceService.createWorkplace("테스트 매장", user))
            .thenReturn(member);

        when(member.getWorkplace()).thenReturn(workplace);
        when(member.getRole()).thenReturn(WorkplaceRole.MANAGER);
        when(workplace.getId()).thenReturn(10L);
        when(workplace.getName()).thenReturn("테스트 매장");

        mvc.perform(
                post("/workplaces")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "테스트 매장"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("201"))
            .andExpect(jsonPath("$.data.workplaceId").value(10))
            .andExpect(jsonPath("$.data.name").value("테스트 매장"))
            .andExpect(jsonPath("$.data.role").value("MANAGER"));
    }

    @Test
    @DisplayName("근무지 이름이 공백이면 400을 반환한다")
    void rejectBlankWorkplaceName() throws Exception {
        mvc.perform(
                post("/workplaces")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "   "
                        }
                        """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COM-001"));

        verifyNoInteractions(userService, workplaceService);
    }

    @Test
    @DisplayName("초대코드로 참여하면 201과 직원 정보를 반환한다")
    void joinWorkplace() throws Exception {
        User user = mock(User.class);
        Workplace workplace = mock(Workplace.class);
        WorkplaceMember member = mock(WorkplaceMember.class);

        when(rq.getActorId()).thenReturn(1L);
        when(userService.getById(1L)).thenReturn(user);

        when(workplaceService.joinWorkplace("ABCD1234", user))
            .thenReturn(member);

        when(member.getWorkplace()).thenReturn(workplace);
        when(member.getRole()).thenReturn(WorkplaceRole.EMPLOYEE);
        when(workplace.getId()).thenReturn(10L);
        when(workplace.getName()).thenReturn("테스트 매장");

        mvc.perform(
                post("/workplaces/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "inviteCode": "ABCD1234"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("201"))
            .andExpect(jsonPath("$.data.workplaceId").value(10))
            .andExpect(jsonPath("$.data.role").value("EMPLOYEE"))
            .andExpect(jsonPath("$.data.name").value("테스트 매장"));
    }

    @Test
    @DisplayName("초대코드가 비어 있으면 400을 반환한다")
    void rejectBlankInviteCode() throws Exception {
        mvc.perform(
                post("/workplaces/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "inviteCode": ""
                        }
                        """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COM-001"));

        verifyNoInteractions(userService, workplaceService);
    }

    @Test
    @DisplayName("내 근무지 목록을 200으로 반환한다")
    void getMyWorkplaces() throws Exception {
        User user = mock(User.class);
        Workplace workplace = mock(Workplace.class);
        WorkplaceMember member = mock(WorkplaceMember.class);

        when(rq.getActorId()).thenReturn(1L);
        when(userService.getById(1L)).thenReturn(user);

        when(workplaceService.getMyWorkplaces(user))
            .thenReturn(List.of(member));

        when(member.getWorkplace()).thenReturn(workplace);
        when(member.getRole()).thenReturn(WorkplaceRole.MANAGER);
        when(workplace.getId()).thenReturn(10L);
        when(workplace.getName()).thenReturn("테스트 매장");

        mvc.perform(get("/workplaces"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("200"))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].workplaceId").value(10))
            .andExpect(jsonPath("$.data[0].name").value("테스트 매장"))
            .andExpect(jsonPath("$.data[0].role").value("MANAGER"));
    }

    @Test
    @DisplayName("근무지 구성원 목록을 200으로 반환한다")
    void getWorkplaceMembers() throws Exception {
        User memberUser = mock(User.class);
        WorkplaceMember member = mock(WorkplaceMember.class);

        when(rq.getActorId()).thenReturn(1L);

        when(workplaceService.getWorkplaceMembers(10L, 1L))
            .thenReturn(List.of(member));

        when(member.getId()).thenReturn(100L);
        when(member.getUser()).thenReturn(memberUser);
        when(member.getRole()).thenReturn(WorkplaceRole.EMPLOYEE);

        when(memberUser.getName()).thenReturn("홍길동");
        when(memberUser.getEmail()).thenReturn("test@test.com");

        mvc.perform(get("/workplaces/10/members"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].memberId").value(100))
            .andExpect(jsonPath("$.data[0].name").value("홍길동"))
            .andExpect(jsonPath("$.data[0].email").value("test@test.com"))
            .andExpect(jsonPath("$.data[0].role").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("관리자는 초대코드를 200으로 조회한다")
    void getInviteCode() throws Exception {
        Workplace workplace = mock(Workplace.class);

        when(rq.getActorId()).thenReturn(1L);

        when(workplaceService.getInviteCode(10L, 1L))
            .thenReturn(workplace);

        when(workplace.getId()).thenReturn(10L);
        when(workplace.getInviteCode()).thenReturn("ABCD1234");

        mvc.perform(get("/workplaces/10/invite-code"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.workplaceId").value(10))
            .andExpect(jsonPath("$.data.inviteCode").value("ABCD1234"));
    }

    @Test
    @DisplayName("관리자 권한이 없으면 403을 반환한다")
    void rejectUnauthorizedMembers() throws Exception {
        when(rq.getActorId()).thenReturn(1L);

        when(workplaceService.getWorkplaceMembers(10L, 1L))
            .thenThrow(new BusinessException(
                WorkplaceErrorCode.MANAGER_REQUIRED
            ));

        mvc.perform(get("/workplaces/10/members"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("WPL-004"));
    }

    @Test
    @DisplayName("잘못된 초대코드로 참여하면 404를 반환한다")
    void rejectInvalidInviteCode() throws Exception {
        User user = mock(User.class);

        when(rq.getActorId()).thenReturn(1L);
        when(userService.getById(1L)).thenReturn(user);

        when(workplaceService.joinWorkplace("INVALID", user))
            .thenThrow(new BusinessException(
                WorkplaceErrorCode.INVALID_INVITE_CODE
            ));

        mvc.perform(
                post("/workplaces/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "inviteCode": "INVALID"
                        }
                        """)
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("WPL-001"));
    }


    @Test
    @DisplayName("이미 참여한 근무지에 다시 참여하면 409를 반환한다")
    void rejectDuplicateJoin() throws Exception {
        User user = mock(User.class);

        when(rq.getActorId()).thenReturn(1L);
        when(userService.getById(1L)).thenReturn(user);

        when(workplaceService.joinWorkplace("ABCD1234", user))
            .thenThrow(new BusinessException(
                WorkplaceErrorCode.ALREADY_JOINED_WORKPLACE
            ));

        mvc.perform(
                post("/workplaces/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                    {
                        "inviteCode": "ABCD1234"
                    }
                    """)
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("WPL-002"));
    }


    @Test
    @DisplayName("존재하지 않는 근무지의 구성원 조회 시 404를 반환한다")
    void rejectMissingWorkplace() throws Exception {
        when(rq.getActorId()).thenReturn(1L);

        when(workplaceService.getWorkplaceMembers(10L, 1L))
            .thenThrow(new BusinessException(
                WorkplaceErrorCode.WORKPLACE_NOT_FOUND
            ));

        mvc.perform(get("/workplaces/10/members"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("WPL-005"));
    }


    @Test
    @DisplayName("근무지 이름 필드가 누락되면 400을 반환한다")
    void rejectMissingWorkplaceName() throws Exception {
        mvc.perform(
                post("/workplaces")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COM-001"));

        verifyNoInteractions(userService, workplaceService);
    }

    @Test
    @DisplayName("근무지 이름이 null이면 400을 반환한다")
    void rejectNullWorkplaceName() throws Exception {
        mvc.perform(
                post("/workplaces")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                    {
                        "name": null
                    }
                    """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COM-001"));

        verifyNoInteractions(userService, workplaceService);
    }

    @Test
    @DisplayName("초대코드 필드가 누락되면 400을 반환한다")
    void rejectMissingInviteCode() throws Exception {
        mvc.perform(
                post("/workplaces/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COM-001"));

        verifyNoInteractions(userService, workplaceService);
    }

    @Test
    @DisplayName("초대코드가 null이면 400을 반환한다")
    void rejectNullInviteCode() throws Exception {
        mvc.perform(
                post("/workplaces/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                    {
                        "inviteCode": null
                    }
                    """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COM-001"));

        verifyNoInteractions(userService, workplaceService);
    }

    @Test
    @DisplayName("JSON 문법이 잘못되면 400과 COM-002를 반환한다")
    void rejectMalformedJson() throws Exception {
        mvc.perform(
                post("/workplaces")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COM-002"));

        verifyNoInteractions(userService, workplaceService);
    }

    @Test
    @DisplayName("workplaceId가 숫자가 아니면 400을 반환한다")
    void rejectInvalidWorkplaceIdType() throws Exception {
        mvc.perform(
                get("/workplaces/abc/members")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COM-003"));

        verifyNoInteractions(workplaceService);
    }

    @Test
    @DisplayName("소속 근무지가 없으면 빈 배열을 200으로 반환한다")
    void getEmptyMyWorkplaces() throws Exception {
        User user = mock(User.class);

        when(rq.getActorId()).thenReturn(1L);
        when(userService.getById(1L)).thenReturn(user);
        when(workplaceService.getMyWorkplaces(user))
            .thenReturn(List.of());

        mvc.perform(get("/workplaces"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("200"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("현재 근무지 구성원이 아니면 403을 반환한다")
    void rejectNonCurrentMemberAccess() throws Exception {
        when(rq.getActorId()).thenReturn(1L);

        when(workplaceService.getWorkplaceMembers(10L, 1L))
            .thenThrow(new BusinessException(
                WorkplaceErrorCode.NOT_WORKPLACE_MEMBER
            ));

        mvc.perform(get("/workplaces/10/members"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("WPL-003"));
    }


}
