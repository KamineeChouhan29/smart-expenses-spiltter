package com.neev.expensesplitter.service;

import com.neev.expensesplitter.dto.BalanceResponse;
import com.neev.expensesplitter.model.*;
import com.neev.expensesplitter.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final SplitRepository splitRepository;

    // ── Create ────────────────────────────────────────────────────────────────

    public ExpenseGroup createGroup(String name, User creator) {
        ExpenseGroup group = ExpenseGroup.builder()
                .name(name)
                .createdBy(creator)
                .build();
        group = groupRepository.save(group);

        // Creator is automatically a member
        groupMemberRepository.save(GroupMember.builder()
                .group(group)
                .user(creator)
                .build());

        return group;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<ExpenseGroup> getUserGroups(User user) {
        return groupRepository.findGroupsByUserId(user.getId());
    }

    public List<User> getGroupMembers(Long groupId, User requestingUser) {
        assertMember(groupId, requestingUser.getId());
        return groupMemberRepository.findByGroupId(groupId)
                .stream()
                .map(GroupMember::getUser)
                .collect(Collectors.toList());
    }

    // ── Add member ────────────────────────────────────────────────────────────

    public void addMember(Long groupId, String username, User requestingUser) {
        assertMember(groupId, requestingUser.getId());

        ExpenseGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User toAdd = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User '" + username + "' not found"));

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, toAdd.getId())) {
            throw new RuntimeException(username + " is already a member of this group");
        }

        groupMemberRepository.save(GroupMember.builder()
                .group(group)
                .user(toAdd)
                .build());
    }

    // ── Balance calculation ───────────────────────────────────────────────────

    /**
     * Calculates the simplified "Who owes Whom" for a group.
     *
     * Algorithm:
     *   1. For every split where split.user ≠ payer → split.user owes payer.
     *   2. Build a net-balance map: netBalance[debtor][creditor] = total owed.
     *   3. For each unique pair (A, B), compute net = A-owes-B minus B-owes-A.
     *      If net > 0 → A still owes B the net amount.
     */
    public List<BalanceResponse> calculateBalances(Long groupId, User requestingUser) {
        assertMember(groupId, requestingUser.getId());

        List<Split> splits = splitRepository.findByGroupId(groupId);

        // netBalance[debtorId][creditorId] = amount
        Map<Long, Map<Long, BigDecimal>> net = new HashMap<>();

        for (Split split : splits) {
            Long payerId  = split.getExpense().getPaidBy().getId();
            Long debtorId = split.getUser().getId();

            if (!payerId.equals(debtorId)) {
                net.computeIfAbsent(debtorId, k -> new HashMap<>())
                   .merge(payerId, split.getAmountOwed(), BigDecimal::add);
            }
        }

        List<BalanceResponse> result = new ArrayList<>();
        Set<String> processed = new HashSet<>();

        for (Map.Entry<Long, Map<Long, BigDecimal>> dEntry : net.entrySet()) {
            Long A = dEntry.getKey();
            for (Map.Entry<Long, BigDecimal> cEntry : dEntry.getValue().entrySet()) {
                Long B = cEntry.getValue() == null ? 0L : cEntry.getKey();
                String pairKey = Math.min(A, B) + ":" + Math.max(A, B);

                if (!processed.contains(pairKey)) {
                    processed.add(pairKey);

                    BigDecimal aOwesB = net.getOrDefault(A, Collections.emptyMap())
                                          .getOrDefault(B, BigDecimal.ZERO);
                    BigDecimal bOwesA = net.getOrDefault(B, Collections.emptyMap())
                                          .getOrDefault(A, BigDecimal.ZERO);
                    BigDecimal netAmount = aOwesB.subtract(bOwesA);

                    if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                        result.add(new BalanceResponse(
                                username(A), username(B), netAmount));
                    } else if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
                        result.add(new BalanceResponse(
                                username(B), username(A), netAmount.negate()));
                    }
                }
            }
        }
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void assertMember(Long groupId, Long userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new RuntimeException("You are not a member of this group");
        }
    }

    private String username(Long userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse("Unknown");
    }
}
