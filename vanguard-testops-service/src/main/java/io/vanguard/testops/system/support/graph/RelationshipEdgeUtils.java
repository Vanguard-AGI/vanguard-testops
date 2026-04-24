package io.vanguard.testops.system.support.graph;

import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.dto.RelationshipEdgeDTO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RelationshipEdgeUtils {

    public static void checkEdge(List<RelationshipEdgeDTO> edgeDTOS) {
        HashSet<String> nodeIds = new HashSet<>();
        nodeIds.addAll(edgeDTOS.stream().map(RelationshipEdgeDTO::getSourceId).collect(Collectors.toSet()));
        nodeIds.addAll(edgeDTOS.stream().map(RelationshipEdgeDTO::getTargetId).collect(Collectors.toSet()));
        HashSet<String> visitedSet = new HashSet<>();
        nodeIds.forEach(nodeId -> {
            if (!visitedSet.contains(nodeId) && directedCycle(nodeId, edgeDTOS, new HashSet<>(), visitedSet)) {
                throw new MSException(Translator.get("cycle_relationship"));
            }
        });
    }

    public static boolean directedCycle(String id, List<RelationshipEdgeDTO> edges, Set<String> markSet, Set<String> visitedSet) {
        if (markSet.contains(id)) {
            return true;
        }

        markSet.add(id);
        visitedSet.add(id);

        ArrayList<String> nextLevelNodes = new ArrayList<>();
        for (RelationshipEdgeDTO relationshipEdge : edges) {
            if (id.equals(relationshipEdge.getSourceId())) {
                nextLevelNodes.add(relationshipEdge.getTargetId());
            }
        }

        for (String nextNode : nextLevelNodes) {
            if (directedCycle(nextNode, edges, markSet, visitedSet)) {
                return true;
            }
        }

        markSet.remove(id);
        return false;
    }

    public static void updateGraphId(String id, Function<String, RelationshipEdgeDTO> getGraphIdFunc, Function<String, List<RelationshipEdgeDTO>> getEdgeByGraphIdFunc, BiConsumer<List, String> updateFunc) {
        RelationshipEdgeDTO edge = getGraphIdFunc.apply(id);
        if (edge == null) {
            throw new MSException(Translator.get("relationship_not_exist"));
        }
        List<RelationshipEdgeDTO> edges = getEdgeByGraphIdFunc.apply(edge.getGraphId());

        edges = edges.stream()
                .filter(i -> !i.getSourceId().equals(edge.getSourceId()) && !i.getTargetId().equals(edge.getTargetId()))
                .collect(Collectors.toList());

        Set<String> nodes = new HashSet<>();
        Set<String> markSet = new HashSet<>();
        nodes.addAll(edges.stream().map(RelationshipEdgeDTO::getSourceId).collect(Collectors.toSet()));
        nodes.addAll(edges.stream().map(RelationshipEdgeDTO::getTargetId).collect(Collectors.toSet()));

        dfsForMark(edge.getSourceId(), edges, markSet, true);
        dfsForMark(edge.getSourceId(), edges, markSet, false);

        if (markSet.size() != nodes.size()) {
            List<String> updateIds = new ArrayList<>(markSet);
            updateFunc.accept(updateIds, UUID.randomUUID().toString());
        }
    }

    public static void dfsForMark(String node, List<RelationshipEdgeDTO> edges, Set<String> markSet, boolean isForwardDirection) {
        markSet.add(node);

        Set<String> nextLevelNodes = new HashSet<>();

        for (RelationshipEdgeDTO edge : edges) {
            if (isForwardDirection) {
                if (node.equals(edge.getSourceId())) {
                    nextLevelNodes.add(edge.getTargetId());
                }
            } else {
                if (node.equals(edge.getTargetId())) {
                    nextLevelNodes.add(edge.getSourceId());
                }
            }
        }

        nextLevelNodes.forEach(nextNode -> {
            if (!markSet.contains(nextNode)) {
                dfsForMark(nextNode, edges, markSet, true);
                dfsForMark(nextNode, edges, markSet, false);
            }
        });
    }
}
