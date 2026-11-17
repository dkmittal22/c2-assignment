// Shared error display: surfaces API `message` and `fieldErrors`
// (spec/architecture.md §7 error display).

export function ErrorBanner({ error, onDismiss }) {
  if (!error) return null;
  return (
    <div className="error-banner" role="alert">
      <span>{error}</span>
      {onDismiss && (
        <button type="button" className="error-dismiss" onClick={onDismiss}>
          ×
        </button>
      )}
    </div>
  );
}

export function FieldErrors({ fieldErrors, field }) {
  if (!fieldErrors || !fieldErrors[field]) return null;
  return <p className="field-error">{fieldErrors[field]}</p>;
}