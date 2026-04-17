package com.example.demo.rule.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Component
public class RiskScoreCalculator {

    public BigDecimal calculate(BigDecimal fatigueScore, BigDecimal distractionScore) {
        BigDecimal fatigue = validateScore(fatigueScore, "fatigueScore");
        BigDecimal distraction = validateScore(distractionScore, "distractionScore");
        return fatigue.max(distraction).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal validateScore(BigDecimal score, String fieldName) {
        Objects.requireNonNull(score, fieldName + " must not be null");
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(fieldName + " must be in [0, 1]");
        }
        return score;
    }
}
