package com.c2assignment.comment;

import com.c2assignment.common.TicketNotFoundException;
import com.c2assignment.comment.dto.CommentCreateRequest;
import com.c2assignment.comment.dto.CommentResponse;
import com.c2assignment.ticket.Ticket;
import com.c2assignment.ticket.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Comment business rules (spec FR-5, api-contract §5.5, data-model §5-6).
 */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;

    public CommentService(CommentRepository commentRepository, TicketRepository ticketRepository) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Adds a comment to an existing ticket. Allowed on any status, including
     * CLOSED and CANCELLED. Refreshes the parent ticket's updatedAt.
     */
    @Transactional
    public CommentResponse add(UUID ticketId, CommentCreateRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        Comment comment = new Comment(
                UUID.randomUUID(),
                ticketId,
                request.author().trim(),
                request.content().trim(),
                Instant.now()
        );
        commentRepository.save(comment);

        ticket.setUpdatedAt(Instant.now());
        ticketRepository.save(ticket);

        return CommentMapper.toResponse(comment);
    }
}