package com.c2assignment.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Add-comment request (spec/api-contract.md §5.5). Allowed fields only: author, content.
 */
public record CommentCreateRequest(
        @NotBlank(message = "Author is required")
        @Size(max = 100, message = "Author must not exceed 100 characters")
        String author,

        @NotBlank(message = "Content is required")
        @Size(max = 2000, message = "Content must not exceed 2000 characters")
        String content
) {
}