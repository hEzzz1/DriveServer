package com.example.demo.rule;

import com.example.demo.rule.service.RiskScoreCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskScoreCalculatorTest {

    private final RiskScoreCalculator calculator = new RiskScoreCalculator();

    @Test
    void shouldCalculateWeightedRiskScore() {
        BigDecimal result = calculator.calculate(new BigDecimal("0.82"), new BigDecimal("0.64"));

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.7390"));
    }

    @Test
    void shouldKeepScoreWithinExpectedScale() {
        BigDecimal result = calculator.calculate(new BigDecimal("1.00"), new BigDecimal("1.00"));

        assertThat(result).isEqualByComparingTo(new BigDecimal("1.0000"));
    }

    @Test
    void shouldRejectOutOfRangeScore() {
        assertThatThrownBy(() -> calculator.calculate(new BigDecimal("1.10"), new BigDecimal("0.20")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fatigueScore");
    }
}
