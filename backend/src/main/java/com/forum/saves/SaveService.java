package com.forum.saves;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forum.posts.Post;
import com.forum.posts.PostService;
import com.forum.posts.PostSummaryResponse;
import com.forum.shared.ContentStatus;
import com.forum.shared.error.ApiException;
import com.forum.users.AuthUser;

@Service
class SaveService {

    private final SavedPostRepository savedPostRepository;
    private final PostService postService;

    SaveService(SavedPostRepository savedPostRepository, PostService postService) {
        this.savedPostRepository = savedPostRepository;
        this.postService = postService;
    }

    /** Idempotente: si ya lo tenía guardado, no hace nada. */
    @Transactional
    public void save(Long postId, AuthUser principal) {
        principal.ensureCanWrite();
        Post post = postService.loadVisible(postId, principal); // 404 si no existe o no es visible
        if (post.getStatus() != ContentStatus.ACTIVE) {
            throw ApiException.conflict("No se puede guardar un post que no está activo");
        }
        if (!savedPostRepository.existsByUserIdAndPostId(principal.getId(), postId)) {
            savedPostRepository.save(new SavedPost(principal.getId(), postId));
        }
    }

    /** Idempotente: no falla aunque no lo tuviera guardado. */
    @Transactional
    public void unsave(Long postId, AuthUser principal) {
        principal.ensureCanWrite();
        savedPostRepository.deleteByUserIdAndPostId(principal.getId(), postId);
    }

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> listSaved(AuthUser principal, Pageable pageable) {
        Page<SavedPost> page = savedPostRepository.findByUserId(principal.getId(), pageable);
        List<Long> postIds = page.getContent().stream().map(SavedPost::getPostId).toList();
        List<PostSummaryResponse> summaries = postService.summariesForIds(postIds, principal);
        return new PageImpl<>(summaries, pageable, page.getTotalElements());
    }
}
