package com.chutneytesting.design.infra.storage.scenario.compose;

import com.chutneytesting.design.domain.scenario.compose.ComposableStep;
import com.chutneytesting.design.domain.scenario.compose.Strategy;
import com.chutneytesting.design.infra.storage.scenario.compose.wrapper.StepVertex;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.OVertex;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrientComposableStepMapper {

    // SAVE
    static StepVertex composableStepToVertex(final ComposableStep composableStep, OVertex oVertex, ODatabaseSession dbSession) {
        return StepVertex.builder()
            .from(oVertex)
            .usingSession(dbSession)
            .withName(composableStep.name)
            .withTags(composableStep.tags)
            .withImplementation(composableStep.implementation)
            .withStrategy(composableStep.strategy)
            .withDefaultParameters(composableStep.defaultParameters)
            .withExecutionParameters(composableStep.executionParameters)
            .withSteps(composableStep.steps)
            .build();
    }

    // GET
    public static ComposableStep vertexToComposableStep(final StepVertex vertex) {
        vertex.reloadIfDirty();

        ComposableStep.ComposableStepBuilder builder = ComposableStep.builder()
            .withId(vertex.id())
            .withName(vertex.name())
            .withTags(vertex.tags())
            .withImplementation(vertex.implementation())
            .withDefaultParameters(vertex.defaultParameters())
            .withExecutionParameters(vertex.executionParameters().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, LinkedHashMap::new))
            );

        OElement strategy = vertex.strategy();
        Optional.ofNullable(strategy).ifPresent( s ->
            builder.withStrategy(new Strategy(strategy.getProperty("name"), strategy.getProperty("parameters")))
        );

        builder.withSteps(
            vertexToComposableStep(vertex.listChildrenSteps())
        );

        return builder.build();
    }

    public static List<ComposableStep> vertexToComposableStep(List<StepVertex> subSteps) {
        return subSteps.stream()
            .map(OrientComposableStepMapper::vertexToComposableStep)
            .collect(Collectors.toList());
    }

}
