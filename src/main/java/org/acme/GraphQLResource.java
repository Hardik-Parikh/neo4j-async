package org.acme;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.logging.Logger;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.async.AsyncSession;

@ApplicationScoped
@GraphQLApi
public class GraphQLResource {

    @Inject
    Logger logger;

    @Inject
    Driver driver;

@Query
@Blocking
public List<Fruit> getFruitBlocking(int sleepTime) {
    logger.error(Thread.currentThread());
    Session session = driver.session();
    Result result = session.run("CALL apoc.util.sleep(%s) MATCH (f:Fruit) RETURN f ORDER BY f.name".formatted(sleepTime));
    return result.list().stream().map(record -> Fruit.from(record.get("f").asNode())).collect(Collectors.toList());
}

@Query
public Uni<List<Fruit>> getFruitUni(int sleepTime) {
    logger.error(Thread.currentThread());
    AsyncSession session = driver.session(AsyncSession.class);
    CompletionStage<List<Fruit>> cs = session
            .executeReadAsync(tx -> tx
                    .runAsync("CALL apoc.util.sleep(%s) MATCH (f:Fruit) RETURN f ORDER BY f.name".formatted(sleepTime))
                    .thenCompose(cursor -> cursor
                            .listAsync(record -> Fruit.from(record.get("f").asNode()))));
    return Uni.createFrom().completionStage(cs);
}
}
