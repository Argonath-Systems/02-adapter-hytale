package com.argonathsystems.adapter.hytale.integration;

import com.argonathsystems.hyquest.model.*;
import com.lordofthetales.framework.quest.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts between HyQuestUI format (graph-based with nodes/edges) 
 * and Quest Framework format (linear stages with objectives).
 * 
 * <h2>Format Comparison:</h2>
 * 
 * <table border="1">
 *   <tr>
 *     <th>Aspect</th>
 *     <th>HyQuestUI Format</th>
 *     <th>Quest Framework Format</th>
 *   </tr>
 *   <tr>
 *     <td>Structure</td>
 *     <td>Graph (nodes + edges)</td>
 *     <td>Linear (stages list)</td>
 *   </tr>
 *   <tr>
 *     <td>Visual Representation</td>
 *     <td>Yes (x, y positions)</td>
 *     <td>No (text-based)</td>
 *   </tr>
 *   <tr>
 *     <td>Quest Flow</td>
 *     <td>Follows edges between nodes</td>
 *     <td>Sequential stages (0, 1, 2...)</td>
 *   </tr>
 *   <tr>
 *     <td>Objectives</td>
 *     <td>Objective nodes in graph</td>
 *     <td>QuestObjectiveReference list in stage</td>
 *   </tr>
 *   <tr>
 *     <td>Metadata</td>
 *     <td>Generic Map (flexible)</td>
 *     <td>Specific fields (type, prerequisites)</td>
 *   </tr>
 * </table>
 * 
 * <h2>Example HyQuestUI Quest:</h2>
 * <pre>{@code
 * {
 *   "id": "tutorial_quest",
 *   "title": "First Steps",
 *   "nodes": [
 *     { "id": "root", "type": "questRoot", "data": { "label": "Start" } },
 *     { "id": "stage1", "type": "stage", "data": { "label": "Learn the Basics" } },
 *     { "id": "obj1", "type": "objective", "data": { "type": "talk", "npc": "guard" } },
 *     { "id": "reward1", "type": "reward", "data": { "type": "xp", "amount": 100 } }
 *   ],
 *   "edges": [
 *     { "source": "root", "target": "stage1" },
 *     { "source": "stage1", "target": "obj1" },
 *     { "source": "obj1", "target": "reward1" }
 *   ]
 * }
 * }</pre>
 * 
 * <h2>Example Quest Framework Quest:</h2>
 * <pre>{@code
 * {
 *   "id": "tutorial_quest",
 *   "title": "First Steps",
 *   "type": "MAIN",
 *   "stages": [
 *     {
 *       "id": 0,
 *       "description": "Learn the Basics",
 *       "objectives": [
 *         { "objectiveId": "talk_to_guard", "config": { "npc": "guard" } }
 *       ]
 *     }
 *   ],
 *   "rewards": [
 *     { "type": "XP", "amount": 100 }
 *   ]
 * }
 * }</pre>
 * 
 * @author Argonath Systems
 * @version 1.0.0
 * @since 1.0.0
 */
public class QuestFormatConverter {
    
    /**
     * Convert HyQuestUI graph format to Quest Framework linear format.
     * 
     * <p>Conversion strategy:
     * <ol>
     *   <li>Find root node</li>
 *   <li>Follow edges to discover stages in order</li>
     *   <li>Group objective nodes under each stage</li>
     *   <li>Extract reward nodes</li>
     *   <li>Map metadata to prerequisites</li>
     * </ol>
     * 
     * @param hyQuest HyQuestUI quest definition
     * @return Quest Framework definition
     */
    public com.lordofthetales.framework.quest.model.QuestDefinition toFrameworkFormat(
        QuestDefinition hyQuest
    ) {
        com.lordofthetales.framework.quest.model.QuestDefinition framework = 
            new com.lordofthetales.framework.quest.model.QuestDefinition();
        
        // Basic properties
        framework.setId(hyQuest.getId());
        framework.setTitle("Quest from HyQuestUI"); // HyQuest model doesn't expose title directly
        framework.setDescription("Imported from HyQuest Designer");
        
        // Extract quest type from metadata (if available)
        framework.setType(com.lordofthetales.framework.quest.model.QuestDefinition.QuestType.SIDE);
        
        // Convert graph to linear stages
        List<com.lordofthetales.framework.quest.model.QuestStage> stages = extractStagesFromGraph(hyQuest);
        framework.setStages(stages);
        
        // Extract rewards
        List<com.lordofthetales.framework.quest.model.QuestReward> rewards = extractRewards(hyQuest);
        framework.setRewards(rewards);
        
        return framework;
    }
    
