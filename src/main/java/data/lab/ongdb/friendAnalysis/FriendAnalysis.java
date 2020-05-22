package data.lab.ongdb.friendAnalysis;
/**
 * 　　　　　　　 ┏┓       ┏┓+ +
 * 　　　　　　　┏┛┻━━━━━━━┛┻┓ + +
 * 　　　　　　　┃　　　　　　 ┃
 * 　　　　　　　┃　　　━　　　┃ ++ + + +
 * 　　　　　　 █████━█████  ┃+
 * 　　　　　　　┃　　　　　　 ┃ +
 * 　　　　　　　┃　　　┻　　　┃
 * 　　　　　　　┃　　　　　　 ┃ + +
 * 　　　　　　　┗━━┓　　　 ┏━┛
 * ┃　　  ┃
 * 　　　　　　　　　┃　　  ┃ + + + +
 * 　　　　　　　　　┃　　　┃　Code is far away from     bug with the animal protecting
 * 　　　　　　　　　┃　　　┃ +
 * 　　　　　　　　　┃　　　┃
 * 　　　　　　　　　┃　　　┃　　+
 * 　　　　　　　　　┃　 　 ┗━━━┓ + +
 * 　　　　　　　　　┃ 　　　　　┣┓
 * 　　　　　　　　　┃ 　　　　　┏┛
 * 　　　　　　　　　┗┓┓┏━━━┳┓┏┛ + + + +
 * 　　　　　　　　　 ┃┫┫　 ┃┫┫
 * 　　　　　　　　　 ┗┻┛　 ┗┻┛+ + + +
 */

import data.lab.ongdb.result.NodeFriendCountList;
import data.lab.ongdb.result.NodeResult;
import data.lab.ongdb.util.NodeHandle;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * @author YanchaoMa yanchaoma@foxmail.com
 * @PACKAGE_NAME: casia.isiteam.zdr.neo4j.friendAnalysis
 * @Description: TODO(好友关系分析过程 / 函数)
 * @date 2019/3/30 17:05
 */
public class FriendAnalysis {
    /**
     * 运行环境/上下文
     */
    @Context
    public GraphDatabaseService db;

    @Context
    public Log log;

    /**
     * 统计两两好友之间的关系数量并动态更新节点属性
     * 1.Nodes list
     * WITH [60667,60652,60669,60635,80988] AS groupIds
     * MATCH (n) WHERE id(n) IN groupIds
     * WITH collect(n) AS nodes
     * UNWIND nodes AS source
     * UNWIND nodes AS target
     * WITH source,target WHERE id(source)<id(target)
     * MATCH paths=(source)-[:好友]-(target) WITH collect(source) AS sourceNodes,collect(target) AS targetNodes
     * CALL zdr.apoc.publicFriendAnalysis(sourceNodes,targetNodes) YIELD node RETURN node
     *
     * 2.Nodes id list
     * WITH [60667,60652,60669,60635,80988] AS groupIds
     * MATCH (n) WHERE id(n) IN groupIds
     * WITH collect(n) AS nodes
     * UNWIND nodes AS source
     * UNWIND nodes AS target
     * WITH source,target WHERE id(source)<id(target)
     * MATCH paths=(source)-[:好友]-(target) WITH collect(id(source)) AS sourceList,collect(id(target)) AS targetList
     * CALL zdr.apoc.publicFriendAnalysisMap(sourceList,targetList) YIELD list WITH list
     * UNWIND list AS row
     * MATCH (n) WHERE id(n)=row.id SET n.targetGroupFriendsRelaCount=row.count RETURN n
     * **/

    /**
     * @param
     * @return
     * @Description: TODO(两两之间的好友关系分析)
     */
    @Procedure(name = "zdr.apoc.publicFriendAnalysis", mode = Mode.WRITE)
    @Description("Public friend analysis")
    public Stream<NodeResult> publicFriendAnalysis(@Name("sourceList") List<Node> sourceList, @Name("targetList") List<Node> targetList) {

        List<Node> nodes = new ArrayList<>();

        // 合并节点集合
        nodes.addAll(sourceList);
        nodes.addAll(targetList);

        // 统计好友关系数
        HashMap<Long, Integer> countMap = countFriedsRela(nodes);

        // 节点集合排重
        NodeHandle nodeHandle = new NodeHandle();
        nodes = nodeHandle.distinctNodes(nodes);

        // 给节点更新属性并返回
        return returnFriendResultNodes(nodes, countMap);
    }

