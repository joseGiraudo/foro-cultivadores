package com.forum.users;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.forum.users.UserImageService.ServedImage;

@RestController
class UserImageController {

    private final UserImageService userImageService;

    UserImageController(UserImageService userImageService) {
        this.userImageService = userImageService;
    }

    @PostMapping("/users/me/avatar")
    UserResponse uploadAvatar(@AuthenticationPrincipal AuthUser principal,
                              @RequestParam("file") MultipartFile file) {
        return userImageService.uploadAvatar(principal, file);
    }

    @DeleteMapping("/users/me/avatar")
    UserResponse deleteAvatar(@AuthenticationPrincipal AuthUser principal) {
        return userImageService.deleteAvatar(principal);
    }

    @PostMapping("/users/me/cover")
    UserResponse uploadCover(@AuthenticationPrincipal AuthUser principal,
                             @RequestParam("file") MultipartFile file) {
        return userImageService.uploadCover(principal, file);
    }

    @DeleteMapping("/users/me/cover")
    UserResponse deleteCover(@AuthenticationPrincipal AuthUser principal) {
        return userImageService.deleteCover(principal);
    }

    @GetMapping("/users/{id}/avatar")
    ResponseEntity<Resource> avatar(@PathVariable Long id) {
        ServedImage served = userImageService.serveAvatar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(served.contentType()))
                .body(served.resource());
    }

    @GetMapping("/users/{id}/cover")
    ResponseEntity<Resource> cover(@PathVariable Long id) {
        ServedImage served = userImageService.serveCover(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(served.contentType()))
                .body(served.resource());
    }
}