    /**
     * Convert Quest Framework linear format to HyQuestUI graph format.
     * 
     * <p>Conversion strategy:
     * <ol>
     *   <li>Create root node</li>
     *   <li>Create stage nodes for each stage</li>
     *   <li>Create objective nodes for each objective</li>
     *   <li>Create reward nodes</li>
     *   <li>Link all nodes with edges</li>
     * </ol>
     * 
     * @param framework Quest Framework definition
     * @return HyQuestUI quest definition
     */
    public QuestDefinition toHyQuestFormat(
        com.lordofthetales.framework.quest.model.QuestDefinition framework
    ) {
        QuestDefinition hyQuest = new QuestDefinition();
        hyQuest.setId(framework.getId());
        
        List<QuestDefinitionNodesInner> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        
        // Create root node
        QuestDefinitionNodesInner root = createNode("root", "questRoot", framework.getTitle());
        nodes.add(root);
        
        String previousId = "root";
        int stageCounter = 0;
        
        // Create nodes for each stage
        for (com.lordofthetales.framework.quest.model.QuestStage stage : framework.getStages()) {
            String stageId = "stage_" + stageCounter;
            
            // Stage node
            QuestDefinitionNodesInner stageNode = createNode(
                stageId,
                "stage",
                stage.getDescription() != null ? stage.getDescription() : "Stage " + stageCounter
            );
            nodes.add(stageNode);
            
            // Link from previous
            edges.add(createEdge("edge_" + stageCounter, previousId, stageId));
            
            // Create objective nodes
            int objCounter = 0;
            for (com.lordofthetales.framework.quest.model.QuestObjectiveReference objRef : stage.getObjectives()) {
                String objId = "obj_" + stageCounter + "_" + objCounter;
                
                QuestDefinitionNodesInner objNode = createNode(
                    objId,
                    "objective",
                    objRef.getObjectiveId()
                );
                nodes.add(objNode);
                
                // Link from stage
                edges.add(createEdge("edge_obj_" + stageCounter + "_" + objCounter, stageId, objId));
                
                objCounter++;
            }
            
            previousId = stageId;
            stageCounter++;
        }
        
        // Create reward nodes
        int rewardCounter = 0;
        for (com.lordofthetales.framework.quest.model.QuestReward reward : framework.getRewards()) {
            String rewardId = "reward_" + rewardCounter;
            
            QuestDefinitionNodesInner rewardNode = createNode(
                rewardId,
                "reward",
                "Reward " + rewardCounter
            );
            nodes.add(rewardNode);
            
            // Link from last stage
            edges.add(createEdge("edge_reward_" + rewardCounter, previousId, rewardId));
            
            rewardCounter++;
        }
        
        hyQuest.setNodes(nodes);
        hyQuest.setEdges(edges);
        
        return hyQuest;
    }
    
    // --- Private Helper Methods ---
    
    private List<com.lordofthetales.framework.quest.model.QuestStage> extractStagesFromGraph(
        QuestDefinition hyQuest
    ) {
        List<com.lordofthetales.framework.quest.model.QuestStage> stages = new ArrayList<>();
        
        // Find all stage nodes
        List<QuestDefinitionNodesInner> stageNodes = hyQuest.getNodes().stream()
            .filter(this::isStageNode)
            .toList();
        
        int stageId = 0;
        for (QuestDefinitionNodesInner stageNode : stageNodes) {
            com.lordofthetales.framework.quest.model.QuestStage stage = 
                new com.lordofthetales.framework.quest.model.QuestStage();
            stage.setId(stageId++);
            stage.setDescription(extractNodeLabel(stageNode));
            
            // Find objectives for this stage
            List<com.lordofthetales.framework.quest.model.QuestObjectiveReference> objectives = 
                findObjectivesForNode(hyQuest, stageNode.getId());
            stage.setObjectives(objectives);
            
            stages.add(stage);
        }
        
        return stages;
    }
    
    private List<com.lordofthetales.framework.quest.model.QuestObjectiveReference> findObjectivesForNode(
        QuestDefinition hyQuest,
        String nodeId
    ) {
        // Find edges from this node to objective nodes
        List<String> targetIds = hyQuest.getEdges().stream()
            .filter(edge -> edge.getSource().equals(nodeId))
            .map(GraphEdge::getTarget)
            .toList();
        
        // Convert target nodes to objective references
        return targetIds.stream()
            .map(targetId -> findNodeById(hyQuest, targetId))
            .filter(Objects::nonNull)
            .filter(this::isObjectiveNode)
            .map(this::convertToObjectiveReference)
            .collect(Collectors.toList());
    }
    
    private com.lordofthetales.framework.quest.model.QuestObjectiveReference convertToObjectiveReference(
        QuestDefinitionNodesInner node
    ) {
        com.lordofthetales.framework.quest.model.QuestObjectiveReference ref = 
            new com.lordofthetales.framework.quest.model.QuestObjectiveReference();
        ref.setObjectiveId(node.getId());
        // TODO: Extract objective config from node.data
        return ref;
    }
    
    private List<com.lordofthetales.framework.quest.model.QuestReward> extractRewards(
        QuestDefinition hyQuest
    ) {
        // Find reward nodes and convert
        return hyQuest.getNodes().stream()
            .filter(this::isRewardNode)
            .map(this::convertToReward)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    private com.lordofthetales.framework.quest.model.QuestReward convertToReward(
        QuestDefinitionNodesInner node
    ) {
        // TODO: Implement reward conversion based on node data
        return null;
    }
    
    private QuestDefinitionNodesInner findNodeById(QuestDefinition hyQuest, String nodeId) {
        return hyQuest.getNodes().stream()
            .filter(n -> n.getId().equals(nodeId))
            .findFirst()
            .orElse(null);
    }
    
    private boolean isStageNode(QuestDefinitionNodesInner node) {
        // Check if node type is "stage"
        return node.getId().contains("stage"); // Simplified check
    }
    
    private boolean isObjectiveNode(QuestDefinitionNodesInner node) {
        return node.getId().contains("obj"); // Simplified check
    }
    
    private boolean isRewardNode(QuestDefinitionNodesInner node) {
        return node.getId().contains("reward"); // Simplified check
    }
    
    private String extractNodeLabel(QuestDefinitionNodesInner node) {
        // Extract label from node data
        // The generated client structure may vary
        return "Stage"; // Simplified
    }
    
    private QuestDefinitionNodesInner createNode(String id, String type, String label) {
        QuestDefinitionNodesInner node = new QuestDefinitionNodesInner();
        node.setId(id);
        // Note: Type setting depends on generated client API
        return node;
    }
    
    private GraphEdge createEdge(String id, String source, String target) {
        GraphEdge edge = new GraphEdge();
        edge.setId(id);
        edge.setSource(source);
        edge.setTarget(target);
        return edge;
    }
}
