"""Unit tests for ConfidenceScore value object."""
import pytest

from src.ai.domain.model import ConfidenceScore


class TestConfidenceScore:
    def test_high_confidence_is_reliable(self):
        assert ConfidenceScore(0.80).is_reliable is True

    def test_low_confidence_is_not_reliable(self):
        assert ConfidenceScore(0.50).is_reliable is False

    def test_boundary_is_reliable(self):
        assert ConfidenceScore(0.65).is_reliable is True

    def test_just_below_boundary_is_not_reliable(self):
        assert ConfidenceScore(0.64).is_reliable is False

    def test_str_shows_percentage(self):
        assert str(ConfidenceScore(0.75)) == "75%"

    def test_rejects_value_above_one(self):
        with pytest.raises(ValueError):
            ConfidenceScore(1.1)

    def test_rejects_negative_value(self):
        with pytest.raises(ValueError):
            ConfidenceScore(-0.1)
