package com.forum.posts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.shared.error.ApiException;
import com.forum.users.AuthUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/posts")
class PostController {

    private final PostService postService;

    PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    PostResponse create(@AuthenticationPrincipal AuthUser principal, @Valid @RequestBody CreatePostRequest request) {
        return postService.create(principal, request);
    }

    // Variante multipart: crea el post y sube sus imágenes en una sola request/transacción (consultas
    // con fotos). La metadata viaja como parte JSON "post"; las imágenes como partes "images".
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    PostResponse createWithImages(@AuthenticationPrincipal AuthUser principal,
                                  @Valid @RequestPart("post") CreatePostRequest request,
                                  @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return postService.create(principal, request, toBytes(images));
    }

    private static List<byte[]> toBytes(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<byte[]> bytes = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                bytes.add(file.getBytes());
            } catch (IOException e) {
                throw ApiException.badRequest("No se pudo leer una de las imágenes");
            }
        }
        return bytes;
    }

    @GetMapping
    Page<PostSummaryResponse> list(@AuthenticationPrincipal AuthUser principal,
                                   @RequestParam(required = false) List<Long> categoryIds,
                                   @RequestParam(required = false) Long authorId,
                                   @RequestParam(required = false) PostType postType,
                                   @RequestParam(required = false) ContentStatus status,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(required = false, defaultValue = "false") boolean following,
                                   @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return postService.list(categoryIds, authorId, postType, status, q, following, principal, pageable);
    }

    // Tipos que el actor puede crear (según su rol). La UI arma el combo de tipo con esto.
    // Ruta literal: tiene precedencia sobre /posts/{id}, así que no colisiona.
    @GetMapping("/creatable-types")
    List<PostType> creatableTypes(@AuthenticationPrincipal AuthUser principal) {
        return postService.creatableTypes(principal);
    }

    @GetMapping("/{id}")
    PostResponse get(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id) {
        return postService.get(id, principal);
    }

    @PatchMapping("/{id}")
    PostResponse update(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id,
                        @Valid @RequestBody UpdatePostRequest request) {
        return postService.update(id, principal, request);
    }

    @PatchMapping("/{id}/status")
    PostResponse changeStatus(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id,
                              @Valid @RequestBody ChangePostStatusRequest request) {
        return postService.changeStatus(id, principal, request.status());
    }
}
