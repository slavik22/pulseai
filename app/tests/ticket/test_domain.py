"""
Unit tests for the Ticket aggregate — pure domain logic, no I/O.
"""
import pytest

from src.ticket.domain.model import Priority, Ticket, TicketStatus
from src.shared.events import TicketCreatedEvent, TicketAssignedEvent, TicketResolvedEvent


def make_ticket(**kwargs) -> Ticket:
    defaults = dict(
        title="Login page crashes on mobile",
        content="When I tap the login button on my iPhone, the app closes.",
        priority=Priority.HIGH,
        customer_id="customer-42",
        category="bug",
    )
    return Ticket.open(**{**defaults, **kwargs})


class TestTicketCreation:
    def test_creates_with_open_status(self):
        ticket = make_ticket()
        assert ticket.status == TicketStatus.OPEN

    def test_emits_created_event(self):
        ticket = make_ticket()
        events = ticket.pull_domain_events()
        assert len(events) == 1
        assert isinstance(events[0], TicketCreatedEvent)
        assert events[0].priority == "HIGH"

    def test_events_cleared_after_pull(self):
        ticket = make_ticket()
        ticket.pull_domain_events()
        assert ticket.pull_domain_events() == []

    def test_rejects_blank_title(self):
        with pytest.raises(ValueError, match="title"):
            make_ticket(title="   ")

    def test_rejects_blank_customer_id(self):
        with pytest.raises(ValueError, match="customer_id"):
            make_ticket(customer_id="")


class TestTicketAssignment:
    def test_transitions_to_in_progress(self):
        ticket = make_ticket()
        ticket.pull_domain_events()
        ticket.assign_to("agent-1", "Alice")
        assert ticket.status == TicketStatus.IN_PROGRESS

    def test_emits_assigned_event(self):
        ticket = make_ticket()
        ticket.pull_domain_events()
        ticket.assign_to("agent-1", "Alice")
        events = ticket.pull_domain_events()
        assert len(events) == 1
        assert isinstance(events[0], TicketAssignedEvent)
        assert events[0].agent_name == "Alice"

    def test_cannot_assign_resolved_ticket(self):
        ticket = make_ticket()
        ticket.pull_domain_events()
        ticket.assign_to("agent-1", "Alice")
        ticket.pull_domain_events()
        ticket.resolve("Fixed in v1.2")
        with pytest.raises(ValueError, match="Invalid transition"):
            ticket.assign_to("agent-2", "Bob")


class TestTicketResolution:
    def test_transitions_to_resolved(self):
        ticket = make_ticket()
        ticket.pull_domain_events()
        ticket.assign_to("agent-1", "Alice")
        ticket.pull_domain_events()
        ticket.resolve("Fixed by clearing cache")
        assert ticket.status == TicketStatus.RESOLVED
        assert ticket.resolution_note == "Fixed by clearing cache"
        assert ticket.resolved_at is not None

    def test_emits_resolved_event(self):
        ticket = make_ticket()
        ticket.pull_domain_events()
        ticket.assign_to("agent-1", "Alice")
        ticket.pull_domain_events()
        ticket.resolve("Fixed by clearing cache")
        events = ticket.pull_domain_events()
        assert len(events) == 1
        assert isinstance(events[0], TicketResolvedEvent)

    def test_cannot_resolve_unassigned_ticket(self):
        ticket = make_ticket()
        with pytest.raises(ValueError, match="unassigned"):
            ticket.resolve("Some fix")
