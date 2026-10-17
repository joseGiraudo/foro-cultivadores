package com.forum.contributorrequests;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forum.users.AuthUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/contributor-requests")
class ContributorRequestController {

    private final ContributorRequestService requestService;

    ContributorRequestController(ContributorRequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ContributorRequestResponse create(@AuthenticationPrincipal AuthUser principal,
                                      @Valid @RequestBody CreateContributorRequestRequest request) {
        return requestService.create(principal, request);
    }

    @GetMapping("/me")
    List<ContributorRequestResponse> myRequests(@AuthenticationPrincipal AuthUser principal) {
        return requestService.myRequests(principal);
    }

    @GetMapping
    Page<ContributorRequestResponse> list(@RequestParam(required = false) ContributorRequestStatus status,
                                          @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return requestService.list(status, pageable);
    }

    @GetMapping("/pending-count")
    PendingCountResponse pendingCount() {
        return new PendingCountResponse(requestService.pendingCount());
    }

    @PostMapping("/{id}/approve")
    ContributorRequestResponse approve(@AuthenticationPrincipal AuthUser staff, @PathVariable Long id) {
        return requestService.approve(staff, id);
    }

    @PostMapping("/{id}/reject")
    ContributorRequestResponse reject(@AuthenticationPrincipal AuthUser staff, @PathVariable Long id) {
        return requestService.reject(staff, id);
    }

    record PendingCountResponse(long count) {
    }
}
