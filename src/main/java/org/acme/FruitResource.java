package org.acme;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Application;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

import java.util.stream.Collectors;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.logging.Logger;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.async.ResultCursor;
import org.neo4j.driver.exceptions.NoSuchRecordException;

@Path("/fruits")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FruitResource {

    @Inject
    Driver driver;

    @Inject
    ThreadContext threadContext;

    @Inject
    Logger logger;

@GET
public CompletionStage<Response> get() {
    int sleep = 30000;
    logger.debug("Async Request received");
    logger.error(Thread.currentThread());
    AsyncSession session = driver.session(AsyncSession.class);
    CompletionStage<List<Fruit>> cs = session
            .executeReadAsync(tx -> tx
                    .runAsync("CALL apoc.util.sleep(%s) MATCH (f:Fruit) RETURN f ORDER BY f.name".formatted(sleep))
                    .thenCompose(cursor -> cursor
                            .listAsync(record -> Fruit.from(record.get("f").asNode()))));
    return threadContext.withContextCapture(cs)
            .thenCompose(fruits -> session.closeAsync().thenApply(signal -> fruits))
            .thenApply(Response::ok)
            .thenApply(ResponseBuilder::build);
}

@GET
@Path("/blocking")
public Response getBlocking() {
    logger.error(Thread.currentThread());
    logger.debug("Sync Request received");
    int sleep = 30000;
    Session session = driver.session();
    Result result = session.run("CALL apoc.util.sleep(%s) MATCH (f:Fruit) RETURN f ORDER BY f.name".formatted(sleep));
    List<Fruit> f = result.list().stream().map(record -> Fruit.from(record.get("f").asNode())).collect(Collectors.toList());
    return Response.ok(f).build();
}

    @POST
    public CompletionStage<Response> create(Fruit fruit) {
        AsyncSession session = driver.session(AsyncSession.class);
        CompletionStage<Fruit> cs = session
                .executeWriteAsync(tx -> tx
                        .runAsync(
                                "CREATE (f:Fruit {id: randomUUID(), name: $name}) RETURN f",
                                Map.of("name", fruit.name))
                        .thenCompose(ResultCursor::singleAsync)
                        .thenApply(record -> Fruit.from(record.get("f").asNode())));
        return threadContext.withContextCapture(cs)
                .thenCompose(persistedFruit -> session
                        .closeAsync().thenApply(signal -> persistedFruit))
                .thenApply(persistedFruit -> Response
                        .created(URI.create("/fruits/" + persistedFruit.id))
                        .build());
    }


}
