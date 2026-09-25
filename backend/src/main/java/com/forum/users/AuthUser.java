package com.forum.users;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.forum.shared.error.ApiException;

/**
 * Principal autenticado. Se construye desde la base en cada request (HTTP Basic es stateless),
 * por lo que el rol y el estado siempre están actualizados. Los módulos lo reciben con
 * {@code @AuthenticationPrincipal} para saber quién ejecuta la acción.
 */
public class AuthUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Role role;
    private final UserStatus status;

    public AuthUser(Long id, String username, String password, Role role, UserStatus status) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.status = status;
    }

    static AuthUser from(User user) {
        return new AuthUser(user.getId(), user.getUsername(), user.getPassword(), user.getRole(), user.getStatus());
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    /** Un usuario suspendido puede leer pero no escribir: cualquier escritura devuelve 403. */
    public void ensureCanWrite() {
        if (!isActive()) {
            throw ApiException.forbidden("El usuario está suspendido y no puede escribir");
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
