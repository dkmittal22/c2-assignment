// Shared constants. These mirror backend enums for UX only; the backend is
// the source of truth for every rule (architecture §7).

export const TICKET_STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED'];

export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

// Status transitions offered in the UI (state-machine.md). UX-only: the
// backend validates every transition request.
export const VALID_TRANSITIONS = {
  OPEN: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['RESOLVED', 'CANCELLED'],
  RESOLVED: ['CLOSED'],
  CLOSED: [],
  CANCELLED: [],
};

export function formatInstant(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}