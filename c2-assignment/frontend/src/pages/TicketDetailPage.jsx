import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getTicket, patchTicket, transitionTicket, addComment } from '../api/client.js';
import { VALID_TRANSITIONS, formatInstant } from '../constants.js';
import { ErrorBanner, FieldErrors } from '../components/ErrorDisplay.jsx';
import { TicketForm } from '../components/TicketForm.jsx';

export function TicketDetailPage() {
  const { id } = useParams();
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [transitionError, setTransitionError] = useState(null);
  const [transitioning, setTransitioning] = useState(false);
  const [commentError, setCommentError] = useState(null);
  const [editing, setEditing] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setTicket(await getTicket(id));
      setLoadError(null);
    } catch (err) {
      if (err instanceof ApiError && (err.status === 404 || err.status === 400)) {
        setLoadError({ status: err.status, message: err.message });
      } else {
        setLoadError({ status: null, message: 'Failed to load this ticket. Is the API running?' });
      }
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) return <p className="muted">Loading…</p>;

  if (loadError) {
    return (
      <section className="page">
        <h1>Ticket</h1>
        <ErrorBanner error={loadError.message} />
        <p className="muted">
          {loadError.status === 404 ? 'This ticket does not exist.' : 'The request could not be processed.'}
        </p>
        <Link className="button" to="/tickets">
          Back to Tickets
        </Link>
      </section>
    );
  }

  const targets = VALID_TRANSITIONS[ticket.status] ?? [];

  async function handlePatch(payload) {
    const changed = {};
    if (payload.title !== ticket.title) changed.title = payload.title;
    if (payload.description !== ticket.description) changed.description = payload.description;
    if (payload.priority !== ticket.priority) changed.priority = payload.priority;
    if ((payload.assignee || null) !== ticket.assignee) changed.assignee = payload.assignee || null;

    if (Object.keys(changed).length === 0) {
      setEditing(false);
      return;
    }

    // PATCH/transition responses omit `comments` (api-contract §4); keep the
    // comments already loaded from the GET detail response.
    const updated = await patchTicket(id, changed);
    setTicket((current) => ({ ...updated, comments: current.comments }));
    setEditing(false);
  }

  async function handleTransition(toStatus) {
    setTransitioning(true);
    setTransitionError(null);
    try {
      const updated = await transitionTicket(id, toStatus);
      setTicket((current) => ({ ...updated, comments: current.comments }));
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setTransitionError(`Conflict: ${err.message} — refreshing.`);
        await load();
      } else {
        setTransitionError(err instanceof ApiError ? err.message : 'Transition failed');
      }
    } finally {
      setTransitioning(false);
    }
  }

  async function handleComment({ author, content }) {
    const created = await addComment(id, { author, content });
    setTicket((current) => ({ ...current, comments: [...current.comments, created] }));
  }

  return (
    <section className="page detail-page">
      <Link className="button" to="/tickets">
        ← Back to Tickets
      </Link>

      <ErrorBanner error={transitionError} onDismiss={() => setTransitionError(null)} />

      <h1>{ticket.title}</h1>
      <p className="muted">
        ID {ticket.id} · Created {formatInstant(ticket.createdAt)} · Updated {formatInstant(ticket.updatedAt)}
      </p>

      {editing ? (
        <TicketForm
          initial={{
            title: ticket.title,
            description: ticket.description,
            priority: ticket.priority,
            assignee: ticket.assignee ?? '',
          }}
          statusLabel={ticket.status}
          submitLabel="Save Changes"
          onSubmit={handlePatch}
          extraActions={
            <button type="button" className="button" onClick={() => setEditing(false)}>
              Cancel Edit
            </button>
          }
        />
      ) : (
        <div className="card ticket-view">
          <p>
            <strong>Description:</strong> {ticket.description}
          </p>
          <p>
            <strong>Priority:</strong> {ticket.priority}
          </p>
          <p>
            <strong>Assignee:</strong> {ticket.assignee || '—'}
          </p>
          <p>
            <strong>Status:</strong>{' '}
            <span className={`badge badge-${ticket.status.toLowerCase().replace('_', '-')}`}>
              {ticket.status}
            </span>
          </p>
          <button className="button" type="button" onClick={() => setEditing(true)}>
            Edit
          </button>
        </div>
      )}

      <section className="card">
        <h2>Status</h2>
        {targets.length === 0 ? (
          <p className="muted">This ticket is {ticket.status.toLowerCase()} and cannot be transitioned.</p>
        ) : (
          <p className="muted">Move to:</p>
        )}
        {targets.map((target) => (
          <button
            key={target}
            className="button"
            type="button"
            disabled={transitioning}
            onClick={() => handleTransition(target)}
          >
            {target}
          </button>
        ))}
      </section>

      <section className="card">
        <h2>Comments</h2>
        {ticket.comments.length === 0 ? (
          <p className="muted">No comments yet.</p>
        ) : (
          <ul className="comment-list">
            {ticket.comments.map((comment) => (
              <li key={comment.id} className="comment">
                <div className="comment-meta">
                  <strong>{comment.author}</strong>
                  <span className="muted">{formatInstant(comment.createdAt)}</span>
                </div>
                <div className="comment-content">{comment.content}</div>
              </li>
            ))}
          </ul>
        )}
        <CommentForm onSubmit={handleComment} onError={setCommentError} />
        <ErrorBanner error={commentError} onDismiss={() => setCommentError(null)} />
      </section>
    </section>
  );
}

function CommentForm({ onSubmit, onError }) {
  const [author, setAuthor] = useState('');
  const [content, setContent] = useState('');
  const [fieldErrors, setFieldErrors] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    setFieldErrors(null);
    onError(null);
    try {
      await onSubmit({ author: author.trim(), content });
      setAuthor('');
      setContent('');
    } catch (err) {
      if (err instanceof ApiError) {
        setFieldErrors(err.fieldErrors);
        onError(err.message);
      } else {
        onError('Failed to add comment');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit} noValidate>
      <div className="form-row">
        <label htmlFor="comment-author">Author</label>
        <div>
          <input
            id="comment-author"
            value={author}
            onChange={(e) => setAuthor(e.target.value)}
            maxLength={100}
          />
          <FieldErrors fieldErrors={fieldErrors} field="author" />
        </div>
      </div>
      <div className="form-row">
        <label htmlFor="comment-content">Content</label>
        <div>
          <textarea
            id="comment-content"
            rows={3}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            maxLength={2000}
          />
          <FieldErrors fieldErrors={fieldErrors} field="content" />
        </div>
      </div>
      <div className="form-actions">
        <button className="button primary" type="submit" disabled={submitting}>
          {submitting ? 'Adding…' : 'Add Comment'}
        </button>
      </div>
    </form>
  );
}