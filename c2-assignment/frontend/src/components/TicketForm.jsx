import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError } from '../api/client.js';
import { PRIORITIES } from '../constants.js';
import { ErrorBanner, FieldErrors } from './ErrorDisplay.jsx';

// Shared create/edit ticket form. Never submits `status` — status only changes
// through the transitions endpoint (architecture §9). The backend is the
// source of truth; client checks are UX convenience only.
export function TicketForm({
  initial = {},
  statusLabel,
  submitLabel,
  onSubmit,
  extraActions,
}) {
  const [title, setTitle] = useState(initial.title ?? '');
  const [description, setDescription] = useState(initial.description ?? '');
  const [priority, setPriority] = useState(initial.priority ?? 'MEDIUM');
  const [assignee, setAssignee] = useState(initial.assignee ?? '');
  const [fieldErrors, setFieldErrors] = useState(null);
  const [globalError, setGlobalError] = useState(null);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setGlobalError(null);
    setFieldErrors(null);
    const payload = { title: title.trim(), description: description.trim(), priority };
    if (assignee.trim()) payload.assignee = assignee.trim();

    try {
      await onSubmit(payload);
    } catch (err) {
      if (err instanceof ApiError) {
        setFieldErrors(err.fieldErrors);
        setGlobalError(err.message);
      } else {
        setGlobalError('Failed to save ticket');
      }
      setSaving(false);
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit} noValidate>
      {statusLabel != null && (
        <div className="form-row static-row">
          <label>Status</label>
          <div>
            <span className={`badge badge-${statusLabel.toLowerCase().replace('_', '-')}`}>
              {statusLabel}
            </span>
            <p className="form-note">Status cannot be edited here; use the transitions below.</p>
          </div>
        </div>
      )}

      <ErrorBanner error={globalError} onDismiss={() => setGlobalError(null)} />

      <div className="form-row">
        <label htmlFor="title">Title</label>
        <div>
          <input
            id="title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={200}
          />
          <FieldErrors fieldErrors={fieldErrors} field="title" />
        </div>
      </div>

      <div className="form-row">
        <label htmlFor="description">Description</label>
        <div>
          <textarea
            id="description"
            rows={6}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            maxLength={5000}
          />
          <FieldErrors fieldErrors={fieldErrors} field="description" />
        </div>
      </div>

      <div className="form-row">
        <label htmlFor="priority">Priority</label>
        <div>
          <select id="priority" value={priority} onChange={(e) => setPriority(e.target.value)}>
            {PRIORITIES.map((p) => (
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </select>
          <FieldErrors fieldErrors={fieldErrors} field="priority" />
        </div>
      </div>

      <div className="form-row">
        <label htmlFor="assignee">Assignee</label>
        <div>
          <input
            id="assignee"
            value={assignee}
            onChange={(e) => setAssignee(e.target.value)}
            maxLength={100}
          />
          <FieldErrors fieldErrors={fieldErrors} field="assignee" />
        </div>
      </div>

      <div className="form-actions">
        <button className="button primary" type="submit" disabled={saving}>
          {saving ? 'Saving…' : submitLabel}
        </button>
        <Link className="button" to="/tickets">
          Cancel
        </Link>
        {extraActions}
      </div>
    </form>
  );
}