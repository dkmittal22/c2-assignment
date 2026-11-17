import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { TicketListPage } from './pages/TicketListPage.jsx';
import { TicketCreatePage } from './pages/TicketCreatePage.jsx';
import { TicketDetailPage } from './pages/TicketDetailPage.jsx';

export default function App() {
  return (
    <div className="app">
      <header className="app-header">
        <Link to="/tickets" className="app-title">
          c2-assignment
        </Link>
        <nav className="app-nav">
          <Link to="/tickets">Tickets</Link>
          <Link to="/tickets/new">New Ticket</Link>
        </nav>
      </header>
      <main className="app-main">
        <Routes>
          <Route path="/" element={<Navigate to="/tickets" replace />} />
          <Route path="/tickets" element={<TicketListPage />} />
          <Route path="/tickets/new" element={<TicketCreatePage />} />
          <Route path="/tickets/:id" element={<TicketDetailPage />} />
          <Route path="*" element={<Navigate to="/tickets" replace />} />
        </Routes>
      </main>
    </div>
  );
}