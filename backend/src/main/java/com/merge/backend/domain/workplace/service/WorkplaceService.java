package com.merge.backend.domain.workplace.service;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.dto.request.WorkplaceCreateRequest;
import com.merge.backend.domain.workplace.dto.request.WorkplaceJoinRequest;
import com.merge.backend.domain.workplace.dto.response.WorkplaceCreateResponse;
import com.merge.backend.domain.workplace.dto.response.WorkplaceJoinResponse;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.exception.WorkplaceErrorCode;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.global.exception.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkplaceService {

    private final WorkplaceRepository workplaceRepository;
    private final WorkplaceMemberRepository workplaceMemberRepository;

    @Transactional
    public WorkplaceCreateResponse createWorkplace(WorkplaceCreateRequest request, User user) {

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

    @Transactional
    public WorkplaceJoinResponse joinWorkplace(WorkplaceJoinRequest request, User user) {
        Workplace workplace = workplaceRepository.findByInviteCode(request.getInviteCode())
            .orElseThrow(() -> new BusinessException(WorkplaceErrorCode.INVALID_INVITE_CODE));

        if (workplaceMemberRepository.existsByWorkplaceAndUser(workplace, user)) {
            throw new BusinessException(WorkplaceErrorCode.ALREADY_JOINED_WORKPLACE);
        }

        WorkplaceMember member = new WorkplaceMember(workplace, user, WorkplaceRole.EMPLOYEE);

        workplaceMemberRepository.save(member);

        return new WorkplaceJoinResponse(workplace.getId(), workplace.getName(),
            WorkplaceRole.EMPLOYEE);
    }
}
