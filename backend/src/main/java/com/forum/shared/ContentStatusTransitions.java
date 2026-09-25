package com.forum.shared;

import com.forum.shared.error.ApiException;

/**
 * Transiciones de estado válidas para posts y comentarios (mismas reglas):
 * <ul>
 *   <li>Autor: {@code ACTIVE → DELETED_BY_USER}</li>
 *   <li>Moderador: {@code ACTIVE → HIDDEN | DELETED_BY_MOD}, {@code HIDDEN → ACTIVE | DELETED_BY_MOD}</li>
 * </ul>
 * Los estados {@code DELETED_*} son terminales. Cualquier otra transición es inválida (409).
 */
public final class ContentStatusTransitions {

    private ContentStatusTransitions() {
    }

    /** Devuelve true si la transición pedida está permitida para el actor. */
    public static boolean isAllowed(ContentStatus current, ContentStatus target, boolean isAuthor, boolean isModerator) {
        if (isModerator) {
            if (current == ContentStatus.ACTIVE
                    && (target == ContentStatus.HIDDEN || target == ContentStatus.DELETED_BY_MOD)) {
                return true;
            }
            if (current == ContentStatus.HIDDEN
                    && (target == ContentStatus.ACTIVE || target == ContentStatus.DELETED_BY_MOD)) {
                return true;
            }
        }
        if (isAuthor && current == ContentStatus.ACTIVE && target == ContentStatus.DELETED_BY_USER) {
            return true;
        }
        return false;
    }

    /** Valida la transición o lanza 409 si no está permitida. */
    public static void validate(ContentStatus current, ContentStatus target, boolean isAuthor, boolean isModerator) {
        if (!isAllowed(current, target, isAuthor, isModerator)) {
            throw ApiException.conflict(
                    "Transición de estado inválida: " + current + " → " + target);
        }
    }
}
