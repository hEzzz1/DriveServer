package com.example.demo.rule;

import com.example.demo.rule.repository.RuleConfigRepository;
import com.example.demo.rule.service.EdgeConfigVersionResolver;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EdgeConfigVersionResolverTest {

    @Test
    void shouldBuildVersionFromRepositorySummary() {
        RuleConfigRepository repository = mock(RuleConfigRepository.class);
        when(repository.summarizeActiveRuleset()).thenReturn(new Object[]{3L, 7, LocalDateTime.of(2026, 4, 28, 8, 30, 0)});

        EdgeConfigVersionResolver resolver = new EdgeConfigVersionResolver(repository);

        assertThat(resolver.resolveCurrentVersion()).isEqualTo("ruleset/3/7/1777365000");
    }

    @Test
    void shouldReturnEmptyVersionWhenNoActiveRule() {
        RuleConfigRepository repository = mock(RuleConfigRepository.class);
        when(repository.summarizeActiveRuleset()).thenReturn(new Object[]{0L, null, null});

        EdgeConfigVersionResolver resolver = new EdgeConfigVersionResolver(repository);

        assertThat(resolver.resolveCurrentVersion()).isEqualTo("ruleset/empty");
    }

    @Test
    void shouldBuildVersionFromNestedRepositorySummaryRow() {
        RuleConfigRepository repository = mock(RuleConfigRepository.class);
        when(repository.summarizeActiveRuleset()).thenReturn(new Object[]{
                new Object[]{3L, 7, LocalDateTime.of(2026, 4, 28, 8, 30, 0)}
        });

        EdgeConfigVersionResolver resolver = new EdgeConfigVersionResolver(repository);

        assertThat(resolver.resolveCurrentVersion()).isEqualTo("ruleset/3/7/1777365000");
    }

    @Test
    void shouldReuseCachedSummaryWithinTtl() {
        RuleConfigRepository repository = mock(RuleConfigRepository.class);
        when(repository.summarizeActiveRuleset()).thenReturn(new Object[]{1L, 2, LocalDateTime.of(2026, 4, 28, 8, 30, 0)});

        EdgeConfigVersionResolver resolver = new EdgeConfigVersionResolver(repository);

        String first = resolver.resolveCurrentVersion();
        String second = resolver.resolveCurrentVersion();

        assertThat(first).isEqualTo(second);
        verify(repository, times(1)).summarizeActiveRuleset();
    }
}
