package com.c2assignment.comment.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Comment representation (spec/api-contract.md §4).
 */
public record CommentResponse(
        UUID id,
        UUID ticketId,
        String author,
        String content,
        Instant createdAt
) {
}