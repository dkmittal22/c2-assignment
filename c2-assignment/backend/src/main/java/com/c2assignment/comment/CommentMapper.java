package com.c2assignment.comment;

import com.c2assignment.comment.dto.CommentResponse;

/**
 * Maps Comment entities to API DTOs.
 */
public final class CommentMapper {

    private CommentMapper() {
    }

    public static CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicketId(),
                comment.getAuthor(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}