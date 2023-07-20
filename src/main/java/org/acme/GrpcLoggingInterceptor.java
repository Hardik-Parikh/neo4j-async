package org.acme;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.quarkus.grpc.GlobalInterceptor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Prioritized;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Global Interceptor for logging of every request and response to any gRPC method.
 */
@GlobalInterceptor
@ApplicationScoped
public class GrpcLoggingInterceptor implements ServerInterceptor, Prioritized {

    @Inject
    Logger logger;

    /**
     * This method logs request and response for each gRPC endpoint.
     */
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall,
            Metadata metadata,
            ServerCallHandler<ReqT, RespT> serverCallHandler) {

        // This will have the name of the rpc method invoked.
        String methodInvoked = serverCall.getMethodDescriptor().getFullMethodName();

        logger.infof("Request for method %s received.", methodInvoked);

        // Define custom listener to log response from gRPC method.
        ServerCall.Listener<ReqT> listener = serverCallHandler.startCall(serverCall, metadata);
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
            @Override
            public void onComplete() {
                logger.debugf("Response for %s method: COMPLETED.", methodInvoked);
            }

            @Override
            public void onCancel() {
                logger.debugf("Response for %s method: CANCELED", methodInvoked);
            }
        };
    }

    /**
     * Priority is used for ordering the grpc interceptors.
     *
     * @return Interceptor Priority
     */
    @Override
    public int getPriority() {
        // Ref: https://quarkus.io/guides/grpc-service-implementation#server-interceptors
        return 10;
    }
}
