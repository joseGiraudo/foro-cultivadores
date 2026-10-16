package com.forum.rules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import com.forum.shared.PostType;
import com.forum.users.Role;

/**
 * Única clase de política del foro. Centraliza todas las reglas configurables (roles mínimos,
 * orden de comentarios, límites de imágenes, etc.) leídas de {@code application.yml} bajo el
 * prefijo {@code forum}. Los services consultan esta clase; ningún rol mínimo se hardcodea.
 *
 * <p>Al arrancar valida que la configuración esté completa (todos los {@link PostType} presentes
 * en los mapas y los roles mínimos definidos); si falta algo, la aplicación falla con un mensaje
 * claro.
 */
@ConfigurationProperties(prefix = "forum")
public class ForumPolicy implements InitializingBean {

    private Rules rules = new Rules();
    private Images images = new Images();
    private Storage storage = new Storage();

    // --- Acceso conveniente para los services ---

    public Role postCreationMinRole(PostType type) {
        return rules.postCreationMinRole.get(type);
    }

    public CommentOrder commentOrder(PostType type) {
        return rules.commentOrder.get(type);
    }

    public Role commentMinRole() {
        return rules.commentMinRole;
    }

    public Role postVoteMinRole() {
        return rules.postVoteMinRole;
    }

    public Role commentVoteMinRole() {
        return rules.commentVoteMinRole;
    }

    public boolean isSelfVoteAllowed() {
        return rules.allowSelfVote;
    }

    public Role followMinRole() {
        return rules.followMinRole;
    }

    public Role followTargetMinRole() {
        return rules.followTargetMinRole;
    }

    public Role moderationMinRole() {
        return rules.moderationMinRole;
    }

    public Role businessProfileMinRole() {
        return rules.businessProfileMinRole;
    }

    public int maxImagesPerPost() {
        return images.maxPerPost;
    }

    public DataSize maxImageFileSize() {
        return images.maxFileSize;
    }

    public List<String> allowedImageTypes() {
        return images.allowedTypes;
    }

    public String imageStorageBasePath() {
        return storage.local.basePath;
    }

    // --- Validación al arranque ---

    @Override
    public void afterPropertiesSet() {
        for (PostType type : PostType.values()) {
            if (rules.postCreationMinRole.get(type) == null) {
                throw new IllegalStateException(
                        "Falta configurar forum.rules.post-creation-min-role para el tipo " + type);
            }
            if (rules.commentOrder.get(type) == null) {
                throw new IllegalStateException(
                        "Falta configurar forum.rules.comment-order para el tipo " + type);
            }
        }
        requireRole(rules.commentMinRole, "comment-min-role");
        requireRole(rules.postVoteMinRole, "post-vote-min-role");
        requireRole(rules.commentVoteMinRole, "comment-vote-min-role");
        requireRole(rules.followMinRole, "follow-min-role");
        requireRole(rules.followTargetMinRole, "follow-target-min-role");
        requireRole(rules.moderationMinRole, "moderation-min-role");
        requireRole(rules.businessProfileMinRole, "business-profile-min-role");
        if (images.allowedTypes == null || images.allowedTypes.isEmpty()) {
            throw new IllegalStateException("Falta configurar forum.images.allowed-types");
        }
        if (storage.local.basePath == null || storage.local.basePath.isBlank()) {
            throw new IllegalStateException("Falta configurar forum.storage.local.base-path");
        }
    }

    private static void requireRole(Role role, String property) {
        if (role == null) {
            throw new IllegalStateException("Falta configurar forum.rules." + property);
        }
    }

    // --- Getters/setters para el binding de @ConfigurationProperties ---

    public Rules getRules() {
        return rules;
    }

    public void setRules(Rules rules) {
        this.rules = rules;
    }

    public Images getImages() {
        return images;
    }

    public void setImages(Images images) {
        this.images = images;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public static class Rules {
        private Map<PostType, Role> postCreationMinRole = new HashMap<>();
        private Map<PostType, CommentOrder> commentOrder = new HashMap<>();
        private Role commentMinRole;
        private Role postVoteMinRole;
        private Role commentVoteMinRole;
        private boolean allowSelfVote;
        private Role followMinRole;
        private Role followTargetMinRole;
        private Role moderationMinRole;
        private Role businessProfileMinRole;

        public Map<PostType, Role> getPostCreationMinRole() {
            return postCreationMinRole;
        }

        public void setPostCreationMinRole(Map<PostType, Role> postCreationMinRole) {
            this.postCreationMinRole = postCreationMinRole;
        }

        public Map<PostType, CommentOrder> getCommentOrder() {
            return commentOrder;
        }

        public void setCommentOrder(Map<PostType, CommentOrder> commentOrder) {
            this.commentOrder = commentOrder;
        }

        public Role getCommentMinRole() {
            return commentMinRole;
        }

        public void setCommentMinRole(Role commentMinRole) {
            this.commentMinRole = commentMinRole;
        }

        public Role getPostVoteMinRole() {
            return postVoteMinRole;
        }

        public void setPostVoteMinRole(Role postVoteMinRole) {
            this.postVoteMinRole = postVoteMinRole;
        }

        public Role getCommentVoteMinRole() {
            return commentVoteMinRole;
        }

        public void setCommentVoteMinRole(Role commentVoteMinRole) {
            this.commentVoteMinRole = commentVoteMinRole;
        }

        public boolean isAllowSelfVote() {
            return allowSelfVote;
        }

        public void setAllowSelfVote(boolean allowSelfVote) {
            this.allowSelfVote = allowSelfVote;
        }

        public Role getFollowMinRole() {
            return followMinRole;
        }

        public void setFollowMinRole(Role followMinRole) {
            this.followMinRole = followMinRole;
        }

        public Role getFollowTargetMinRole() {
            return followTargetMinRole;
        }

        public void setFollowTargetMinRole(Role followTargetMinRole) {
            this.followTargetMinRole = followTargetMinRole;
        }

        public Role getModerationMinRole() {
            return moderationMinRole;
        }

        public void setModerationMinRole(Role moderationMinRole) {
            this.moderationMinRole = moderationMinRole;
        }

        public Role getBusinessProfileMinRole() {
            return businessProfileMinRole;
        }

        public void setBusinessProfileMinRole(Role businessProfileMinRole) {
            this.businessProfileMinRole = businessProfileMinRole;
        }
    }

    public static class Images {
        private int maxPerPost;
        private DataSize maxFileSize;
        private List<String> allowedTypes;

        public int getMaxPerPost() {
            return maxPerPost;
        }

        public void setMaxPerPost(int maxPerPost) {
            this.maxPerPost = maxPerPost;
        }

        public DataSize getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(DataSize maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        public List<String> getAllowedTypes() {
            return allowedTypes;
        }

        public void setAllowedTypes(List<String> allowedTypes) {
            this.allowedTypes = allowedTypes;
        }
    }

    public static class Storage {
        private Local local = new Local();

        public Local getLocal() {
            return local;
        }

        public void setLocal(Local local) {
            this.local = local;
        }

        public static class Local {
            private String basePath;

            public String getBasePath() {
                return basePath;
            }

            public void setBasePath(String basePath) {
                this.basePath = basePath;
            }
        }
    }
}
