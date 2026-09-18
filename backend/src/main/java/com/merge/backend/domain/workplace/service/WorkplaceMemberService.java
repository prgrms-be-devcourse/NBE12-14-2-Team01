package com.merge.backend.domain.workplace.service;

import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.exception.WorkplaceErrorCode;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkplaceMemberService {

    private final WorkplaceMemberRepository workplaceMemberRepository;

    public WorkplaceMember requireManager(Long actorUserId, Long workplaceId) {

        WorkplaceMember member = workplaceMemberRepository
            .findByWorkplace_IdAndUser_IdAndLeftAtIsNull(workplaceId, actorUserId)
            .orElseThrow(() -> new BusinessException(
                WorkplaceErrorCode.NOT_WORKPLACE_MEMBER
            ));

        if (member.getRole() != WorkplaceRole.MANAGER) {

            throw new BusinessException(
                WorkplaceErrorCode.MANAGER_REQUIRED
            );
        }

        return member;
    }
}
