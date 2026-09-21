package com.merge.backend.domain.substitute.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.shift.repository.UnavailableTimeRepository;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import com.merge.backend.domain.substitute.repository.SubstituteCandidateRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubstituteCandidateServiceTest {

    @Mock
    private WorkplaceMemberRepository workplaceMemberRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private UnavailableTimeRepository unavailableTimeRepository;

    @Mock
    private SubstituteCandidateRepository substituteCandidateRepository;

    @InjectMocks
    private SubstituteCandidateService substituteCandidateService;

    @Test
    @DisplayName("SUB-02 - 현재 사용자에게 온 응답 가능한 대타 요청을 조회한다")
    void findReceivedRequests_Success() {

        // given
        Long userId = 1L; // 현재 로그인한 사용자

        SubstituteCandidate candidate1 =
            mock(SubstituteCandidate.class); // 조회될 대타 후보 1

        SubstituteCandidate candidate2 =
            mock(SubstituteCandidate.class); // 조회될 대타 후보 2

        List<SubstituteCandidate> expected =
            List.of(candidate1, candidate2); // Repository가 돌려줄 결과

        when(
            substituteCandidateRepository.findReceivedRequests(
                eq(userId),
                any(LocalDateTime.class)
            )
        ).thenReturn(expected);

        // when
        List<SubstituteCandidate> result =
            substituteCandidateService.findReceivedRequests(userId);

        // then
        assertThat(result)
            .containsExactly(candidate1, candidate2);

        verify(substituteCandidateRepository)
            .findReceivedRequests(
                eq(userId),
                any(LocalDateTime.class)
            );
    }
}