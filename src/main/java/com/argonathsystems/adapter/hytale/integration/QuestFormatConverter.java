package com.argonathsystems.adapter.hytale.integration;

import com.argonathsystems.hyquest.model.QuestDefinition;
import com.argonathsystems.hyquest.model.QuestMetadata;
import com.argonathsystems.hyquest.model.QuestDefinitionNodesInner;
import com.argonathsystems.hyquest.model.GraphEdge;
import com.argonathsystems.hyquest.model.GraphNode;
import com.argonathsystems.hyquest.model.GraphNodePosition;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts between HyQuestUI format (graph-based with nodes/edges) 
 * and Quest Framework format (linear stages with objectives).
 * 
 * <h2>Conversion Strategy:</h2>
 * <p>The WebUI uses a graph-based format with React Flow nodes and edges,
 * while the game framework uses a linear stage-based format. This converter
 * handles the bidirectional transformation.
 * 
 * <h3>Graph → Linear (WebUI → Game)</h3>
 * <ol>
 *   <li>Find questRoot node from nodes list</li>
 *   <li>Follow edges to stage nodes in order</li>
 *   <li>For each stage, collect connected objective nodes</li>
 *   <li>Collect reward nodes and map to QuestReward</li>
 *   <li>Map metadata to QuestDefinition properties</li>
 * </ol>
 * 
 * <h3>Linear → Graph (Game → WebUI)</h3>
 * <ol>
 *   <li>Create questRoot node at origin</li>
 *   <li>Create stage nodes with vertical layout</li>
 *   <li>Create objective nodes branching from stages</li>
 *   <li>Create reward node at end</li>
 *   <li>Create edges connecting the nodes</li>
 * </ol>
 * 
 * <h2>Specification Reference:</h2>
 * <ul>
 *   <li>SF-QUEST-015: Quest Framework</li>
 *   <li>SF-QUEST-052: Quest Format Conversion</li>
 * </ul>
 * 
 * @version 2.0.0
 * @author Argonath Systems
 * @see com.argonathsystems.hyquest.model.QuestDefinition
 * @see com.lordofthetales.framework.quest.model.QuestDefinition
 */
