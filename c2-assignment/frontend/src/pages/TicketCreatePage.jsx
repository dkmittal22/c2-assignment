import { useNavigate } from 'react-router-dom';
import { createTicket } from '../api/client.js';
import { TicketForm } from '../components/TicketForm.jsx';

export function TicketCreatePage() {
  const navigate = useNavigate();

  async function handleSubmit(payload) {
    const created = await createTicket(payload);
    navigate(`/tickets/${created.id}`, { replace: true });
  }

  return (
    <section className="page">
      <h1>New Ticket</h1>
      <TicketForm submitLabel="Create Ticket" onSubmit={handleSubmit} />
    </section>
  );
}