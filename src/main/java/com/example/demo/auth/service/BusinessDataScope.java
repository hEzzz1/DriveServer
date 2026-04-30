package com.example.demo.auth.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record BusinessDataScope(
        boolean platformWide,
        Set<Long> enterpriseIds,
        Map<Long, Long> fleetEnterpriseIds
) {

    public BusinessDataScope {
        enterpriseIds = enterpriseIds == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(enterpriseIds));
        fleetEnterpriseIds = fleetEnterpriseIds == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(fleetEnterpriseIds));
    }

    public static BusinessDataScope empty() {
        return new BusinessDataScope(false, Set.of(), Map.of());
    }

    // Reserved for platform metadata queries. Do not use this scope for enterprise business data access.
    public static BusinessDataScope globalScope() {
        return new BusinessDataScope(true, Set.of(), Map.of());
    }

    public Set<Long> fleetIds() {
        return fleetEnterpriseIds.keySet();
    }

    public boolean isEmpty() {
        return !platformWide && enterpriseIds.isEmpty() && fleetEnterpriseIds.isEmpty();
    }

    public boolean canAccessEnterpriseResource(Long enterpriseId) {
        return platformWide || (enterpriseId != null && enterpriseIds.contains(enterpriseId));
    }

    public boolean canAccessData(Long enterpriseId, Long fleetId) {
        if (platformWide) {
            return true;
        }
        if (enterpriseId != null && enterpriseIds.contains(enterpriseId)) {
            return true;
        }
        return fleetId != null && fleetEnterpriseIds.containsKey(fleetId);
    }

    public BusinessDataScope restrictToEnterprise(Long enterpriseId) {
        if (enterpriseId == null) {
            return this;
        }
        if (platformWide) {
            return new BusinessDataScope(false, Set.of(enterpriseId), Map.of());
        }
        Set<Long> scopedEnterprises = enterpriseIds.contains(enterpriseId) ? Set.of(enterpriseId) : Set.of();
        Map<Long, Long> scopedFleets = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> entry : fleetEnterpriseIds.entrySet()) {
            if (enterpriseId.equals(entry.getValue())) {
                scopedFleets.put(entry.getKey(), entry.getValue());
            }
        }
        return new BusinessDataScope(false, scopedEnterprises, scopedFleets);
    }

    public BusinessDataScope restrictToFleet(Long fleetId, Long enterpriseId) {
        if (fleetId == null) {
            return this;
        }
        if (!canAccessData(enterpriseId, fleetId)) {
            return empty();
        }
        return new BusinessDataScope(false, Set.of(), Map.of(fleetId, enterpriseId));
    }

    public Predicate toPredicate(From<?, ?> root,
                                 CriteriaBuilder cb,
                                 String enterpriseField,
                                 String fleetField) {
        if (platformWide) {
            return cb.conjunction();
        }
        List<Predicate> scopes = new ArrayList<>();
        if (enterpriseField != null && !enterpriseIds.isEmpty()) {
            scopes.add(root.get(enterpriseField).in(enterpriseIds));
        }
        if (fleetField != null && !fleetEnterpriseIds.isEmpty()) {
            scopes.add(root.get(fleetField).in(fleetEnterpriseIds.keySet()));
        }
        if (scopes.isEmpty()) {
            return cb.disjunction();
        }
        return cb.or(scopes.toArray(Predicate[]::new));
    }
}
