package com.merge.backend.domain.workplace.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WorkplaceMemberServiceTest {

    @Mock
    private WorkplaceMemberRepository workplaceMemberRepository;

    @InjectMocks
    private WorkplaceMemberService workplaceMemberService;

    @Test
    @DisplayName("현재 MANAGER이면 WorkplaceMember를 반환한다.")
    void test1() {
        Long actorUserId = 1L;
        Long workplaceId = 1L;
        WorkplaceMember member = mock(WorkplaceMember.class);

        when(workplaceMemberRepository
            .findByWorkplace_IdAndUser_IdAndLeftAtIsNull(workplaceId, actorUserId))
            .thenReturn(Optional.of(member));
        when(member.getRole()).thenReturn(WorkplaceRole.MANAGER);

        WorkplaceMember result =
            workplaceMemberService.requireManager(actorUserId, workplaceId);

        assertSame(member, result);
    }
}