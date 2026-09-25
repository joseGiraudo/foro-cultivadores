package com.forum.images;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.forum.images.ImageService.ServedImage;
import com.forum.users.AuthUser;

@RestController
class ImageController {

    private final ImageService imageService;

    ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/images/{imageId}")
    ResponseEntity<Resource> serve(@AuthenticationPrincipal AuthUser principal, @PathVariable Long imageId) {
        ServedImage served = imageService.serve(imageId, principal);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(served.contentType()))
                .body(served.resource());
    }
}
