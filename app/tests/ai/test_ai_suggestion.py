"""Unit tests for AiSuggestion aggregate."""
import pytest

from src.ai.domain.model import AiSuggestion, ConfidenceScore, SuggestionStatus


def make_suggestion(**kwargs) -> AiSuggestion:
    defaults = dict(
        ticket_id="ticket-1",
        draft_response="Thank you for reaching out. We are investigating the issue.",
        confidence=ConfidenceScore(0.82),
        source_article_ids=["article-a", "article-b"],
    )
    return AiSuggestion.create(**{**defaults, **kwargs})


class TestAiSuggestionLifecycle:
    def test_creates_with_pending_status(self):
        s = make_suggestion()
        assert s.status == SuggestionStatus.PENDING

    def test_accept_transitions_status(self):
        s = make_suggestion()
        s.accept()
        assert s.status == SuggestionStatus.ACCEPTED

    def test_reject_transitions_status(self):
        s = make_suggestion()
        s.reject()
        assert s.status == SuggestionStatus.REJECTED

    def test_edit_and_accept_stores_edited_response(self):
        s = make_suggestion()
        s.edit_and_accept("My improved version")
        assert s.status == SuggestionStatus.EDITED
        assert s.edited_response == "My improved version"

    def test_accept_updates_timestamp(self):
        s = make_suggestion()
        original = s.updated_at
        s.accept()
        assert s.updated_at >= original
