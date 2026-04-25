package com.example.demo.rule.service;

import com.example.demo.rule.model.RuleDefinition;

import java.util.List;

public interface RuleDefinitionProvider {
    List<RuleDefinition> loadEnabledRuleDefinitions();
}
