// API client for the Support Ticket backend (spec/architecture.md §7).
// Targets the Spring Boot API; the backend remains the source of truth for
// every business rule (state machine, validation, filters).

const API_BASE =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

export class ApiError extends Error {
  constructor(status, code, message, fieldErrors, body) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.body = body;
  }
}

async function request(path, { method = 'GET', body } = {}) {
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the ticket API', null, null);
  }

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new ApiError(
      response.status,
      data?.error ?? 'ERROR',
      data?.message ?? `Request failed with status ${response.status}`,
      data?.fieldErrors ?? null,
      data
    );
  }

  return data;
}

function withQuery(params) {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value));
    }
  }
  const qs = query.toString();
  return qs ? `?${qs}` : '';
}

export function listTickets({ search, status, page, size } = {}) {
  return request(`/tickets${withQuery({ search, status, page, size })}`);
}

export function getTicket(id) {
  return request(`/tickets/${encodeURIComponent(id)}`);
}

export function createTicket(payload) {
  return request('/tickets', { method: 'POST', body: payload });
}

export function patchTicket(id, payload) {
  return request(`/tickets/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    body: payload,
  });
}

export function transitionTicket(id, toStatus) {
  return request(`/tickets/${encodeURIComponent(id)}/transitions`, {
    method: 'POST',
    body: { toStatus },
  });
}

export function addComment(id, payload) {
  return request(`/tickets/${encodeURIComponent(id)}/comments`, {
    method: 'POST',
    body: payload,
  });
}