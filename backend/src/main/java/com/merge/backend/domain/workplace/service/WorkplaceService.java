package com.merge.backend.domain.workplace.service;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.dto.request.WorkplaceCreateRequest;
import com.merge.backend.domain.workplace.dto.request.WorkplaceJoinRequest;
import com.merge.backend.domain.workplace.dto.response.MyWorkplaceResponse;
import com.merge.backend.domain.workplace.dto.response.WorkplaceCreateResponse;
import com.merge.backend.domain.workplace.dto.response.WorkplaceInviteCodeResponse;
import com.merge.backend.domain.workplace.dto.response.WorkplaceJoinResponse;
import com.merge.backend.domain.workplace.dto.response.WorkplaceMemberResponse;
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
    public WorkplaceCreateResponse createWorkplace(WorkplaceCreateRequest request, User user) {

        String inviteCode = createInviteCode();

        Workplace workplace = new Workplace(request.getName(), inviteCode);

        workplaceRepository.save(workplace);

        WorkplaceMember member = new WorkplaceMember(
            workplace,
            user,
            WorkplaceRole.MANAGER,
            LocalDateTime.now(clock)
        );

        workplaceMemberRepository.save(member);

        return new WorkplaceCreateResponse(
            workplace.getId(),
            workplace.getName(),
            WorkplaceRole.MANAGER
        );
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

        WorkplaceMember member = new WorkplaceMember(
            workplace,
            user,
            WorkplaceRole.EMPLOYEE,
            LocalDateTime.now(clock)
        );

        workplaceMemberRepository.save(member);

        return new WorkplaceJoinResponse(
            workplace.getId(),
            workplace.getName(),
            WorkplaceRole.EMPLOYEE
        );
    }

    @Transactional(readOnly = true)
    public List<MyWorkplaceResponse> getMyWorkplaces(User user) {
        return workplaceMemberRepository
            .findAllByUser_IdAndLeftAtIsNull(user.getId())
            .stream()
            .map(member -> new MyWorkplaceResponse(
                member.getWorkplace().getId(),
                member.getWorkplace().getName(),
                member.getRole()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkplaceMemberResponse> getWorkplaceMembers(
        Long workplaceId,
        Long actorUserId
    ) {

        getWorkplace(workplaceId);

        workplaceMemberService.requireManager(actorUserId, workplaceId);

        return workplaceMemberRepository
            .findAllByWorkplace_IdAndLeftAtIsNull(workplaceId)
            .stream()
            .map(member -> new WorkplaceMemberResponse(
                member.getId(),
                member.getUser().getName(),
                member.getUser().getEmail(),
                member.getRole()
            ))
            .toList();
    }

    private Workplace getWorkplace(Long workplaceId) {
        return workplaceRepository.findById(workplaceId)
            .orElseThrow(() -> new BusinessException(
                WorkplaceErrorCode.WORKPLACE_NOT_FOUND
            ));
    }

    @Transactional(readOnly = true)
    public WorkplaceInviteCodeResponse getInviteCode(
        Long workplaceId,
        Long actorUserId
    ) {
        Workplace workplace = getWorkplace(workplaceId);

        workplaceMemberService.requireManager(actorUserId, workplaceId);

        return new WorkplaceInviteCodeResponse(
            workplace.getId(),
            workplace.getInviteCode()
        );
    }
}
