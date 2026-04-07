package com.example.demo.rule.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Component
public class RiskScoreCalculator {

    static final BigDecimal FATIGUE_WEIGHT = new BigDecimal("0.55");
    static final BigDecimal DISTRACTION_WEIGHT = new BigDecimal("0.45");

    public BigDecimal calculate(BigDecimal fatigueScore, BigDecimal distractionScore) {
        BigDecimal fatigue = validateScore(fatigueScore, "fatigueScore");
        BigDecimal distraction = validateScore(distractionScore, "distractionScore");

        BigDecimal weightedFatigue = fatigue.multiply(FATIGUE_WEIGHT);
        BigDecimal weightedDistraction = distraction.multiply(DISTRACTION_WEIGHT);
        return weightedFatigue.add(weightedDistraction).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal validateScore(BigDecimal score, String fieldName) {
        Objects.requireNonNull(score, fieldName + " must not be null");
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(fieldName + " must be in [0, 1]");
        }
        return score;
    }
}
