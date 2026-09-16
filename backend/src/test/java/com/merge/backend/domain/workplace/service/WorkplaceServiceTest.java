package com.merge.backend.domain.workplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.dto.request.WorkplaceCreateRequest;
import com.merge.backend.domain.workplace.dto.request.WorkplaceJoinRequest;
import com.merge.backend.domain.workplace.dto.response.WorkplaceCreateResponse;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WorkplaceServiceTest {

    @Mock
    private WorkplaceRepository workplaceRepository;

    @Mock
    private WorkplaceMemberRepository workplaceMemberRepository;

    @InjectMocks
    private WorkplaceService workplaceService;

    @Test
    @DisplayName("workplace(근무지)를 생성하면 maneger(매니저 권환)가 등록된다.")
    void test1() {
        WorkplaceCreateRequest request = mock(WorkplaceCreateRequest.class);
        User user = mock(User.class);

        when(request.getName()).thenReturn("테스트 매장");
        when(workplaceRepository.existsByInviteCode(any())).thenReturn(false);

        WorkplaceCreateResponse response = workplaceService.createWorkplace(request, user);

        verify(workplaceRepository).save(any(Workplace.class));
        verify(workplaceMemberRepository).save(any(WorkplaceMember.class));

        assertEquals("테스트 매장", response.getName());
        assertEquals(WorkplaceRole.MANAGER, response.getRole());
    }

    @Test
    @DisplayName("이미 참여 중인 사용자는 다시 참여할 수 없다.")
    void test2() {
        WorkplaceJoinRequest request = mock(WorkplaceJoinRequest.class);
        User user = mock(User.class);
        Workplace workplace = mock(Workplace.class);

        when(request.getInviteCode()).thenReturn("ABC123");
        when(workplaceRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(workplace));
        when(workplaceMemberRepository.existsByWorkplaceAndUser(workplace, user)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> workplaceService.joinWorkplace(request, user));
    }
}
