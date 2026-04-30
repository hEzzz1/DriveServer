package com.example.demo.stats.service;

import com.example.demo.alert.entity.AlertEvent;
import com.example.demo.alert.repository.AlertEventRepository;
import com.example.demo.alert.model.AlertStatus;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.auth.service.BusinessDataScope;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.stats.dto.OverviewLatestAlertItemData;
import com.example.demo.stats.dto.OverviewRiskDistributionItemData;
import com.example.demo.stats.dto.RankingItemData;
import com.example.demo.stats.dto.RankingResponseData;
import com.example.demo.stats.dto.StatsOverviewResponseData;
import com.example.demo.stats.dto.TrendItemData;
import com.example.demo.stats.dto.TrendResponseData;
import com.example.demo.stats.model.RankingDimension;
import com.example.demo.stats.model.RankingSortBy;
import com.example.demo.stats.model.TrendGroupBy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class StatsService {

    private static final int DEFAULT_RANKING_LIMIT = 10;
    private static final int MAX_RANKING_LIMIT = 100;
    private static final int LATEST_ALERT_LIMIT = 5;

    private final AlertEventRepository alertEventRepository;
    private final BusinessAccessService businessAccessService;

    public StatsService(AlertEventRepository alertEventRepository,
                        BusinessAccessService businessAccessService) {
        this.alertEventRepository = alertEventRepository;
        this.businessAccessService = businessAccessService;
    }

    @Transactional(readOnly = true)
    public StatsOverviewResponseData getRealtimeOverview(AuthenticatedUser operator, Long fleetId) {
        LocalDateTime windowEnd = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime windowStart = windowEnd.minusMinutes(5);
        BusinessDataScope dataScope = businessAccessService.resolveDataScope(operator, null, fleetId);

        List<AlertEvent> recentAlerts = alertEventRepository.findAll(
                buildFilterSpecification(dataScope, null, null, windowStart, windowEnd));
        List<AlertEvent> latestAlerts = alertEventRepository.findAll(
                buildFilterSpecification(dataScope, null, null, null, null),
                PageRequest.of(0, LATEST_ALERT_LIMIT,
                        Sort.by(Sort.Direction.DESC, "triggerTime").and(Sort.by(Sort.Direction.DESC, "id"))))
                .getContent();

        long highRiskCount = recentAlerts.stream()
                .filter(alert -> alert.getRiskLevel() != null && alert.getRiskLevel() >= 3)
                .count();
        long handledCount = recentAlerts.stream()
                .filter(alert -> alert.getStatus() != null && alert.getStatus() != AlertStatus.NEW.getCode())
                .count();

        List<OverviewRiskDistributionItemData> riskDistribution = new ArrayList<>();
        for (int riskLevel = 1; riskLevel <= 3; riskLevel++) {
            int currentRiskLevel = riskLevel;
            long count = recentAlerts.stream()
                    .filter(alert -> alert.getRiskLevel() != null && alert.getRiskLevel() == currentRiskLevel)
                    .count();
            riskDistribution.add(new OverviewRiskDistributionItemData(currentRiskLevel, count));
        }

        return new StatsOverviewResponseData(
                fleetId,
                toOffsetDateTime(windowStart),
                toOffsetDateTime(windowEnd),
                recentAlerts.size(),
                highRiskCount,
                handledCount,
                latestAlerts.stream().map(this::toOverviewLatestAlertItem).toList(),
                riskDistribution);
    }

    @Transactional(readOnly = true)
    public TrendResponseData getTrend(AuthenticatedUser operator,
                                      Long fleetId,
                                      Integer riskLevel,
                                      Integer status,
                                      OffsetDateTime startTime,
                                      OffsetDateTime endTime,
                                      TrendGroupBy groupBy) {
        LocalDateTime endTimeUtc = endTime == null ? LocalDateTime.now(ZoneOffset.UTC) : toUtcLocalDateTime(endTime);
        LocalDateTime startTimeUtc = startTime == null ? defaultTrendStart(endTimeUtc, groupBy) : toUtcLocalDateTime(startTime);
        validateTimeWindow(startTimeUtc, endTimeUtc);
        validateRiskLevel(riskLevel);
        validateStatus(status);

        BusinessDataScope dataScope = businessAccessService.resolveDataScope(operator, null, fleetId);
        List<AlertEvent> alerts = alertEventRepository.findAll(
                buildFilterSpecification(dataScope, riskLevel, status, startTimeUtc, endTimeUtc));

        Map<LocalDateTime, BucketAccumulator> buckets = initializeBuckets(startTimeUtc, endTimeUtc, groupBy);
        for (AlertEvent alert : alerts) {
            LocalDateTime bucket = truncateToGroup(alert.getTriggerTime(), groupBy);
            BucketAccumulator accumulator = buckets.get(bucket);
            if (accumulator != null) {
                accumulator.accept(alert);
            }
        }

        List<TrendItemData> items = buckets.entrySet().stream()
                .map(entry -> entry.getValue().toTrendItem(entry.getKey()))
                .toList();

        return new TrendResponseData(
                groupBy,
                fleetId,
                riskLevel,
                status,
                toOffsetDateTime(startTimeUtc),
                toOffsetDateTime(endTimeUtc),
                items);
    }

    @Transactional(readOnly = true)
    public RankingResponseData getRanking(AuthenticatedUser operator,
                                          Long fleetId,
                                          Integer riskLevel,
                                          Integer status,
                                          OffsetDateTime startTime,
                                          OffsetDateTime endTime,
                                          RankingDimension dimension,
                                          RankingSortBy sortBy,
                                          Integer limit) {
        LocalDateTime endTimeUtc = endTime == null ? LocalDateTime.now(ZoneOffset.UTC) : toUtcLocalDateTime(endTime);
        LocalDateTime startTimeUtc = startTime == null ? endTimeUtc.minusDays(7) : toUtcLocalDateTime(startTime);
        validateTimeWindow(startTimeUtc, endTimeUtc);
        validateRiskLevel(riskLevel);
        validateStatus(status);
        int normalizedLimit = normalizeLimit(limit);

        BusinessDataScope dataScope = businessAccessService.resolveDataScope(operator, null, fleetId);
        List<AlertEvent> alerts = alertEventRepository.findAll(
                buildFilterSpecification(dataScope, riskLevel, status, startTimeUtc, endTimeUtc));

        Map<Long, BucketAccumulator> dimensionBuckets = new LinkedHashMap<>();
        for (AlertEvent alert : alerts) {
            Long dimensionValue = resolveDimensionValue(alert, dimension);
            if (dimensionValue == null) {
                continue;
            }
            dimensionBuckets.computeIfAbsent(dimensionValue, key -> new BucketAccumulator()).accept(alert);
        }

        Comparator<Map.Entry<Long, BucketAccumulator>> comparator = comparatorFor(sortBy)
                .thenComparing(Map.Entry::getKey);
        List<Map.Entry<Long, BucketAccumulator>> sortedEntries = dimensionBuckets.entrySet().stream()
                .sorted(comparator)
                .limit(normalizedLimit)
                .toList();

        List<RankingItemData> items = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Long, BucketAccumulator> entry : sortedEntries) {
            BucketAccumulator accumulator = entry.getValue();
            items.add(new RankingItemData(
                    rank++,
                    entry.getKey(),
                    accumulator.alertCount,
                    accumulator.highRiskCount,
                    accumulator.averageRiskScore(),
                    accumulator.averageFatigueScore(),
                    accumulator.averageDistractionScore()));
        }

        return new RankingResponseData(
                dimension,
                sortBy,
                normalizedLimit,
                fleetId,
                riskLevel,
                status,
                toOffsetDateTime(startTimeUtc),
                toOffsetDateTime(endTimeUtc),
                dimensionBuckets.size(),
                items);
    }

    private Specification<AlertEvent> buildFilterSpecification(BusinessDataScope dataScope,
                                                               Integer riskLevel,
                                                               Integer status,
                                                               LocalDateTime startTime,
                                                               LocalDateTime endTime) {
        List<Specification<AlertEvent>> specifications = new ArrayList<>();
        specifications.add((root, query, cb) -> dataScope.toPredicate(root, cb, "enterpriseId", "fleetId"));
        if (riskLevel != null) {
            specifications.add((root, query, cb) -> cb.equal(root.get("riskLevel"), riskLevel.byteValue()));
        }
        if (status != null) {
            specifications.add((root, query, cb) -> cb.equal(root.get("status"), status.byteValue()));
        }
        if (startTime != null) {
            specifications.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("triggerTime"), startTime));
        }
        if (endTime != null) {
            specifications.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("triggerTime"), endTime));
        }
        return specifications.stream().reduce(Specification.where(null), Specification::and);
    }

    private Map<LocalDateTime, BucketAccumulator> initializeBuckets(LocalDateTime startTime,
                                                                    LocalDateTime endTime,
                                                                    TrendGroupBy groupBy) {
        Map<LocalDateTime, BucketAccumulator> buckets = new LinkedHashMap<>();
        LocalDateTime cursor = truncateToGroup(startTime, groupBy);
        LocalDateTime endBucket = truncateToGroup(endTime, groupBy);
        while (!cursor.isAfter(endBucket)) {
            buckets.put(cursor, new BucketAccumulator());
            cursor = plusGroup(cursor, groupBy);
        }
        return buckets;
    }

    private LocalDateTime truncateToGroup(LocalDateTime time, TrendGroupBy groupBy) {
        if (groupBy == TrendGroupBy.DAY) {
            return time.truncatedTo(ChronoUnit.DAYS);
        }
        return time.truncatedTo(ChronoUnit.HOURS);
    }

    private LocalDateTime plusGroup(LocalDateTime time, TrendGroupBy groupBy) {
        if (groupBy == TrendGroupBy.DAY) {
            return time.plusDays(1);
        }
        return time.plusHours(1);
    }

    private LocalDateTime defaultTrendStart(LocalDateTime endTime, TrendGroupBy groupBy) {
        if (groupBy == TrendGroupBy.DAY) {
            return endTime.minusDays(6);
        }
        return endTime.minusHours(23);
    }

    private Comparator<Map.Entry<Long, BucketAccumulator>> comparatorFor(RankingSortBy sortBy) {
        if (sortBy == RankingSortBy.HIGH_RISK_COUNT) {
            return Comparator.<Map.Entry<Long, BucketAccumulator>>comparingLong(entry -> entry.getValue().highRiskCount)
                    .reversed();
        }
        if (sortBy == RankingSortBy.AVG_RISK_SCORE) {
            return Comparator.<Map.Entry<Long, BucketAccumulator>, BigDecimal>comparing(
                            entry -> entry.getValue().averageRiskScore(),
                            Comparator.nullsLast(BigDecimal::compareTo))
                    .reversed();
        }
        return Comparator.<Map.Entry<Long, BucketAccumulator>>comparingLong(entry -> entry.getValue().alertCount)
                .reversed();
    }

    private Long resolveDimensionValue(AlertEvent alert, RankingDimension dimension) {
        Function<AlertEvent, Long> extractor;
        if (dimension == RankingDimension.FLEET_ID) {
            extractor = AlertEvent::getFleetId;
        } else if (dimension == RankingDimension.VEHICLE_ID) {
            extractor = AlertEvent::getVehicleId;
        } else if (dimension == RankingDimension.DRIVER_ID) {
            extractor = AlertEvent::getDriverId;
        } else {
            extractor = AlertEvent::getRuleId;
        }
        return extractor.apply(alert);
    }

    private OverviewLatestAlertItemData toOverviewLatestAlertItem(AlertEvent alert) {
        return new OverviewLatestAlertItemData(
                alert.getId(),
                alert.getAlertNo(),
                alert.getFleetId(),
                alert.getVehicleId(),
                alert.getDriverId(),
                alert.getRiskLevel() == null ? null : (int) alert.getRiskLevel(),
                alert.getStatus() == null ? null : (int) alert.getStatus(),
                alert.getRiskScore(),
                toOffsetDateTime(alert.getTriggerTime()));
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_RANKING_LIMIT;
        }
        if (limit < 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "limit必须大于等于1");
        }
        return Math.min(limit, MAX_RANKING_LIMIT);
    }

    private void validateTimeWindow(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "startTime不能晚于endTime");
        }
    }

    private void validateRiskLevel(Integer riskLevel) {
        if (riskLevel != null && (riskLevel < 1 || riskLevel > 3)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "riskLevel仅支持1到3");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null) {
            return;
        }
        try {
            AlertStatus.fromCode(status.byteValue());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "status取值非法");
        }
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime time) {
        return time == null ? null : time.atOffset(ZoneOffset.UTC);
    }

    private LocalDateTime toUtcLocalDateTime(OffsetDateTime time) {
        return LocalDateTime.ofInstant(time.toInstant(), ZoneOffset.UTC);
    }

    private static final class BucketAccumulator {
        private long alertCount;
        private long highRiskCount;
        private BigDecimal riskScoreSum = BigDecimal.ZERO;
        private BigDecimal fatigueScoreSum = BigDecimal.ZERO;
        private BigDecimal distractionScoreSum = BigDecimal.ZERO;

        private void accept(AlertEvent alert) {
            alertCount++;
            if (alert.getRiskLevel() != null && alert.getRiskLevel() >= 3) {
                highRiskCount++;
            }
            riskScoreSum = riskScoreSum.add(zeroIfNull(alert.getRiskScore()));
            fatigueScoreSum = fatigueScoreSum.add(zeroIfNull(alert.getFatigueScore()));
            distractionScoreSum = distractionScoreSum.add(zeroIfNull(alert.getDistractionScore()));
        }

        private TrendItemData toTrendItem(LocalDateTime bucketTime) {
            return new TrendItemData(
                    bucketTime.atOffset(ZoneOffset.UTC),
                    alertCount,
                    highRiskCount,
                    averageRiskScore(),
                    averageFatigueScore(),
                    averageDistractionScore());
        }

        private BigDecimal averageRiskScore() {
            return average(riskScoreSum, alertCount);
        }

        private BigDecimal averageFatigueScore() {
            return average(fatigueScoreSum, alertCount);
        }

        private BigDecimal averageDistractionScore() {
            return average(distractionScoreSum, alertCount);
        }

        private static BigDecimal zeroIfNull(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }

        private static BigDecimal average(BigDecimal total, long count) {
            if (count == 0) {
                return BigDecimal.ZERO;
            }
            return total.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
        }
    }
}
