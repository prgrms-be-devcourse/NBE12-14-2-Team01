package com.merge.backend.domain.workplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WorkplaceServiceTest {

    @Mock
    private WorkplaceRepository workplaceRepository;

    @Mock
    private WorkplaceMemberRepository workplaceMemberRepository;

    @Mock
    private WorkplaceMemberService workplaceMemberService;

    @Mock
    private User user;

    private WorkplaceService workplaceService;

    private final Clock clock = Clock.fixed(
        Instant.parse("2026-09-16T00:00:00Z"),
        ZoneId.of("Asia/Seoul")
    );

    @BeforeEach
    void setUp() {
        workplaceService = new WorkplaceService(
            clock,
            workplaceRepository,
            workplaceMemberRepository,
            workplaceMemberService
        );
    }

    @Test
    @DisplayName("workplace(근무지)를 생성하면 manager(매니저 권한)가 등록된다.")
    void test1() {
        when(workplaceRepository.existsByInviteCode(any())).thenReturn(false);

        workplaceService.createWorkplace("테스트 매장", user);

        ArgumentCaptor<WorkplaceMember> memberCaptor =
            ArgumentCaptor.forClass(WorkplaceMember.class);

        verify(workplaceRepository).save(any(Workplace.class));
        verify(workplaceMemberRepository).save(memberCaptor.capture());

        assertEquals(WorkplaceRole.MANAGER, memberCaptor.getValue().getRole());
    }

    @Test
    @DisplayName("이미 참여 중인 사용자는 다시 참여할 수 없다.")
    void test2() {
        Workplace workplace = mock(Workplace.class);

        when(workplaceRepository.findByInviteCode("ABC123"))
            .thenReturn(Optional.of(workplace));

        when(workplaceMemberRepository.existsByWorkplaceAndUser(workplace, user))
            .thenReturn(true);

        assertThrows(
            BusinessException.class,
            () -> workplaceService.joinWorkplace("ABC123", user)
        );
    }

    @Test
    @DisplayName("사용자의 소속 Workplace 목록을 조회한다.")
    void test3() {
        Workplace workplace = mock(Workplace.class);
        WorkplaceMember member = mock(WorkplaceMember.class);

        when(user.getId()).thenReturn(1L);
        when(workplaceMemberRepository.findAllByUser_IdAndLeftAtIsNull(1L))
            .thenReturn(List.of(member));

        when(member.getWorkplace()).thenReturn(workplace);
        when(workplace.getId()).thenReturn(10L);
        when(workplace.getName()).thenReturn("테스트 매장");
        when(member.getRole()).thenReturn(WorkplaceRole.MANAGER);

        List<WorkplaceMember> response =
            workplaceService.getMyWorkplaces(user);

        assertEquals(1, response.size());
        assertEquals(10L, response.get(0).getWorkplace().getId());
        assertEquals("테스트 매장", response.get(0).getWorkplace().getName());
        assertEquals(WorkplaceRole.MANAGER, response.get(0).getRole());
    }

    @Test
    @DisplayName("소속 Workplace가 없으면 빈 목록을 반환한다.")
    void test4() {
        when(user.getId()).thenReturn(1L);
        when(workplaceMemberRepository.findAllByUser_IdAndLeftAtIsNull(1L))
            .thenReturn(List.of());

        List<WorkplaceMember> response =
            workplaceService.getMyWorkplaces(user);

        assertEquals(0, response.size());
    }

    @Test
    @DisplayName("관리자는 Workplace의 현재 멤버 목록을 조회한다.")
    void test5() {
        Workplace workplace = mock(Workplace.class);
        User memberUser = mock(User.class);
        WorkplaceMember member = mock(WorkplaceMember.class);

        when(workplaceRepository.findById(10L))
            .thenReturn(Optional.of(workplace));

        when(workplaceMemberRepository
            .findAllByWorkplace_IdAndLeftAtIsNull(10L))
            .thenReturn(List.of(member));

        when(member.getId()).thenReturn(100L);
        when(member.getUser()).thenReturn(memberUser);
        when(memberUser.getName()).thenReturn("홍길동");
        when(memberUser.getEmail()).thenReturn("test@test.com");
        when(member.getRole()).thenReturn(WorkplaceRole.EMPLOYEE);

        List<WorkplaceMember> response =
            workplaceService.getWorkplaceMembers(10L, 1L);

        assertEquals(100L, response.get(0).getId());
        assertEquals("홍길동", response.get(0).getUser().getName());
        assertEquals("test@test.com", response.get(0).getUser().getEmail());
        assertEquals(WorkplaceRole.EMPLOYEE, response.get(0).getRole());

        verify(workplaceMemberService).requireManager(1L, 10L);
    }

    @Test
    @DisplayName("관리자는 Workplace의 초대 코드를 조회한다.")
    void test6() {
        Workplace workplace = mock(Workplace.class);

        when(workplaceRepository.findById(10L))
            .thenReturn(Optional.of(workplace));
        when(workplace.getId()).thenReturn(10L);
        when(workplace.getInviteCode()).thenReturn("ABCD1234");

        Workplace response =
            workplaceService.getInviteCode(10L, 1L);

        assertEquals(10L, response.getId());
        assertEquals("ABCD1234", response.getInviteCode());

        verify(workplaceMemberService).requireManager(1L, 10L);
    }
}
