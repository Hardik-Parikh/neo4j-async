package org.acme;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.acme.grpc.HelloGrpcAsync;
import org.acme.grpc.HelloReply;
import org.acme.grpc.HelloRequest;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.logging.Logger;
import org.neo4j.driver.Driver;
import org.neo4j.driver.async.AsyncSession;

@GrpcService
public class HelloMutiny implements HelloGrpcAsync {
    @Inject
    Driver driver;

    @Inject
    Logger logger;

@Override
public Uni<HelloReply> asyncSayHello(HelloRequest request) {
    logger.error(Thread.currentThread());
    AsyncSession session = driver.session(AsyncSession.class);
    CompletionStage<List<Fruit>> cs = session
            .executeReadAsync(tx -> tx
                    .runAsync("CALL apoc.util.sleep(%s) MATCH (f:Fruit) RETURN f ORDER BY f.name".formatted(request.getQuerySleepTime()))
                    .thenCompose(cursor -> cursor
                            .listAsync(record -> Fruit.from(record.get("f").asNode()))));
    return Uni.createFrom().completionStage(cs)
            .map(fruits -> {
                List<org.acme.grpc.Fruit> fruitList = fruits.stream().map(fruit -> org.acme.grpc.Fruit.newBuilder().setId(fruit.getId())
                        .setName(fruit.getName()).build()).collect(Collectors.toList());
                return HelloReply.newBuilder().addAllFruits(fruitList).build();
            });
}
}
