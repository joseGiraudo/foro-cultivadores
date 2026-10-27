package com.forum.users;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forum.images.ImageStorage;
import com.forum.rules.ForumPolicy;
import com.forum.shared.error.ApiException;

@Service
public class UserService {

    /** Versión vigente de Términos + Privacidad. Subir esto permitiría re-pedir aceptación a futuro. */
    public static final String TERMS_VERSION = "2026-10-27";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FollowStats followStats;
    private final ForumPolicy policy;
    private final AccountService accountService;
    private final ImageStorage imageStorage;

    UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, FollowStats followStats,
                ForumPolicy policy, AccountService accountService, ImageStorage imageStorage) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.followStats = followStats;
        this.policy = policy;
        this.accountService = accountService;
        this.imageStorage = imageStorage;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw ApiException.conflict("El nombre de usuario ya está en uso");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.conflict("El email ya está en uso");
        }
        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.USER,
                UserStatus.ACTIVE);
        user.acceptTerms(TERMS_VERSION); // registró el consentimiento (18 + T&C/Privacidad)
        User saved = userRepository.save(user);
        // El email arranca sin verificar: se manda el link de verificación (no puede loguear hasta confirmar).
        accountService.sendVerificationEmail(saved);
        return UserResponse.of(saved, false, 0, 0, null);
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(AuthUser principal) {
        return toResponse(getEntityById(principal.getId()), true, principal);
    }

    /**
     * Baja de cuenta (derecho de supresión): anonimiza los datos personales y borra los archivos de
     * avatar/portada. Los posts/comentarios se conservan, mostrados como "[cuenta eliminada]". La
     * cuenta queda sin poder iniciar sesión (contraseña ilegible).
     */
    @Transactional
    public void deleteMe(AuthUser principal) {
        User user = getEntityById(principal.getId());
        String avatarKey = user.getAvatarStorageKey();
        String coverKey = user.getCoverStorageKey();
        user.anonymize();
        if (avatarKey != null) {
            imageStorage.delete(avatarKey);
        }
        if (coverKey != null) {
            imageStorage.delete(coverKey);
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getPublicProfile(Long id, AuthUser principal) {
        return toResponse(getEntityById(id), false, principal);
    }

    @Transactional(readOnly = true)
    public UserResponse getPublicProfileByUsername(String username, AuthUser principal) {
        return toResponse(getByUsername(username), false, principal);
    }

    @Transactional
    public UserResponse updateMe(AuthUser principal, UpdateMeRequest request) {
        principal.ensureCanWrite();
        User user = getEntityById(principal.getId());
        user.setBio(blankToNull(request.bio()));

        // Datos de comercio: solo si el rol alcanza. Si un rol insuficiente los envía, 403.
        boolean hasBusinessData = anyPresent(
                request.website(), request.phone(), request.instagram(), request.location());
        boolean canSetBusiness = principal.getRole().isAtLeast(policy.businessProfileMinRole());
        if (hasBusinessData && !canSetBusiness) {
            throw ApiException.forbidden("Tu rol no permite completar datos de comercio");
        }
        if (canSetBusiness) {
            user.setWebsite(blankToNull(request.website()));
            user.setPhone(blankToNull(request.phone()));
            user.setInstagram(blankToNull(request.instagram()));
            user.setLocation(blankToNull(request.location()));
        }
        return toResponse(user, true, principal);
    }

    /** Normaliza strings: espacios en blanco → null, para no guardar cadenas vacías. */
    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean anyPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public UserResponse changeRole(AuthUser admin, Long targetId, Role role) {
        admin.ensureCanWrite();
        if (targetId.equals(admin.getId())) {
            throw ApiException.conflict("Un administrador no puede cambiar su propio rol");
        }
        User target = getEntityById(targetId);
        target.setRole(role);
        return toResponse(target, true, admin);
    }

    /**
     * Promueve un usuario común a CONTRIBUTOR. A diferencia de {@link #changeRole} (admin-only),
     * este salto acotado lo puede ejecutar cualquier integrante del staff (MODERATOR+): es el camino
     * por el que se aprueba una petición de contributor. Solo aplica a usuarios con rol USER.
     */
    @Transactional
    public User promoteToContributor(AuthUser staff, Long targetId) {
        staff.ensureCanWrite();
        User target = getEntityById(targetId);
        if (target.getRole() != Role.USER) {
            throw ApiException.conflict("Solo se puede promover a usuarios comunes");
        }
        target.setRole(Role.CONTRIBUTOR);
        return target;
    }

    @Transactional
    public UserResponse changeStatus(AuthUser admin, Long targetId, UserStatus status) {
        admin.ensureCanWrite();
        if (targetId.equals(admin.getId())) {
            throw ApiException.conflict("Un administrador no puede cambiar su propio estado");
        }
        User target = getEntityById(targetId);
        target.setStatus(status);
        return toResponse(target, true, admin);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Role role, UserStatus status, AuthUser admin, Pageable pageable) {
        Page<User> page;
        if (role != null && status != null) {
            page = userRepository.findByRoleAndStatus(role, status, pageable);
        } else if (role != null) {
            page = userRepository.findByRole(role, pageable);
        } else if (status != null) {
            page = userRepository.findByStatus(status, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        List<Long> ids = page.getContent().stream().map(User::getId).toList();
        Map<Long, Long> followerCounts = followStats.followerCounts(ids);
        Map<Long, Long> followingCounts = followStats.followingCounts(ids);
        Set<Long> followed = admin == null ? Set.of() : followStats.followingAmong(admin.getId(), ids);
        return page.map(user -> UserResponse.of(user, true,
                followerCounts.getOrDefault(user.getId(), 0L),
                followingCounts.getOrDefault(user.getId(), 0L),
                admin == null ? null : followed.contains(user.getId())));
    }

    /**
     * Construye respuestas (perfil público, sin email) para una lista de ids preservando el orden.
     * Usado por el módulo de follows para los listados de seguidores/seguidos, sin N+1.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> responsesForIds(List<Long> ids, AuthUser principal) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, User> byId = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Long> followerCounts = followStats.followerCounts(ids);
        Map<Long, Long> followingCounts = followStats.followingCounts(ids);
        Set<Long> followed = principal == null ? Set.of() : followStats.followingAmong(principal.getId(), ids);
        return ids.stream()
                .map(byId::get)
                .filter(user -> user != null)
                .map(user -> UserResponse.of(user, false,
                        followerCounts.getOrDefault(user.getId(), 0L),
                        followingCounts.getOrDefault(user.getId(), 0L),
                        principal == null ? null : followed.contains(user.getId())))
                .toList();
    }

    /**
     * Devuelve la entidad {@link User} por id, o 404 si no existe. Pensado para que otros
     * módulos resuelvan el autor a partir del usuario autenticado.
     */
    @Transactional(readOnly = true)
    public User getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }

    /**
     * Carga varios usuarios por id en un mapa id→User. Pensado para que otros módulos resuelvan
     * en lote los datos de usuarios de una lista (evita N+1).
     */
    @Transactional(readOnly = true)
    public Map<Long, User> getEntitiesByIds(Collection<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    /** Busca un usuario por username (usado por el seeder de contenido en perfil dev). */
    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado: " + username));
    }

    private UserResponse toResponse(User user, boolean includeEmail, AuthUser principal) {
        List<Long> ids = List.of(user.getId());
        long followerCount = followStats.followerCounts(ids).getOrDefault(user.getId(), 0L);
        long followingCount = followStats.followingCounts(ids).getOrDefault(user.getId(), 0L);
        Boolean followedByMe = principal == null ? null
                : followStats.followingAmong(principal.getId(), ids).contains(user.getId());
        return UserResponse.of(user, includeEmail, followerCount, followingCount, followedByMe);
    }
}
