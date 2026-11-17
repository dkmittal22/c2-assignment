package com.c2assignment.ticket;

import com.c2assignment.common.BadRequestException;
import com.c2assignment.common.FieldValidationException;
import com.c2assignment.common.InvalidTransitionException;
import com.c2assignment.common.TicketNotFoundException;
import com.c2assignment.comment.CommentRepository;
import com.c2assignment.ticket.dto.CreateTicketRequest;
import com.c2assignment.ticket.dto.PatchTicketRequest;
import com.c2assignment.ticket.dto.TicketDetailResponse;
import com.c2assignment.ticket.dto.TicketPageResponse;
import com.c2assignment.ticket.dto.TicketResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.c2assignment.ticket.TicketMapper.toDetailResponse;
import static com.c2assignment.ticket.TicketMapper.toResponse;

/**
 * Ticket business rules. Controllers delegate here; the state machine and
 * validation rules live in the service/domain layer (spec/architecture.md §4.2).
 */
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;

    public TicketService(TicketRepository ticketRepository, CommentRepository commentRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * Creates a ticket. Status is always OPEN; priority defaults to MEDIUM;
     * fields are trimmed; whitespace-only assignee is stored as null.
     */
    @Transactional
    public TicketResponse create(CreateTicketRequest request) {
        Instant now = Instant.now();
        Ticket ticket = new Ticket(
                UUID.randomUUID(),
                request.title().trim(),
                request.description().trim(),
                request.priority() == null ? Priority.MEDIUM : request.priority(),
                TicketStatus.OPEN,
                normalizeOptionalAssignee(request.assignee()),
                now,
                now
        );
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getDetail(UUID id) {
        Ticket ticket = findTicket(id);
        return toDetailResponse(ticket, commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(id)
                .stream()
                .map(com.c2assignment.comment.CommentMapper::toResponse)
                .toList());
    }

    /**
     * Paginated list with optional status filter and case-insensitive substring
     * search over title/description. search and status combine with AND.
     */
    @Transactional(readOnly = true)
    public TicketPageResponse list(String search, TicketStatus status, int page, int size) {
        if (page < 0) {
            throw new BadRequestException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("size must be between 1 and 100");
        }

        Specification<Ticket> spec = Specification.where(statusSpec(status)).and(searchSpec(search));
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Ticket> result = ticketRepository.findAll(spec, pageable);
        return new TicketPageResponse(
                result.getContent().stream().map(TicketMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    /**
     * Partial update of title/description/priority/assignee only. Never changes
     * status. Empty patches are rejected as BAD_REQUEST; fields that are present
     * but blank are rejected as VALIDATION_ERROR with per-field messages.
     */
    @Transactional
    public TicketResponse patch(UUID id, PatchTicketRequest request) {
        Ticket ticket = findTicket(id);

        if (request.title() == null && request.description() == null
                && request.priority() == null && request.assignee() == null) {
            throw new BadRequestException(
                    "PATCH body must contain at least one of: title, description, priority, assignee");
        }

        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw blankField("title", "Title must not be blank");
            }
            ticket.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            if (request.description().isBlank()) {
                throw blankField("description", "Description must not be blank");
            }
            ticket.setDescription(request.description().trim());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.assignee() != null) {
            if (request.assignee().isBlank()) {
                throw blankField("assignee", "Assignee must not be blank");
            }
            ticket.setAssignee(normalizeOptionalAssignee(request.assignee()));
        }

        ticket.setUpdatedAt(Instant.now());
        return toResponse(ticketRepository.save(ticket));
    }

    /**
     * Applies a status transition. Enforces the state machine from
     * spec/state-machine.md; invalid transitions never reach persistence.
     */
    @Transactional
    public TicketResponse transition(UUID id, TicketStatus toStatus) {
        Ticket ticket = findTicket(id);
        if (!TicketStatusTransitionPolicy.isAllowed(ticket.getStatus(), toStatus)) {
            throw new InvalidTransitionException(ticket.getStatus(), toStatus);
        }
        ticket.setStatus(toStatus);
        ticket.setUpdatedAt(Instant.now());
        return toResponse(ticketRepository.save(ticket));
    }

    private Ticket findTicket(UUID id) {
        return ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    private FieldValidationException blankField(String field, String message) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(field, message);
        return new FieldValidationException(fieldErrors);
    }

    private String normalizeOptionalAssignee(String assignee) {
        if (assignee == null) {
            return null;
        }
        String trimmed = assignee.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Specification<Ticket> statusSpec(TicketStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    private Specification<Ticket> searchSpec(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String term = search.trim();
        return (root, query, builder) -> {
            String escaped = term
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            String pattern = "%" + escaped + "%";
            Predicate titleMatch = builder.like(builder.lower(root.get("title")), pattern.toLowerCase(), '\\');
            Predicate descriptionMatch = builder.like(builder.lower(root.get("description")), pattern.toLowerCase(), '\\');
            return builder.or(titleMatch, descriptionMatch);
        };
    }
}