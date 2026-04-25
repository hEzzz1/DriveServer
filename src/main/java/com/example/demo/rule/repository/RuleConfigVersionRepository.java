package com.example.demo.rule.repository;

import com.example.demo.rule.entity.RuleConfigVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RuleConfigVersionRepository extends JpaRepository<RuleConfigVersion, Long> {
    List<RuleConfigVersion> findByRuleConfigIdOrderByVersionNoDesc(Long ruleConfigId);

    Optional<RuleConfigVersion> findByRuleConfigIdAndVersionNo(Long ruleConfigId, Integer versionNo);

    Optional<RuleConfigVersion> findFirstByRuleConfigIdOrderByVersionNoDesc(Long ruleConfigId);
}

