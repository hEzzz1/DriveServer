package com.example.demo.system.service;

import com.example.demo.alert.model.AlertStatus;
import com.example.demo.alert.repository.AlertEventRepository;
import com.example.demo.common.trace.TraceIdContext;
import com.example.demo.rule.model.RuleConfigStatus;
import com.example.demo.rule.repository.RuleConfigRepository;
import com.example.demo.system.dto.SystemHealthResponseData;
import com.example.demo.system.dto.SystemMonitoringResponseData;
import com.example.demo.system.dto.SystemServiceStatusItemData;
import com.example.demo.system.dto.SystemServicesResponseData;
import com.example.demo.system.dto.SystemSummaryResponseData;
import com.example.demo.system.dto.SystemVersionResponseData;
import com.example.demo.system.entity.SystemAuditLog;
import com.example.demo.system.repository.SystemAuditRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

@Service
public class SystemManagementService {

    private final HealthEndpoint healthEndpoint;
    private final Environment environment;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;
    private final AlertEventRepository alertEventRepository;
    private final RuleConfigRepository ruleConfigRepository;
    private final SystemAuditRepository systemAuditRepository;

    public SystemManagementService(HealthEndpoint healthEndpoint,
                                   Environment environment,
                                   ObjectProvider<BuildProperties> buildPropertiesProvider,
                                   AlertEventRepository alertEventRepository,
                                   RuleConfigRepository ruleConfigRepository,
                                   SystemAuditRepository systemAuditRepository) {
        this.healthEndpoint = healthEndpoint;
        this.environment = environment;
        this.buildPropertiesProvider = buildPropertiesProvider;
        this.alertEventRepository = alertEventRepository;
        this.ruleConfigRepository = ruleConfigRepository;
        this.systemAuditRepository = systemAuditRepository;
    }

    @Transactional(readOnly = true)
    public SystemHealthResponseData getHealth() {
        HealthComponent component = healthEndpoint.health();
        Map<String, Object> details = new LinkedHashMap<>();
        if (component instanceof Health health) {
            details.putAll(health.getDetails());
        }
        details.put("traceId", Optional.ofNullable(TraceIdContext.getTraceId()).orElse(""));
        return new SystemHealthResponseData(component.getStatus().getCode(), details);
    }

    @Transactional(readOnly = true)
    public SystemServicesResponseData getServices() {
        List<SystemServiceStatusItemData> items = new ArrayList<>();
        items.add(probe("api", "UP", "HTTP接口与JWT鉴权正常"));
        items.add(probe("alert", probeRepository(alertEventRepository::count), "告警数据访问正常"));
        items.add(probe("rule-engine", probeRepository(ruleConfigRepository::count), "规则配置读取正常"));
        items.add(probe("audit", probeRepository(systemAuditRepository::count), "审计日志访问正常"));
        items.add(probe("monitoring", getHealth().status(), "健康检查汇总"));
        return new SystemServicesResponseData(items);
    }

    @Transactional(readOnly = true)
    public SystemVersionResponseData getVersion() {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        String applicationName = environment.getProperty("spring.application.name", "DriveServer");
        String version = buildProperties == null ? Optional.ofNullable(getClass().getPackage().getImplementationVersion()).orElse("unknown")
                : buildProperties.getVersion();
        String buildTime = buildProperties == null ? "unknown" : buildProperties.getTime().toString();
        String gitCommit = environment.getProperty("git.commit.id.abbrev",
                environment.getProperty("git.commit.id", "unknown"));
        return new SystemVersionResponseData(applicationName, version, buildTime, gitCommit);
    }

    @Transactional(readOnly = true)
    public SystemMonitoringResponseData getMonitoring() {
        LocalDateTime snapshotAt = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime start24h = snapshotAt.minusHours(24);

        long openAlertCount = countAlerts((root, query, cb) -> cb.equal(root.get("status"), AlertStatus.NEW.getCode()));
        long alertCount24h = countAlerts((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("triggerTime"), start24h));
        long auditCount24h = countAudits((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("actionTime"), start24h));
        long enabledRuleCount = ruleConfigRepository.findAll().stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getEnabled()))
                .filter(rule -> RuleConfigStatus.ENABLED.name().equals(rule.getStatus()))
                .count();
        BigDecimal averageRiskScore24h = averageRiskScore(start24h);

        return new SystemMonitoringResponseData(
                snapshotAt.atOffset(ZoneOffset.UTC),
                openAlertCount,
                alertCount24h,
                auditCount24h,
                enabledRuleCount,
                averageRiskScore24h);
    }

    @Transactional(readOnly = true)
    public SystemSummaryResponseData getSummary() {
        return new SystemSummaryResponseData(
                OffsetDateTime.now(ZoneOffset.UTC),
                getHealth(),
                getServices(),
                getVersion(),
                getMonitoring());
    }

    private String probeRepository(Callable<Long> callable) {
        try {
            callable.call();
            return "UP";
        } catch (Exception ex) {
            return "DOWN";
        }
    }

    private SystemServiceStatusItemData probe(String service, String status, String description) {
        return new SystemServiceStatusItemData(service, status, description, OffsetDateTime.now(ZoneOffset.UTC));
    }

    private long countAlerts(Specification<com.example.demo.alert.entity.AlertEvent> specification) {
        return alertEventRepository.count(specification);
    }

    private long countAudits(Specification<SystemAuditLog> specification) {
        return systemAuditRepository.count(specification);
    }

    private BigDecimal averageRiskScore(LocalDateTime start24h) {
        List<com.example.demo.alert.entity.AlertEvent> alerts = alertEventRepository.findAll(
                (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("triggerTime"), start24h));
        if (alerts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = alerts.stream()
                .map(alert -> alert.getRiskScore() == null ? BigDecimal.ZERO : alert.getRiskScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(alerts.size()), 4, java.math.RoundingMode.HALF_UP);
    }
}