public class QuestFormatConverter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestFormatConverter.class);
    
    /** Vertical spacing between nodes in the React Flow layout */
    private static final int NODE_VERTICAL_SPACING = 150;
    
    /** Horizontal spacing between nodes in the React Flow layout */
    private static final int NODE_HORIZONTAL_SPACING = 200;
    
    /** Horizontal offset for objective nodes branching from stages */
    private static final int OBJECTIVE_OFFSET = 250;
    
    /**
     * Convert HyQuestUI graph format to Quest Framework linear format.
     * 
     * <h3>Conversion Algorithm:</h3>
     * <ol>
     *   <li>Extract metadata (title, description, category, level requirements)</li>
     *   <li>Build node index from nodes list</li>
     *   <li>Build edge adjacency maps (source→target, target→source)</li>
     *   <li>Find questRoot node by type</li>
     *   <li>BFS traversal following edges to build linear stages</li>
     *   <li>Map node types: stage → QuestStage, objective → QuestObjective</li>
     *   <li>Collect rewards from rewardNode nodes</li>
     * </ol>
     * 
     * @param hyQuest HyQuestUI quest definition with nodes and edges
     * @return Quest Framework definition with stages
     * @throws ConversionException if the graph structure is invalid
     */
    @SuppressWarnings("unchecked")
    public com.lordofthetales.framework.quest.model.QuestDefinition toFrameworkFormat(
        QuestDefinition hyQuest
    ) {
        // Validate input
        List<String> errors = validateGraphStructure(hyQuest);
        if (!errors.isEmpty()) {
            throw new ConversionException("Invalid graph structure: " + String.join("; ", errors));
        }
        
        // Build node index by ID
        Map<String, Object> nodeIndex = new LinkedHashMap<>();
        for (var node : hyQuest.getNodes()) {
            // QuestDefinitionNodesInner is a oneOf — extract as map-like object
            String nodeId = extractNodeId(node);
            if (nodeId != null) {
                nodeIndex.put(nodeId, node);
            }
        }
        
        // Build edge adjacency maps (source → list of target node IDs)
        Map<String, List<String>> forwardEdges = new LinkedHashMap<>();
        Map<String, List<String>> reverseEdges = new LinkedHashMap<>();
        if (hyQuest.getEdges() != null) {
            for (GraphEdge edge : hyQuest.getEdges()) {
                String source = edge.getSource();
                String target = edge.getTarget();
                if (source != null && target != null) {
                    forwardEdges.computeIfAbsent(source, k -> new ArrayList<>()).add(target);
                    reverseEdges.computeIfAbsent(target, k -> new ArrayList<>()).add(source);
                }
            }
        }
        
        // Find questRoot node
        String rootNodeId = findNodeByType(nodeIndex, "questRoot");
        if (rootNodeId == null) {
            throw new ConversionException("No questRoot node found in graph");
        }
        
        // Extract metadata from hyQuest
        QuestMetadata metadata = hyQuest.getMetadata();
        
        // Create framework quest definition
        var frameworkQuest = new com.lordofthetales.framework.quest.model.QuestDefinition();
        frameworkQuest.setId(hyQuest.getId());
        // Title is in metadata, not on QuestDefinition directly
        if (metadata != null && metadata.getTitle() != null) {
            frameworkQuest.setTitle(metadata.getTitle());
        }
        
        // Map metadata fields
        if (metadata != null) {
            frameworkQuest.setDescription(metadata.getDescription());
            frameworkQuest.setCategory(mapCategory(metadata.getCategory()));
            
            if (metadata.getMinLevel() != null) {
                frameworkQuest.setMinLevel(metadata.getMinLevel());
            }
            if (metadata.getMaxLevel() != null) {
                frameworkQuest.setMaxLevel(metadata.getMaxLevel());
            }
            
            // Map repeatability: HyQuest frequency → Framework repeatable + repeatCooldown
            String frequency = metadata.getFrequency() != null ? 
                metadata.getFrequency().getValue() : null;
            if (frequency != null) {
                switch (frequency.toUpperCase()) {
                    case "ONCE" -> {
                        frameworkQuest.setRepeatable(false);
                        frameworkQuest.setMaxCompletions(1);
                    }
                    case "DAILY" -> {
                        frameworkQuest.setRepeatable(true);
                        frameworkQuest.setRepeatCooldownSeconds(86400); // 24h
                    }
                    case "WEEKLY" -> {
                        frameworkQuest.setRepeatable(true);
                        frameworkQuest.setRepeatCooldownSeconds(604800); // 7d
                    }
                    case "REPEATABLE" -> {
                        frameworkQuest.setRepeatable(true);
                        // Use cooldown from metadata if specified (convert minutes → seconds)
                        if (metadata.getCooldown() != null) {
                            frameworkQuest.setRepeatCooldownSeconds(metadata.getCooldown() * 60);
                        } else {
                            frameworkQuest.setRepeatCooldownSeconds(-1); // No cooldown
                        }
                    }
                    default -> frameworkQuest.setRepeatable(false);
                }
            }
            
            // Note: NPC quest giver not mapped — HyQuest WebUI metadata doesn't have an NPC field.
            // NPC association is handled at the quest stage/trigger level.
        }
        
        // BFS traversal from root to build stage list
        List<com.lordofthetales.framework.quest.model.QuestStage> stages = new ArrayList<>();
        List<com.lordofthetales.framework.quest.model.QuestReward> rewards = new ArrayList<>();
        
        // Follow edges from root to find stage nodes in order
        List<String> stageOrder = new ArrayList<>();
        Queue<String> queue = new LinkedList<>(
            forwardEdges.getOrDefault(rootNodeId, Collections.emptyList())
        );
        Set<String> visited = new HashSet<>();
        visited.add(rootNodeId);
        
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            if (visited.contains(currentId)) continue;
            visited.add(currentId);
            
            String nodeType = extractNodeType(nodeIndex.get(currentId));
            
            if ("stage".equals(nodeType) || "questStage".equals(nodeType)) {
                stageOrder.add(currentId);
            } else if ("reward".equals(nodeType) || "rewardNode".equals(nodeType)) {
                // Extract reward data from node
                com.lordofthetales.framework.quest.model.QuestReward reward = 
                    extractReward(nodeIndex.get(currentId));
                if (reward != null) {
                    rewards.add(reward);
                }
            }
            
            // Continue BFS to next connected nodes
            List<String> nextNodes = forwardEdges.getOrDefault(currentId, Collections.emptyList());
            queue.addAll(nextNodes);
        }
        
        // Build stages from ordered stage nodes
        int stageNumber = 1;
        for (String stageNodeId : stageOrder) {
            var stage = new com.lordofthetales.framework.quest.model.QuestStage();
            stage.setId(stageNumber++);
            
            // Extract stage data from node
            Object stageNode = nodeIndex.get(stageNodeId);
            Map<String, Object> stageData = extractNodeData(stageNode);
            if (stageData != null) {
                stage.setTitle((String) stageData.getOrDefault("label", "Stage " + stage.getId()));
                stage.setDescription((String) stageData.get("description"));
            }
            
            // Find connected objective nodes
            List<String> objectiveNodeIds = forwardEdges.getOrDefault(stageNodeId, Collections.emptyList());
            List<com.lordofthetales.framework.quest.model.QuestObjectiveReference> objectives = new ArrayList<>();
            
            for (String objNodeId : objectiveNodeIds) {
                String objType = extractNodeType(nodeIndex.get(objNodeId));
                if ("objective".equals(objType)) {
                    var objective = extractObjective(nodeIndex.get(objNodeId));
                    if (objective != null) {
                        objectives.add(objective);
                    }
                }
            }
            
            stage.setObjectives(objectives);
            stages.add(stage);
        }
        
        frameworkQuest.setStages(stages);
        frameworkQuest.setRewardsList(rewards);
        
        return frameworkQuest;
    }
    
    /**
     * Convert Quest Framework linear format to HyQuestUI graph format.
     * 
     * <h3>Conversion Algorithm:</h3>
     * <ol>
     *   <li>Create questRoot node at position (0, 0)</li>
     *   <li>For each stage, create stage node with vertical offset</li>
     *   <li>For each objective in stage, create objective node branching horizontally</li>
     *   <li>Create rewardNode at the end of the graph</li>
     *   <li>Create edges connecting: root→stages, stages→objectives, stages→next_stage</li>
     *   <li>Map framework metadata to QuestMetadata</li>
     * </ol>
     * 
     * @param framework Quest Framework definition with stages
     * @return HyQuestUI definition with nodes and edges
     * @throws ConversionException if the framework definition is invalid
     */
    public QuestDefinition toHyQuestFormat(
        com.lordofthetales.framework.quest.model.QuestDefinition framework
    ) {
        if (framework == null || framework.getId() == null) {
            throw new ConversionException("Framework quest definition must have an ID");
        }
        
        QuestDefinition hyQuest = new QuestDefinition();
        hyQuest.setId(framework.getId());
        
        // Map metadata
        QuestMetadata metadata = new QuestMetadata();
        metadata.setTitle(framework.getTitle());
        metadata.setDescription(framework.getDescription());
        metadata.setCategory(framework.getCategory() != null ? framework.getCategory().name() : "SIDE");
        
        if (framework.getMinLevel() != null) {
            metadata.setMinLevel(framework.getMinLevel());
        }
        if (framework.getMaxLevel() != null) {
            metadata.setMaxLevel(framework.getMaxLevel());
        }
        
        // Map repeatability: Framework → HyQuest frequency
        if (!framework.isRepeatable()) {
            metadata.setFrequency(QuestMetadata.FrequencyEnum.ONCE);
        } else {
            int cooldownSec = framework.getRepeatCooldownSeconds();
            if (cooldownSec >= 604800) {
                metadata.setFrequency(QuestMetadata.FrequencyEnum.WEEKLY);
            } else if (cooldownSec >= 86400) {
                metadata.setFrequency(QuestMetadata.FrequencyEnum.DAILY);
            } else {
                metadata.setFrequency(QuestMetadata.FrequencyEnum.REPEATABLE);
                if (cooldownSec > 0) {
                    metadata.setCooldown(cooldownSec / 60); // seconds → minutes
                }
            }
        }
        
        // Note: questGiverNpcId not mapped — HyQuest WebUI metadata doesn't have an NPC field.
        // NPC association is handled at the quest stage/trigger level.
        
        hyQuest.setMetadata(metadata);
        
        // Build nodes and edges
        List<QuestDefinitionNodesInner> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        
        int edgeCounter = 0;
        
        // Create questRoot node
        String rootId = "root_" + framework.getId();
        GraphNode rootNode = new GraphNode();
        rootNode.setId(rootId);
        rootNode.setType(GraphNode.TypeEnum.QUEST_ROOT);
        rootNode.setPosition(createPosition(0, 0));
        rootNode.setData(new com.argonathsystems.hyquest.model.GraphNodeData()
            .label(framework.getTitle() != null ? framework.getTitle() : framework.getId()));
        nodes.add(wrapNode(rootNode));
        
        String previousNodeId = rootId;
        int yOffset = NODE_VERTICAL_SPACING;
        
        // Create stage nodes
        List<com.lordofthetales.framework.quest.model.QuestStage> stages = framework.getStages();
        if (stages != null) {
            for (var stage : stages) {
                String stageId = "stage_" + stage.getId();
                
                GraphNode stageNode = new GraphNode();
                stageNode.setId(stageId);
                stageNode.setType(GraphNode.TypeEnum.STAGE);
                stageNode.setPosition(createPosition(0, yOffset));
                stageNode.setData(new com.argonathsystems.hyquest.model.GraphNodeData()
                    .label(stage.getTitle() != null ? stage.getTitle() : "Stage " + stage.getId()));
                nodes.add(wrapNode(stageNode));
                
                // Edge from previous stage (or root) to this stage
                edges.add(createEdge("edge_" + (edgeCounter++), previousNodeId, stageId, "stage_flow"));
                
                // Create objective nodes branching from this stage
                if (stage.getObjectives() != null) {
                    int xOffset = OBJECTIVE_OFFSET;
                    for (int i = 0; i < stage.getObjectives().size(); i++) {
                        var objective = stage.getObjectives().get(i);
                        String objId = stageId + "_obj_" + i;
                        
                        GraphNode objNode = new GraphNode();
                        objNode.setId(objId);
                        objNode.setType(GraphNode.TypeEnum.OBJECTIVE);
                        objNode.setPosition(createPosition(xOffset, yOffset));
                        
                        // Build config map for GraphNodeData
                        Map<String, Object> objConfig = new LinkedHashMap<>();
                        objConfig.put("type", objective.getType());
                        // Extract target from objective config if present
                        if (objective.getConfig() != null) {
                            objective.getConfig().forEach((k, v) -> objConfig.put(k, v.asString().orElse(v.toString())));
                        }
                        objConfig.put("count", objective.getCount());
                        if (objective.getDescription() != null) {
                            objConfig.put("description", objective.getDescription());
                        }
                        objNode.setData(new com.argonathsystems.hyquest.model.GraphNodeData()
                            .label(objective.getDescription() != null ? objective.getDescription() : objective.getType())
                            .config(objConfig));
                        nodes.add(wrapNode(objNode));
                        
                        // Edge from stage to objective
                        edges.add(createEdge("edge_" + (edgeCounter++), stageId, objId, "objective"));
                        
                        xOffset += NODE_HORIZONTAL_SPACING;
                    }
                }
                
                previousNodeId = stageId;
                yOffset += NODE_VERTICAL_SPACING;
            }
        }
        
        // Create reward nodes
        List<com.lordofthetales.framework.quest.model.QuestReward> rewards = framework.getRewardsList();
        if (rewards != null && !rewards.isEmpty()) {
            for (int i = 0; i < rewards.size(); i++) {
                var reward = rewards.get(i);
                String rewardId = "reward_" + i;
                
                GraphNode rewardNode = new GraphNode();
                rewardNode.setId(rewardId);
                rewardNode.setType(GraphNode.TypeEnum.REWARD);
                rewardNode.setPosition(createPosition(0, yOffset));
                
                Map<String, Object> rewardConfig = new LinkedHashMap<>();
                rewardConfig.put("rewardType", reward.getType() != null ? reward.getType().name() : "ITEM");
                rewardConfig.put("rewardId", reward.getId());
                rewardConfig.put("amount", reward.getAmount());
                rewardNode.setData(new com.argonathsystems.hyquest.model.GraphNodeData()
                    .label(reward.getId() != null ? reward.getId() : "Reward")
                    .config(rewardConfig));
                nodes.add(wrapNode(rewardNode));
                
                // Edge from last stage to reward
                edges.add(createEdge("edge_" + (edgeCounter++), previousNodeId, rewardId, "reward"));
                
                yOffset += NODE_VERTICAL_SPACING / 2;
            }
        }
        
        hyQuest.setNodes(nodes);
        hyQuest.setEdges(edges);
        
        return hyQuest;
    }
    
    /**
     * Validate that a HyQuest graph has the required structure for conversion.
     * 
     * @param hyQuest the quest definition to validate
     * @return list of validation errors (empty if valid)
     */
    public List<String> validateGraphStructure(QuestDefinition hyQuest) {
        List<String> errors = new ArrayList<>();
        
        if (hyQuest == null) {
            errors.add("Quest definition cannot be null");
            return errors;
        }
        
        if (hyQuest.getNodes() == null || hyQuest.getNodes().isEmpty()) {
            errors.add("Quest must have at least one node");
        }
        
        if (hyQuest.getId() == null || hyQuest.getId().isBlank()) {
            errors.add("Quest must have an ID");
        }
        
        // Check for questRoot node
        if (hyQuest.getNodes() != null) {
            Map<String, Object> nodeIndex = new LinkedHashMap<>();
            for (var node : hyQuest.getNodes()) {
                String nodeId = extractNodeId(node);
                if (nodeId != null) {
                    nodeIndex.put(nodeId, node);
                }
            }
            
            String rootId = findNodeByType(nodeIndex, "questRoot");
            if (rootId == null) {
                errors.add("Quest must have a questRoot node");
            }
        }
        
        return errors;
    }
    
    // ========================================================================
    // Helper Methods — Node Extraction
    // ========================================================================
    
    /**
     * Extract node ID from a QuestDefinitionNodesInner (oneOf type).
     * The inner may be a GraphNode or a DialogNode — both have getId().
     */
    @SuppressWarnings("unchecked")
    private String extractNodeId(Object nodeWrapper) {
        if (nodeWrapper instanceof GraphNode gn) {
            return gn.getId();
        }
        // Fallback: try reflection or map-like access for generated oneOf types
        if (nodeWrapper instanceof Map<?,?> map) {
            return (String) map.get("id");
        }
        // QuestDefinitionNodesInner might delegate to getActualInstance()
        try {
            var method = nodeWrapper.getClass().getMethod("getId");
            return (String) method.invoke(nodeWrapper);
        } catch (Exception e) {
            LOGGER.debug("Could not extract node ID from {}: {}", nodeWrapper.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract node type from a node object.
     */
    private String extractNodeType(Object nodeWrapper) {
        if (nodeWrapper instanceof GraphNode gn) {
            var typeEnum = gn.getType();
            return typeEnum != null ? typeEnum.getValue() : null;
        }
        if (nodeWrapper instanceof Map<?,?> map) {
            return (String) map.get("type");
        }
        try {
            var method = nodeWrapper.getClass().getMethod("getType");
            return (String) method.invoke(nodeWrapper);
        } catch (Exception e) {
            LOGGER.debug("Could not extract node type from {}: {}", nodeWrapper.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract data map from a node object.
     * Handles both {@code GraphNodeData} (typed API model) and raw {@code Map} representations.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractNodeData(Object nodeWrapper) {
        if (nodeWrapper instanceof GraphNode gn) {
            var nodeData = gn.getData();
            if (nodeData != null) {
                // Convert GraphNodeData to a Map for uniform downstream processing
                Map<String, Object> result = new LinkedHashMap<>();
                if (nodeData.getLabel() != null) {
                    result.put("label", nodeData.getLabel());
                }
                if (nodeData.getConfig() instanceof Map<?,?> configMap) {
                    configMap.forEach((k, v) -> result.put(String.valueOf(k), v));
                }
                return result.isEmpty() ? null : result;
            }
        }
        if (nodeWrapper instanceof Map<?,?> map) {
            Object data = map.get("data");
            if (data instanceof Map) {
                return (Map<String, Object>) data;
            }
        }
        return null;
    }
    
    /**
     * Find first node ID with the given type.
     */
    private String findNodeByType(Map<String, Object> nodeIndex, String type) {
        for (Map.Entry<String, Object> entry : nodeIndex.entrySet()) {
            if (type.equals(extractNodeType(entry.getValue()))) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Extract a QuestObjectiveReference from an objective node.
     */
    @SuppressWarnings("unchecked")
    private com.lordofthetales.framework.quest.model.QuestObjectiveReference extractObjective(Object nodeWrapper) {
        Map<String, Object> data = extractNodeData(nodeWrapper);
        if (data == null) {
            return null;
        }
        
        var objective = new com.lordofthetales.framework.quest.model.QuestObjectiveReference();
        objective.setType((String) data.getOrDefault("type", "CUSTOM"));
        objective.setDescription((String) data.get("description"));
        
        // Store target in config map if present
        String target = (String) data.get("target");
        if (target != null) {
            var config = new java.util.HashMap<String, com.argonathsystems.framework.accessorapi.data.DataValue>();
            config.put("target", com.argonathsystems.framework.accessorapi.data.DataValue.of(target));
            objective.setConfig(config);
        }
        
        Object amount = data.get("amount");
        if (amount instanceof Number n) {
            objective.setCount(n.intValue());
        } else {
            objective.setCount(1);
        }
        
        return objective;
    }
    
    /**
     * Extract a QuestReward from a reward node.
     */
    @SuppressWarnings("unchecked")
    private com.lordofthetales.framework.quest.model.QuestReward extractReward(Object nodeWrapper) {
        Map<String, Object> data = extractNodeData(nodeWrapper);
        if (data == null) {
            return null;
        }
        
        var reward = new com.lordofthetales.framework.quest.model.QuestReward();
        
        String rewardTypeStr = (String) data.getOrDefault("rewardType", "ITEM");
        try {
            reward.setType(
                com.lordofthetales.framework.quest.model.QuestReward.RewardType.valueOf(rewardTypeStr.toUpperCase())
            );
        } catch (IllegalArgumentException e) {
            reward.setType(com.lordofthetales.framework.quest.model.QuestReward.RewardType.ITEM);
        }
        
        reward.setId((String) data.get("rewardId"));
        
        Object amount = data.get("amount");
        if (amount instanceof Number n) {
            reward.setAmount(n.intValue());
        } else {
            reward.setAmount(1);
        }
        
        return reward;
    }
    
    /**
     * Map category string to framework QuestCategory inner enum.
     * Categories are defined in {@code QuestDefinition.QuestCategory}.
     */
    private com.lordofthetales.framework.quest.model.QuestDefinition.QuestCategory mapCategory(String category) {
        if (category == null || category.isBlank()) {
            return com.lordofthetales.framework.quest.model.QuestDefinition.QuestCategory.SIDE;
        }
        try {
            return com.lordofthetales.framework.quest.model.QuestDefinition.QuestCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Unknown quest category '{}', defaulting to SIDE", category);
            return com.lordofthetales.framework.quest.model.QuestDefinition.QuestCategory.SIDE;
        }
    }
    
    // ========================================================================
    // Helper Methods — Node/Edge Creation
    // ========================================================================
    
    private GraphNodePosition createPosition(int x, int y) {
        GraphNodePosition pos = new GraphNodePosition();
        pos.setX(BigDecimal.valueOf(x));
        pos.setY(BigDecimal.valueOf(y));
        return pos;
    }
    
    private GraphEdge createEdge(String id, String source, String target, String label) {
        GraphEdge edge = new GraphEdge();
        edge.setId(id);
        edge.setSource(source);
        edge.setTarget(target);
        edge.setLabel(label);
        return edge;
    }
    
    /**
     * Wrap a GraphNode into QuestDefinitionNodesInner.
     * Since QuestDefinitionNodesInner is a oneOf union type generated by OpenAPI,
     * this creates the appropriate wrapper.
     */
    private QuestDefinitionNodesInner wrapNode(GraphNode node) {
        return new QuestDefinitionNodesInner(node);
    }
    
    /**
     * Exception thrown when quest format conversion fails.
     */
    public static class ConversionException extends RuntimeException {
        public ConversionException(String message) {
            super(message);
        }
        
        public ConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
