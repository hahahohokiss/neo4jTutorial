import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;

import java.util.ArrayList;
import java.util.List;

public class TestNeo4jTutorial {
    public static final String DB_PATH = "/Users/jinsoohan/Documents/study/graph";

    public static final String USERNAME_KEY = "username";

    public static GraphDatabaseService graphDb;

    @Before
    public void setUp() throws Exception {
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(DB_PATH)
                .setConfig(GraphDatabaseSettings.nodestore_mapped_memory_size, "10M")
                .setConfig(GraphDatabaseSettings.string_block_size, "60")
                .setConfig(GraphDatabaseSettings.array_block_size, "300").newGraphDatabase();
        registerShutdownHook(graphDb);
    }

    @Test
    public void testNeo4jHelloWorld() throws Exception {
        Node firstNode;
        Node secondNode;
        Relationship relationship;
        try (Transaction tx = graphDb.beginTx()) {
            firstNode = graphDb.createNode();
            firstNode.setProperty("message", "Hello, ");
            secondNode = graphDb.createNode();
            secondNode.setProperty("message", "World!");

            relationship = firstNode.createRelationshipTo(secondNode, RelTypes.KNOWS);
            relationship.setProperty("message", "brave Neo4j ");

            System.out.println(firstNode.getProperty("message"));
            System.out.println(relationship.getProperty("message"));
            System.out.println(secondNode.getProperty("message"));

            firstNode.getSingleRelationship(RelTypes.KNOWS, Direction.OUTGOING).delete();
            firstNode.delete();
            secondNode.delete();

            tx.success();
        }
    }

    @Test
    public void testNeo4jWithIndexCreateIndex() throws Exception {
        IndexDefinition indexDefinition;
        try (Transaction tx = graphDb.beginTx()) {
            Schema schema = graphDb.schema();
            indexDefinition = schema.indexFor(DynamicLabel.label("User")).on("username").create();
            tx.success();
        }

    }

    @Test
    public void testNeo4jWithIndexCreateUser() throws Exception {
        try (Transaction tx = graphDb.beginTx()) {
            Label label = DynamicLabel.label("User");

            // Create some users
            for (int id = 0; id < 100; id++) {
                Node userNode = graphDb.createNode(label);
                userNode.setProperty("username", "user" + id + "@neo4j.org");
            }
            System.out.println("Users created");
            tx.success();
        }

    }

    @Test
    public void testNeo4jWithIndexSearchUser() throws Exception {
        try (Transaction tx = graphDb.beginTx()) {
            Label label = DynamicLabel.label("User");
            int idToFind = 45;
            String nameToFind = "user" + idToFind + "@neo4j.org";

            ResourceIterator<Node> users = graphDb.findNodesByLabelAndProperty(label, "username", nameToFind).iterator();

            List<Node> userNodes = new ArrayList<>();

            while (users.hasNext()) {
                userNodes.add(users.next());
            }

            for (Node node : userNodes) {
                System.out.println("The username of user" + idToFind + "is" + node.getProperty("username"));
            }
            tx.success();
        }
    }

    @Test
    public void testNeo4jWithIndexDeleteUser() throws Exception {

        try (Transaction tx = graphDb.beginTx()) {
            Label label = DynamicLabel.label("User");
            for (int id = 0; id < 100; id++) {
                String nameToFind = "user" + id + "@neo4j.org";

                for (Node node : graphDb.findNodesByLabelAndProperty(label, "username", nameToFind)) {
                    node.delete();
                }
            }

            tx.success();
        }
    }

    @Test
    public void testNeo4jWithIndexDeleteIndex() throws Exception {
        try (Transaction tx = graphDb.beginTx()) {
            Label label = DynamicLabel.label("User");
            for (IndexDefinition indexDefinition : graphDb.schema().getIndexes(label)) {
                // There is only one index
                indexDefinition.drop();
            }
            tx.success();
        }
    }

    private static Index<Node> nodeIndex;
    @Test
    public void testNeo4jWithLegacyIndex() throws Exception {
        try (Transaction tx = graphDb.beginTx()) {
            nodeIndex = graphDb.index().forNodes("nodes");
            for (int id = 0; id < 100; id++) {
                createAndIndexUser(idToUserName(id));
            }

            int idToFind = 45;
            String userName = idToUserName(idToFind);
            Node foundUser = nodeIndex.get(USERNAME_KEY, userName).getSingle();
            System.out.println("The username of user " + idToFind + " is " + foundUser.getProperty(USERNAME_KEY));
            for (Node user : nodeIndex.query(USERNAME_KEY, "*")) {
                nodeIndex.remove(user, USERNAME_KEY,
                        user.getProperty(USERNAME_KEY));
                user.delete();
            }
            tx.success();
        }

    }

    private static Node createAndIndexUser( final String username ) {
        Node node = graphDb.createNode();
        node.setProperty(USERNAME_KEY, username);
        nodeIndex.add(node, USERNAME_KEY, username);
        return node;
    }

    private static String idToUserName( final int id ) {
        return "user" + id + "@neo4j.org";
    }

    @After
    public void tearDown() throws Exception {
        graphDb.shutdown();
    }

    private static enum RelTypes implements RelationshipType {
        KNOWS
    }

    private static void registerShutdownHook(final GraphDatabaseService grapDb) {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                grapDb.shutdown();
            }
        });
    }
}
