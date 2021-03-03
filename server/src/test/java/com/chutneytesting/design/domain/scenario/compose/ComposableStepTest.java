package com.chutneytesting.design.domain.scenario.compose;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.groovy.util.Maps;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class ComposableStepTest {

    @Test
    public void should_throw_when_cyclic_dependency_found() {
        // Given
        ComposableStep himself = ComposableStep.builder().withName("himself").build();

        // When
        try {
            ComposableStep.builder()
                .from(himself)
                .withSteps(singletonList(himself))
                .build();
        } catch (Exception e) {
            // Then
            assertThat(e).isInstanceOf(ComposableStepCyclicDependencyException.class);
            assertThat(e.getMessage()).contains(himself.name);
            return;
        }
        Assertions.fail("Should throw when a cyclic dependency is found !");
    }

    @Test
    public void should_not_found_cyclic_dependency() {
        // Given
        ComposableStep leaf_1   = ComposableStep.builder().withId("leaf 1").build();
        ComposableStep leaf_2   = ComposableStep.builder().withId("leaf 2").build();
        ComposableStep subStep  = ComposableStep.builder().withId("subStep")
            .withSteps(asList(leaf_1, leaf_2))
            .build();
        ComposableStep parent   = ComposableStep.builder().withId("parent")
            .withSteps(singletonList(subStep))
            .build();

        // When
        ComposableStep.builder().withId("scenario")
            .withSteps(asList(subStep, parent, subStep))
            .build();
    }

    @Test
    void execution_parameters_should_equals_default_parameters_when_not_override() {
        // When
        ComposableStep step = ComposableStep.builder().withId("step")
            .withDefaultParameters(Maps.of(
                "dont_move_up", "has_default_value",
                "leaf_move_up", "")
            )
            .build();

        // Then
        assertThat(dodo(step.executionParameters)).isEqualTo(Maps.of(
            "dont_move_up", "has_default_value",
            "leaf_move_up", ""
        ));
    }

    private Map<String, String> dodo(Map<String, Pair<String, Boolean>> executionParameters) {
        return executionParameters.entrySet().stream()
            .collect(toMap(Map.Entry::getKey, e -> e.getValue().getKey(), (v1, v2) -> v1, LinkedHashMap::new));
    }

    @Test
    void empty_parameters_should_be_added_to_parent_execution_parameters() {
        // Given
        ComposableStep leaf = ComposableStep.builder().withId("leaf")
            .withDefaultParameters(Maps.of(
                "dont_move_up", "has_default_value",
                "move_up", ""/*because empty*/)
            )
            .build();

        // When
        ComposableStep parent = ComposableStep.builder().withId("parent")
            .withSteps(singletonList(leaf))
            .build();

        assertThat(parent.defaultParameters).isEmpty();
        assertThat(parent.executionParameters.entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        ).containsEntry("move_up", "");
    }

    @Test
    void empty_parameters_should_be_added_to_parent_execution_parameters_2() {
        // Given
        ComposableStep leaf = ComposableStep.builder().withId("leaf")
            .withDefaultParameters(Maps.of(
                "dont_move_up", "has_default_value",
                "leaf_move_up", ""/*because empty*/)
            )
            .build();

        ComposableStep subStep = ComposableStep.builder().withId("subStep")
            .withSteps(singletonList(leaf))
            .withDefaultParameters(Maps.of(
                "dont_move_up", "has_default_value",
                "substep_move_up", ""/*because empty*/)
            )
            .build();

        // When
        ComposableStep parent = ComposableStep.builder().withId("parent")
            .withDefaultParameters(Maps.of("parent_param", "has_default_value" /*but can be override*/))
            .withSteps(singletonList(subStep))
            .build();


        assertThat(dodo(parent.executionParameters)).isEqualTo(Maps.of(
            "leaf_move_up", "",
            "substep_move_up", "",
            "parent_param", "has_default_value"
        ));
    }

    @Test
    void execution_parameters_can_be_override_upon_step_use() {
        // Given
        ComposableStep leaf = ComposableStep.builder().withId("leaf")
            .withDefaultParameters(Maps.of(
                "dont_move_up", "has_default_value",
                "leaf_move_up", "")
            )
            .build();

        ComposableStep parent = ComposableStep.builder().withId("subStep")
            .withSteps(singletonList(
                leaf.usingExecutionParameters(Maps.of(
                    "dont_move_up", "",
                    "leaf_move_up", "has_value_defined_upon_usage")
                )
            ))
            .build();

        assertThat(dodo(parent.executionParameters)).isEqualTo(Maps.of(
            "dont_move_up", ""
        ));
    }

}