    /**
     * @param
     * @return
     * @Description: TODO(两两之间的好友关系分析)
     */
    @Procedure(name = "zdr.apoc.publicFriendAnalysisMap", mode = Mode.WRITE)
    @Description("Public friend analysis")
    public Stream<NodeFriendCountList> publicFriendAnalysisMap(@Name("sourceList") List<Long> sourceList, @Name("targetList") List<Long> targetList) {

        List<Long> nodes = new ArrayList<>();

        // 合并节点集合
        nodes.addAll(sourceList);
        nodes.addAll(targetList);

        // 统计好友关系数
        HashMap<Long, Integer> countMap = countFriedsRelaIds(nodes);

        // 返回节点ID，和对应的统计值MAP LIST
        return Stream.of(new NodeFriendCountList(returnFriendResultNodesById(countMap)));
    }

    /**
     * @param
     * @return
     * @Description: TODO(将统计结果更新为节点属性并返回节点集合)
     */
    private List<Map<String, Object>> returnFriendResultNodesById(HashMap<Long, Integer> countMap) {
        List<Map<String, Object>> list = new ArrayList<>();
        countMap.forEach((k, v) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", k);
            map.put("count", v);
            list.add(map);
        });
        return list;
    }

    /**
     * @param
     * @return
     * @Description: TODO(将统计结果更新为节点属性并返回节点集合)
     */
    private Stream<NodeResult> returnFriendResultNodes(List<Node> nodes, HashMap<Long, Integer> countMap) {
        List<NodeResult> output = new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            // 给节点更新属性 targetGroupFriendsRelaCount
            nodes.forEach(node -> {
                node.setProperty("targetGroupFriendsRelaCount", countMap.get(node.getId()));
                output.add(new NodeResult(node));
            });

            log.debug("Public friend analysis");
            tx.success();
        }
        return output.stream();
    }

    /**
     * @param
     * @return
     * @Description: TODO(统计好友关系数)
     */
    private HashMap<Long, Integer> countFriedsRela(List<Node> nodes) {
        HashMap<Long, Integer> countMap = new HashMap<>();
        nodes.forEach(node -> {
            long id = node.getId();
            if (countMap.containsKey(id)) {
                int count = countMap.get(id);
                count++;
                countMap.put(id, count);
            } else {
                countMap.put(id, 1);
            }
        });
        return countMap;
    }

    /**
     * @param
     * @return
     * @Description: TODO(统计好友关系数)
     */
    private HashMap<Long, Integer> countFriedsRelaIds(List<Long> nodes) {
        HashMap<Long, Integer> countMap = new HashMap<>();
        nodes.forEach(nodeId -> {
            long id = nodeId;
            if (countMap.containsKey(id)) {
                int count = countMap.get(id);
                count++;
                countMap.put(id, count);
            } else {
                countMap.put(id, 1);
            }
        });
        return countMap;
    }

    /**
     * @param relationships:当前路径中的关系集合
     * @param conditionRelas:当前路径中的必须包含的关系（任意一条路径中关系列表中某一个关系不包含在此列表中则返回FALSE）
     * @param node:目标节点
     * @param conditionLabels:目标节点必须满足的标签（任意满足一个即可）
     * @return 当前路径是否满足
     * @Description: TODO(通过关系和节点标签过滤路径 - 寻找满足条件的点)
     */
    @UserFunction(name = "zdr.apoc.targetNodesRelasFilter")
    @Description("Filter target nodes by labels and relationships")
    public boolean targetNodesRelasFilter(@Name("relationships") List<Relationship> relationships, @Name("conditionRelas") List<String> conditionRelas,
                                          @Name("node") Node node, @Name("conditionLabels") List<String> conditionLabels) {
        // 关系是否满足体条件
        boolean relaBool = isPathRelas(relationships, conditionRelas);

        // 节点是否满足条件
        boolean nodeBoll = isPathNode(node, conditionLabels);

        if (relaBool && nodeBoll) {
            return true;
        }
        return false;
    }

    /**
     * @param relationships:当前路径中的关系集合
     * @param conditionRelas:当前路径中的必须包含的关系（任意一条路径中关系列表中某一个关系不包含在此列表中则返回FALSE）(好友关系分析路径过滤)
     * @param relationshipName:关系名（例如关注关系）
     * @param key:被用来判断的键
     * @param value:被用来判断的值
     * @return 当前路径是否满足
     * @Description: TODO(通过关系过滤路径 - 寻找满足条件的点)
     */
    @UserFunction(name = "zdr.apoc.targetNodesRelasFilterByKey")
    @Description("Filter target nodes by labels and relationships")
    public boolean targetNodesRelasFilterByKey(@Name("relationships") List<Relationship> relationships, @Name("conditionRelas") List<String> conditionRelas,
                                               @Name("relationshipName") String relationshipName, @Name("key") String key,
                                               @Name("value") String value, @Name("countThreshold") Number countThreshold) {
        // 关系是否满足体条件
        boolean relaBool = isPathRelas(relationships, conditionRelas);

        // 某种关系是否满足条件 (此过滤条件是上一个条件之上的再次过滤)
        boolean relaKeyBool = isPathRelaKey(relationships, relationshipName, key, value, countThreshold.intValue());

        if (relaBool && relaKeyBool) {
            return true;
        }
        return false;
    }

    /**
     * @param
     * @return
     * @Description: TODO(某种关系是否满足条件 - 不满足条件返回false / 默认返回true)
     */
    private boolean isPathRelaKey(List<Relationship> relationships, String relationshipName, String key, String value, int countThreshold) {
        // 共同好友分析 - PATH必须有两条关注关系满足Friend属性
        int count = 0;
        for (int i = 0; i < relationships.size(); i++) {
            Relationship relationship = relationships.get(i);
            // 创建时默认NAME属性为关系TYPE展示名称
            if (relationship != null) {
                String relaName = relationship.getType().name();
                if (relaName.equals(relationshipName)) {
                    if (relationship.hasProperty(key)) {
                        Object valueObj = relationship.getProperty(key);
                        if (String.valueOf(valueObj).equals(value)) {
                            count++;
                        }
                    }
                }
            }
        }
        if (count >= countThreshold) {
            return true;
        }
        return false;
    }

    /**
     * @param relationships:当前路径中的关系集合
     * @param conditionRelas:当前路径中的必须包含的关系（过滤条件中任意一条关系包含在路径中则返回TRUE）
     * @return 当前路径是否满足
     * @Description: TODO(通过关系过滤路径 - 寻找满足关系条件的路径)
     */
    @UserFunction(name = "zdr.apoc.targetRelasIsContainsOneOrNot")
    @Description("Filter target nodes by labels and relationships")
    public boolean targetRelasIsContainsOneOrNot(@Name("relationships") List<Relationship> relationships, @Name("conditionRelas") List<String> conditionRelas) {
        // 关系是否满足体条件
        return isPathRelasContainsOne(relationships, conditionRelas);
    }

    /**
     * @param relationships:当前路径中的关系集合
     * @return conditionRelas:当前路径中的必须包含的关系 - CONDITIONS设置为NULL默认不过滤
     * @Description: TODO(过滤条件中任意一条关系包含在路径中则返回TRUE)
     */
    private boolean isPathRelasContainsOne(List<Relationship> relationships, List<String> conditionRelas) {
        if (conditionRelas == null) {
            return true;
        }
        List<String> rawRela = new ArrayList<>();
        for (int i = 0; i < relationships.size(); i++) {
            Relationship relationship = relationships.get(i);
            String relaName = relationship.getType().name();
            rawRela.add(relaName);
        }
        for (int i = 0; i < conditionRelas.size(); i++) {
            String relationship = conditionRelas.get(i);
            if (rawRela.contains(relationship)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param node:与源节点相连接的目标节点
     * @param conditionLabels:目标节点必须满足的标签（任意满足一个即可） - CONDITIONS设置为NULL默认不过滤
     * @return
     * @Description: TODO(过滤节点)
     */
    protected boolean isPathNode(Node node, List<String> conditionLabels) {
        if (conditionLabels == null) {
            return true;
        }
        Iterable<Label> labels = node.getLabels();
        for (Iterator<Label> iterator = labels.iterator(); iterator.hasNext(); ) {
            Label label = iterator.next();
            if (conditionLabels.contains(label.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param relationships:当前路径中的关系集合
     * @param conditionRelas:当前路径中的必须包含的关系 - CONDITIONS设置为NULL默认不过滤
     * @return
     * @Description: TODO(任意一条路径中关系列表中某一个关系不包含在此列表中则返回FALSE - relationships中每个关系必须包含在conditionRelas)
     */
    protected boolean isPathRelas(List<Relationship> relationships, List<String> conditionRelas) {
        if (conditionRelas == null) {
            return true;
        }
        for (int i = 0; i < relationships.size(); i++) {
            Relationship relationship = relationships.get(i);
            // 创建时默认NAME属性为关系TYPE展示名称
            if (relationship != null) {
                String relaName = relationship.getType().name();
                if (!conditionRelas.contains(relaName)) {
                    return false;
                }
            }
        }
        return true;
    }

}

