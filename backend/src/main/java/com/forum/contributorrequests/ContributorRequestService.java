package com.forum.contributorrequests;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forum.shared.error.ApiException;
import com.forum.users.AuthUser;
import com.forum.users.Role;
import com.forum.users.User;
import com.forum.users.UserService;

@Service
class ContributorRequestService {

    private final ContributorRequestRepository requestRepository;
    private final UserService userService;
    private final ContributorRequestNotifier notifier;

    ContributorRequestService(ContributorRequestRepository requestRepository, UserService userService,
                              ContributorRequestNotifier notifier) {
        this.requestRepository = requestRepository;
        this.userService = userService;
        this.notifier = notifier;
    }

    /** Un usuario común crea una petición. 409 si ya es Grow+ o si ya tiene una pendiente. */
    @Transactional
    public ContributorRequestResponse create(AuthUser principal, CreateContributorRequestRequest request) {
        principal.ensureCanWrite();
        if (principal.getRole().isAtLeast(Role.CONTRIBUTOR)) {
            throw ApiException.conflict("Ya sos Grow o tenés un rol superior; no necesitás solicitarlo.");
        }
        if (requestRepository.existsByRequesterUserIdAndStatus(principal.getId(),
                ContributorRequestStatus.PENDING)) {
            throw ApiException.conflict("Ya tenés una petición pendiente de revisión.");
        }
        ContributorRequest saved = requestRepository.save(
                new ContributorRequest(principal.getId(), request.message(), request.contact()));
        return toResponse(saved);
    }

    /** Bandeja del staff: lista por estado (o todas si {@code status} es null). */
    @Transactional(readOnly = true)
    public Page<ContributorRequestResponse> list(ContributorRequestStatus status, Pageable pageable) {
        Page<ContributorRequest> page = status == null
                ? requestRepository.findAll(pageable)
                : requestRepository.findByStatus(status, pageable);
        Map<Long, User> users = loadUsers(page.getContent());
        return page.map(r -> toResponse(r, users));
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return requestRepository.countByStatus(ContributorRequestStatus.PENDING);
    }

    /** Historial propio del solicitante (más reciente primero). */
    @Transactional(readOnly = true)
    public List<ContributorRequestResponse> myRequests(AuthUser principal) {
        List<ContributorRequest> requests =
                requestRepository.findByRequesterUserIdOrderByCreatedAtDesc(principal.getId());
        Map<Long, User> users = loadUsers(requests);
        return requests.stream().map(r -> toResponse(r, users)).toList();
    }

    /** Aprueba la petición: promueve al solicitante a CONTRIBUTOR y le avisa. */
    @Transactional
    public ContributorRequestResponse approve(AuthUser staff, Long id) {
        ContributorRequest request = resolvable(staff, id);
        userService.promoteToContributor(staff, request.getRequesterUserId());
        request.resolve(ContributorRequestStatus.APPROVED, staff.getId());
        notifier.requestResolved(request.getRequesterUserId(), staff.getId(), staff.getUsername(), true);
        return toResponse(request);
    }

    /** Rechaza la petición (sin cambio de rol) y avisa al solicitante. */
    @Transactional
    public ContributorRequestResponse reject(AuthUser staff, Long id) {
        ContributorRequest request = resolvable(staff, id);
        request.resolve(ContributorRequestStatus.REJECTED, staff.getId());
        notifier.requestResolved(request.getRequesterUserId(), staff.getId(), staff.getUsername(), false);
        return toResponse(request);
    }

    /** Carga una petición y valida que el staff pueda resolverla (existe y está pendiente). */
    private ContributorRequest resolvable(AuthUser staff, Long id) {
        staff.ensureCanWrite();
        ContributorRequest request = requestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Petición no encontrada"));
        if (request.getStatus() != ContributorRequestStatus.PENDING) {
            throw ApiException.conflict("La petición ya fue resuelta.");
        }
        return request;
    }

    /** Carga en lote los usuarios (solicitantes + resolutores) de una lista de peticiones. */
    private Map<Long, User> loadUsers(List<ContributorRequest> requests) {
        Set<Long> ids = new HashSet<>();
        for (ContributorRequest r : requests) {
            ids.add(r.getRequesterUserId());
            if (r.getResolvedByUserId() != null) {
                ids.add(r.getResolvedByUserId());
            }
        }
        return userService.getEntitiesByIds(ids);
    }

    private ContributorRequestResponse toResponse(ContributorRequest request, Map<Long, User> users) {
        User requester = users.get(request.getRequesterUserId());
        User resolver = request.getResolvedByUserId() == null ? null
                : users.get(request.getResolvedByUserId());
        return ContributorRequestResponse.of(request, requester, resolver);
    }

    /** Versión para un único resultado (create/approve/reject): resuelve los usuarios directo. */
    private ContributorRequestResponse toResponse(ContributorRequest request) {
        User requester = userService.getEntityById(request.getRequesterUserId());
        User resolver = request.getResolvedByUserId() == null ? null
                : userService.getEntityById(request.getResolvedByUserId());
        return ContributorRequestResponse.of(request, requester, resolver);
    }
}
