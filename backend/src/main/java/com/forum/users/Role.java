package com.forum.users;

/**
 * Roles del foro, en orden jerárquico ascendente: cada rol incluye los permisos de los
 * anteriores. El orden de declaración define la jerarquía usada por {@link #isAtLeast}.
 */
public enum Role {
    USER,
    CONTRIBUTOR,
    MODERATOR,
    ADMIN;

    /** Devuelve true si este rol es igual o superior a {@code other} en la jerarquía. */
    public boolean isAtLeast(Role other) {
        return this.ordinal() >= other.ordinal();
    }
}
