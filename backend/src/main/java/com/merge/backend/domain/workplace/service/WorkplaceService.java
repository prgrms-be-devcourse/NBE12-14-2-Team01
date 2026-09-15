package com.merge.backend.domain.workplace.service;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.dto.request.WorkplaceCreateRequest;
import com.merge.backend.domain.workplace.dto.request.WorkplaceJoinRequest;
import com.merge.backend.domain.workplace.dto.response.WorkplaceCreateResponse;
import com.merge.backend.domain.workplace.dto.response.WorkplaceJoinResponse;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkplaceService {

    private final WorkplaceRepository workplaceRepository;
    private final WorkplaceMemberRepository workplaceMemberRepository;

    public WorkplaceCreateResponse createResponse(WorkplaceCreateRequest request, User user) {

        String inviteCode = createInviteCode();

        Workplace workplace = new Workplace(request.getName(), inviteCode);

        workplaceRepository.save(workplace);

        WorkplaceMember member = new WorkplaceMember(workplace, user, WorkplaceRole.MANAGER);

        workplaceMemberRepository.save(member);

        return new WorkplaceCreateResponse(workplace.getId(), workplace.getName(),
            workplace.getInviteCode(), WorkplaceRole.MANAGER);
    }

    private String createInviteCode() {
        String inviteCode;

        do {
            inviteCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (workplaceRepository.existsByInviteCode(inviteCode));

        return inviteCode;
    }

    public WorkplaceJoinResponse joinWorkplace(WorkplaceJoinRequest request, User user) {
        Workplace workplace = workplaceRepository.findAllByInviteCode(request.getInviteCode())
            .orElseThrow(() -> new IllegalArgumentException("유효하지않은 초대 코드입니다."));

        if (workplaceMemberRepository.existsByWorkplaceAndUser(workplace, user)) {
            throw new IllegalArgumentException("이미 참여 중인 근무지입니다.");
        }

        WorkplaceMember member = new WorkplaceMember(workplace, user, WorkplaceRole.EMPLOYEE);

        workplaceMemberRepository.save(member);

        return new WorkplaceJoinResponse(workplace.getId(), workplace.getName(),
            WorkplaceRole.EMPLOYEE);
    }
}
