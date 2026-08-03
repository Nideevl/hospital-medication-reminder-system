package com.medreminder.callservice.config;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@Slf4j
public class GrpcServerConfig {

    @Bean
    public Server grpcServer() throws IOException {
        Server server = ServerBuilder
                .forPort(9003)
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Stopping gRPC server...");
            server.shutdown();
        }));

        server.start();
        log.info("gRPC Server started on port 9003");
        return server;
    }
}
