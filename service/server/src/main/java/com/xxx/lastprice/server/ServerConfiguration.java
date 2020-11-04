package com.xxx.lastprice.server;

import static java.lang.Integer.getInteger;
import static java.lang.Long.getLong;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public final class ServerConfiguration {

    private ServerConfiguration() {
    }

    public static final String SERVER_PORT_PROP = "service.port";
    public static final int SERVER_PORT = getInteger(SERVER_PORT_PROP, 50051);

    public static final String MAX_ACTIVE_BATCH_RUNS_TARGET_PROP = "service.max_active_batch_runs_target";
    public static final int MAX_ACTIVE_BATCH_RUNS_TARGET =
        getInteger(MAX_ACTIVE_BATCH_RUNS_TARGET_PROP, 500);

    public static final String SUPPORTED_INSTRUMENTS_PROP = "service.supported_instruments";
    public static final String SUPPORTED_INSTRUMENTS = System.getProperty(
        SUPPORTED_INSTRUMENTS_PROP,
        "AIR,TEAM,NEE,SAF,TKWY,VOW,RDSA"
    );

    public static final String BATCH_RUNS_CLEAN_UP_INTERVAL_MS_PROP = "service.batch_run_clean_up_interval_ms";
    public static final long BATCH_RUNS_CLEAN_UP_INTERVAL_MS =
        getLong(BATCH_RUNS_CLEAN_UP_INTERVAL_MS_PROP, SECONDS.toMillis(60));

    public static final String ABANDONED_BATCH_RUN_TIMEOUT_MS_PROP = "service.abandoned_batch_run_timeout_ms";
    public static final long ABANDONED_BATCH_RUN_TIMEOUT_MS =
        getLong(ABANDONED_BATCH_RUN_TIMEOUT_MS_PROP, SECONDS.toMillis(5));

}
