package com.forum.users;

/** Estado de la cuenta. Un usuario SUSPENDED puede autenticarse y leer, pero no escribir. */
public enum UserStatus {
    ACTIVE,
    SUSPENDED
}
