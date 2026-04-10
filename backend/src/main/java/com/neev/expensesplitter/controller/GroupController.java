package com.neev.expensesplitter.controller;

import com.neev.expensesplitter.dto.*;
import com.neev.expensesplitter.model.ExpenseGroup;
import com.neev.expensesplitter.model.User;
import com.neev.expensesplitter.repository.UserRepository;
import com.neev.expensesplitter.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final UserRepository userRepository;

    // ── GET  /api/groups  →  all groups the current user belongs to ───────────
    @GetMapping
    public ResponseEntity<List<GroupResponse>> getMyGroups(Principal principal) {
        User user = resolveUser(principal);
        List<GroupResponse> response = groupService.getUserGroups(user)
                .stream()
                .map(this::toGroupResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // ── POST /api/groups  →  create a new group ───────────────────────────────
    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody GroupRequest request,
                                                     Principal principal) {
        User user = resolveUser(principal);
        ExpenseGroup group = groupService.createGroup(request.name(), user);
        return ResponseEntity.ok(toGroupResponse(group));
    }

    // ── POST /api/groups/{id}/members  →  add a member ───────────────────────
    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addMember(@PathVariable Long id,
                                          @Valid @RequestBody AddMemberRequest request,
                                          Principal principal) {
        User user = resolveUser(principal);
        groupService.addMember(id, request.username(), user);
        return ResponseEntity.ok().build();
    }

    // ── GET  /api/groups/{id}/members  →  list members ───────────────────────
    @GetMapping("/{id}/members")
    public ResponseEntity<List<UserResponse>> getMembers(@PathVariable Long id,
                                                         Principal principal) {
        User user = resolveUser(principal);
        List<UserResponse> members = groupService.getGroupMembers(id, user)
                .stream()
                .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getEmail()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(members);
    }

    // ── GET  /api/groups/{id}/balances  →  who owes whom ─────────────────────
    @GetMapping("/{id}/balances")
    public ResponseEntity<List<BalanceResponse>> getBalances(@PathVariable Long id,
                                                             Principal principal) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(groupService.calculateBalances(id, user));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User resolveUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    private GroupResponse toGroupResponse(ExpenseGroup g) {
        return new GroupResponse(
                g.getId(),
                g.getName(),
                g.getCreatedBy().getUsername(),
                g.getCreatedAt());
    }
}
