package net.daum.neo4j.upt;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;

public class Neo4jTutorial {
    public static final String databasePath = "/Users/jinsoohan/Documents/study/graph";

    public static void main(String[] args) {
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
