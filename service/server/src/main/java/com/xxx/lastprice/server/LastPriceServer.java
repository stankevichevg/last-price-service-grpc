package com.xxx.lastprice.server;

import com.xxx.lastprice.domain.InMemoryBatchRunRepository;
import com.xxx.lastprice.domain.LastPriceService;
import com.xxx.lastprice.domain.LastPriceServiceImpl;
import com.xxx.lastprice.domain.PriceRecordContainer;
import com.xxx.lastprice.SystemEpochClock;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static com.xxx.lastprice.server.ServerConfiguration.ABANDONED_BATCH_RUN_TIMEOUT_MS;
import static com.xxx.lastprice.server.ServerConfiguration.BATCH_RUNS_CLEAN_UP_INTERVAL_MS;
import static com.xxx.lastprice.server.ServerConfiguration.MAX_ACTIVE_BATCH_RUNS_TARGET;
import static com.xxx.lastprice.server.ServerConfiguration.SERVER_PORT;
import static com.xxx.lastprice.server.ServerConfiguration.SUPPORTED_INSTRUMENTS;

/**
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public class LastPriceServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LastPriceServer.class);

    private ScheduledExecutorService executorService;
    private Server server;

    public void start() throws IOException, InterruptedException {
        executorService = Executors.newSingleThreadScheduledExecutor();
        server = ServerBuilder.forPort(SERVER_PORT)
            .addService(new LastPriceServiceHandler(provideLastPriceService(executorService)))
            .build()
            .start();
        LOGGER.info("Server started, listening on " + SERVER_PORT);
        setUpShutdownHook();
        blockUntilShutdown();
    }

    private LastPriceService provideLastPriceService(ScheduledExecutorService executorService) {
        return new LastPriceServiceImpl(
            new PriceRecordContainer(),
            new InMemoryBatchRunRepository(
                SystemEpochClock.INSTANCE,
                new LongSupplier() {

                    private AtomicLong sequence = new AtomicLong();

                    @Override
                    public long getAsLong() {
                        return sequence.getAndIncrement();
                    }
            }),
            Arrays.asList(SUPPORTED_INSTRUMENTS.split(",")),
            MAX_ACTIVE_BATCH_RUNS_TARGET,
            BATCH_RUNS_CLEAN_UP_INTERVAL_MS,
            ABANDONED_BATCH_RUN_TIMEOUT_MS,
            executorService
        );
    }

    private void setUpShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    LastPriceServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() throws InterruptedException {
        if (executorService != null) {
            executorService.shutdown();
            executorService.awaitTermination(30, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

}
