package casia.isiteam.zdr.neo4j.shortestPath;

import casia.isiteam.zdr.neo4j.procedures.CustomerProcedures;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.junit.Neo4jRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
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

/**
 * @author YanchaoMa yanchaoma@foxmail.com
 * @PACKAGE_NAME: casia.isiteam.zdr.neo4j.shortestPath
 * @Description: TODO(Describe the role of this JAVA class)
 * @date 2019/4/8 18:52
 */
public class ShortestPathTest {
    @Rule
    public Neo4jRule neo4j = new Neo4jRule()
            .withProcedure(ShortestPath.class);

    @Test
    public void allPathsTightness() {
        GraphDatabaseService db = neo4j.getGraphDatabaseService();

        try (Transaction tx = db.beginTx()) {

            List<Long> sourceIds = new ArrayList<>();
            sourceIds.add(123l);
            sourceIds.add(223l);
            sourceIds.add(223l);

            List<Long> targetIds = new ArrayList<>();
            targetIds.add(32423l);
            targetIds.add(4353l);
            targetIds.add(4353l);

            List<Double> distanceSTE = new ArrayList<>();
            distanceSTE.add(23.0);
            distanceSTE.add(324.0);
            distanceSTE.add(324.0);

            Map<String, Object> map = new HashMap<>();
            map.put("sourceIds", sourceIds);
            map.put("targetIds", targetIds);
            map.put("distanceSTE", distanceSTE);

            Result res = db.execute("CALL zdr.shortestPath.allPathsTightness({sourceIds},{targetIds},{distanceSTE}) YIELD source,target,distance,tightnessSort RETURN source,target,distance,tightnessSort", map);

            while (res.hasNext()) {
                Map map2 = res.next();
                System.out.println(map2.get("source") + " " + map2.get("target") + " " + map2.get("distance") + " " + map2.get("tightnessSort"));
            }

        }
    }

}