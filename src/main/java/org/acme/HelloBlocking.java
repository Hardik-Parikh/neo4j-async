package org.acme;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import org.acme.grpc.HelloGrpcSyncGrpc;
import org.acme.grpc.HelloReply;
import org.acme.grpc.HelloRequest;
import org.jboss.logging.Logger;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

@GrpcService
public class HelloBlocking extends HelloGrpcSyncGrpc.HelloGrpcSyncImplBase {
    @Inject
    Driver driver;

    @Inject
    Logger logger;

@Override
@Blocking
public void syncSayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
    Session session = driver.session();
    logger.error(Thread.currentThread());

    Result result = session.run("CALL apoc.util.sleep(%s) MATCH (f:Fruit) RETURN f ORDER BY f.name".formatted(request.getQuerySleepTime()));
    List<Fruit> f = result.list().stream().map(record -> Fruit.from(record.get("f").asNode())).collect(Collectors.toList());
    responseObserver.onNext(
            HelloReply.newBuilder()
                    .addAllFruits(f.stream().map(fruit -> org.acme.grpc.Fruit.newBuilder().setId(fruit.getId())
                            .setName(fruit.getName()).build()).collect(Collectors.toList()))
                    .build());
    responseObserver.onCompleted();
}
}
