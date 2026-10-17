package com.forum.users;

import java.time.Instant;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
@BatchSize(size = 50)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 500)
    private String bio;

    // Campos de perfil de comercio/growshop (opcionales). Solo se completan si el rol alcanza
    // forum.rules.business-profile-min-role; para el resto quedan null.
    @Column(length = 255)
    private String website;

    @Column(length = 30)
    private String phone;

    @Column(length = 50)
    private String instagram;

    @Column(length = 100)
    private String location;

    // Imágenes del usuario (avatar y portada). Guardamos la clave de almacenamiento y el tipo;
    // el archivo vive en el ImageStorage. Null si no subió.
    @Column(name = "avatar_storage_key", length = 100)
    private String avatarStorageKey;

    @Column(name = "avatar_content_type", length = 30)
    private String avatarContentType;

    @Column(name = "cover_storage_key", length = 100)
    private String coverStorageKey;

    @Column(name = "cover_content_type", length = 30)
    private String coverContentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {
        // requerido por JPA
    }

    public User(String username, String email, String password, Role role, UserStatus status) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAvatarStorageKey() {
        return avatarStorageKey;
    }

    public String getAvatarContentType() {
        return avatarContentType;
    }

    public void setAvatar(String storageKey, String contentType) {
        this.avatarStorageKey = storageKey;
        this.avatarContentType = contentType;
    }

    public String getCoverStorageKey() {
        return coverStorageKey;
    }

    public String getCoverContentType() {
        return coverContentType;
    }

    public void setCover(String storageKey, String contentType) {
        this.coverStorageKey = storageKey;
        this.coverContentType = contentType;
    }

    /** URL pública del avatar (o null si no tiene). El {@code v} invalida caché al cambiar. */
    public String getAvatarUrl() {
        return avatarStorageKey == null ? null
                : "/users/" + id + "/avatar?v=" + Integer.toHexString(avatarStorageKey.hashCode());
    }

    /** URL pública de la portada (o null si no tiene). */
    public String getCoverUrl() {
        return coverStorageKey == null ? null
                : "/users/" + id + "/cover?v=" + Integer.toHexString(coverStorageKey.hashCode());
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
