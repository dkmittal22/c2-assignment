package com.c2assignment.ticket;

import com.c2assignment.ticket.dto.CreateTicketRequest;
import com.c2assignment.ticket.dto.PatchTicketRequest;
import com.c2assignment.ticket.dto.TicketDetailResponse;
import com.c2assignment.ticket.dto.TicketPageResponse;
import com.c2assignment.ticket.dto.TicketResponse;
import com.c2assignment.ticket.dto.TransitionRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Ticket HTTP API (spec/api-contract.md §5). HTTP concerns only; all business
 * rules live in the service layer.
 */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse created = ticketService.create(request);
        URI location = UriComponentsBuilder
                .fromPath("/api/v1/tickets/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public TicketDetailResponse get(@PathVariable UUID id) {
        return ticketService.getDetail(id);
    }

    @GetMapping
    public TicketPageResponse list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ticketService.list(search, status, page, size);
    }

    @PatchMapping("/{id}")
    public TicketResponse patch(@PathVariable UUID id, @Valid @RequestBody PatchTicketRequest request) {
        return ticketService.patch(id, request);
    }

    @PostMapping("/{id}/transitions")
    public TicketResponse transition(@PathVariable UUID id, @Valid @RequestBody TransitionRequest request) {
        return ticketService.transition(id, request.toStatus());
    }
}