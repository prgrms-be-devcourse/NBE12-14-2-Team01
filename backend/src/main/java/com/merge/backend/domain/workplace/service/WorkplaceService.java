package com.merge.backend.domain.workplace.service;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.exception.WorkplaceErrorCode;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.global.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkplaceService {

    private final Clock clock;
    private final WorkplaceRepository workplaceRepository;
    private final WorkplaceMemberRepository workplaceMemberRepository;
    private final WorkplaceMemberService workplaceMemberService;

    @Transactional
    public WorkplaceMember createWorkplace(String name, User user) {

        String inviteCode = createInviteCode();

        Workplace workplace = new Workplace(name, inviteCode);

        workplaceRepository.save(workplace);

        WorkplaceMember member = new WorkplaceMember(
            workplace,
            user,
            WorkplaceRole.MANAGER,
            LocalDateTime.now(clock)
        );

        return workplaceMemberRepository.save(member);
    }

    private String createInviteCode() {

        String inviteCode;

        do {
            inviteCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (workplaceRepository.existsByInviteCode(inviteCode));

        return inviteCode;
    }

    @Transactional
    public WorkplaceMember joinWorkplace(String inviteCode, User user) {

        Workplace workplace = workplaceRepository.findByInviteCode(inviteCode)
            .orElseThrow(() -> new BusinessException(WorkplaceErrorCode.INVALID_INVITE_CODE));

        if (workplaceMemberRepository.existsByWorkplaceAndUser(workplace, user)) {
            throw new BusinessException(WorkplaceErrorCode.ALREADY_JOINED_WORKPLACE);
        }

        WorkplaceMember member = new WorkplaceMember(
            workplace,
            user,
            WorkplaceRole.EMPLOYEE,
            LocalDateTime.now(clock)
        );

        return workplaceMemberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<WorkplaceMember> getMyWorkplaces(User user) {
        return workplaceMemberRepository
            .findAllByUser_IdAndLeftAtIsNull(user.getId());
    }

    @Transactional(readOnly = true)
    public List<WorkplaceMember> getWorkplaceMembers(
        Long workplaceId,
        Long actorUserId
    ) {

        getWorkplace(workplaceId);

        workplaceMemberService.requireManager(actorUserId, workplaceId);

        return workplaceMemberRepository
            .findAllByWorkplace_IdAndLeftAtIsNull(workplaceId);
    }

    private Workplace getWorkplace(Long workplaceId) {
        return workplaceRepository.findById(workplaceId)
            .orElseThrow(() -> new BusinessException(
                WorkplaceErrorCode.WORKPLACE_NOT_FOUND
            ));
    }

    @Transactional(readOnly = true)
    public Workplace getInviteCode(
        Long workplaceId,
        Long actorUserId
    ) {
        Workplace workplace = getWorkplace(workplaceId);

        workplaceMemberService.requireManager(actorUserId, workplaceId);

        return workplace;
    }
}
