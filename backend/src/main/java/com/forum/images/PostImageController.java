package com.forum.images;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.forum.posts.ImageResponse;
import com.forum.users.AuthUser;

@RestController
class PostImageController {

    private final ImageService imageService;

    PostImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping(path = "/posts/{postId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ImageResponse upload(@AuthenticationPrincipal AuthUser principal, @PathVariable Long postId,
                         @RequestParam("file") MultipartFile file) {
        return imageService.upload(postId, principal, file);
    }

    @DeleteMapping("/posts/{postId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthUser principal, @PathVariable Long postId, @PathVariable Long imageId) {
        imageService.delete(postId, imageId, principal);
    }
}
