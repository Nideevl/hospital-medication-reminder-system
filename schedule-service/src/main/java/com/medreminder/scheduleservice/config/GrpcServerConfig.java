package com.medreminder.scheduleservice.config;

import com.medreminder.common.util.Constants;
import com.medreminder.scheduleservice.grpc.ScheduleGrpcService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class GrpcServerConfig {
    private static final Logger log = LoggerFactory.getLogger(GrpcServerConfig.class);

    @Autowired
    private ScheduleGrpcService scheduleGrpcService;

    private Server grpcServer;

    @Bean
    public Server grpcServer() throws IOException {
        int port = Constants.GRPC_SERVICE_SCHEDULE_PORT;
        grpcServer = NettyServerBuilder.forPort(port)
                .addService(scheduleGrpcService)
                .build();
        grpcServer.start();
        log.info("gRPC server started on port: {}", port);
        log.info("ScheduleService registered successfully");
        return grpcServer;
    }

    @PreDestroy
    public void shutdownGrpcServer() {
        if (grpcServer != null && !grpcServer.isShutdown()) {
            grpcServer.shutdown();
            log.info("gRPC server shutdown gracefully");
        }
    }
}