package com.forum.contributorrequests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta de una petición para ser CONTRIBUTOR. */
public record CreateContributorRequestRequest(
        @NotBlank @Size(min = 20, max = 1000) String message,
        @NotBlank @Size(max = 200) String contact
) {
}
