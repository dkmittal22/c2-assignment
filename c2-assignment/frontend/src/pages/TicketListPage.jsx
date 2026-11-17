import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listTickets, ApiError } from '../api/client.js';
import { TICKET_STATUSES, formatInstant } from '../constants.js';
import { ErrorBanner } from '../components/ErrorDisplay.jsx';

const PAGE_SIZE = 20;

export function TicketListPage() {
  const [query, setQuery] = useState('');
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async (nextSearch, nextStatus, nextPage) => {
    setLoading(true);
    setError(null);
    try {
      const result = await listTickets({
        search: nextSearch || undefined,
        status: nextStatus || undefined,
        page: nextPage,
        size: PAGE_SIZE,
      });
      setData(result);
      setSearch(nextSearch);
      setStatus(nextStatus);
      setPage(nextPage);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load tickets');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load('', '', 0);
  }, [load]);

  function submitSearch(event) {
    event.preventDefault();
    load(query.trim(), status, 0);
  }

  function changeStatus(nextStatus) {
    load(search, nextStatus, 0);
  }

  function changePage(nextPage) {
    load(search, status, nextPage);
  }

  const totalPages = data?.totalPages ?? 0;

  return (
    <section className="page list-page">
      <div className="page-head">
        <h1>Tickets</h1>
        <Link className="button primary" to="/tickets/new">
          New Ticket
        </Link>
      </div>

      <ErrorBanner error={error} onDismiss={() => setError(null)} />

      <form className="filters" onSubmit={submitSearch}>
        <input
          type="search"
          aria-label="Search tickets"
          placeholder="Search title or description"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button className="button" type="submit">
          Search
        </button>
        <select
          aria-label="Filter by status"
          value={status}
          onChange={(e) => changeStatus(e.target.value)}
        >
          <option value="">ALL</option>
          {TICKET_STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </form>

      {loading && <p className="muted">Loading…</p>}

      {!loading && data && data.content.length === 0 && (
        <p className="muted empty-state">No tickets found.</p>
      )}

      {!loading && data && data.content.length > 0 && (
        <>
          <table className="table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Assignee</th>
                <th>Created</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.content.map((ticket) => (
                <tr key={ticket.id}>
                  <td className="title-cell">
                    <Link to={`/tickets/${ticket.id}`}>{ticket.title}</Link>
                  </td>
                  <td>{ticket.priority}</td>
                  <td>
                    <span className={`badge badge-${ticket.status.toLowerCase().replace('_', '-')}`}>
                      {ticket.status}
                    </span>
                  </td>
                  <td>{ticket.assignee || '—'}</td>
                  <td>{formatInstant(ticket.createdAt)}</td>
                  <td>
                    <Link className="button" to={`/tickets/${ticket.id}`}>
                      View
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="pagination">
            <button
              className="button"
              type="button"
              disabled={page <= 0}
              onClick={() => changePage(page - 1)}
            >
              Previous
            </button>
            <span className="muted">
              Page {data.page + 1} of {Math.max(totalPages, 1)}
            </span>
            <button
              className="button"
              type="button"
              disabled={page >= totalPages - 1}
              onClick={() => changePage(page + 1)}
            >
              Next
            </button>
          </div>
        </>
      )}
    </section>
  );
}