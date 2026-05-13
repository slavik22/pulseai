package com.pulseai.ai.domain;

import com.pulseai.ai.domain.model.ConfidenceScore;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ConfidenceScoreTest {

    @Test
    void shouldCreateValidConfidenceScore() {
        ConfidenceScore score = ConfidenceScore.of(0.85);
        assertThat(score.value()).isEqualTo(0.85);
    }

    @Test
    void shouldRejectNegativeScore() {
        assertThatThrownBy(() -> ConfidenceScore.of(-0.1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectScoreAboveOne() {
        assertThatThrownBy(() -> ConfidenceScore.of(1.01))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptBoundaryValues() {
        assertThatNoException().isThrownBy(() -> ConfidenceScore.of(0.0));
        assertThatNoException().isThrownBy(() -> ConfidenceScore.of(1.0));
    }
}
