package com.forum.contributorrequests;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ContributorRequestRepository extends JpaRepository<ContributorRequest, Long> {

    Page<ContributorRequest> findByStatus(ContributorRequestStatus status, Pageable pageable);

    List<ContributorRequest> findByRequesterUserIdOrderByCreatedAtDesc(Long requesterUserId);

    boolean existsByRequesterUserIdAndStatus(Long requesterUserId, ContributorRequestStatus status);

    long countByStatus(ContributorRequestStatus status);
}
