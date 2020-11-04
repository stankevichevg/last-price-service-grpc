package com.xxx.lastprice;

import com.google.protobuf.ByteString;
import com.xxx.lastprice.client.LastPriceClient;
import com.xxx.lastprice.transport.InstrumentPriceRecord;
import com.xxx.lastprice.transport.StartBatchRunResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.HdrHistogram.Histogram;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.xxx.lastprice.server.ServerConfiguration.SUPPORTED_INSTRUMENTS;

/**
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public class StartUploadCompletePerformanceTest {

    private static final Random RANDOM = new Random();
    private static final List<String> INSTRUMENTS = Arrays.asList(SUPPORTED_INSTRUMENTS.split(","));

    private static final Histogram START_BATCH_HISTOGRAM = new Histogram(TimeUnit.SECONDS.toNanos(10), 3);
    private static final Histogram UPLOAD_CHUNK_HISTOGRAM = new Histogram(TimeUnit.SECONDS.toNanos(10), 3);
    private static final Histogram COMPLETE_BATCH_HISTOGRAM = new Histogram(TimeUnit.SECONDS.toNanos(10), 3);

    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:50051")
            .usePlaintext()
            .build();
        try {
            LastPriceClient client = new LastPriceClient(channel);
            for (int i = 0; i < 70000; i++) {
                final StartBatchRunResponse response = client.startBatchRun();
                final List<InstrumentPriceRecord> records = createChunk();
                client.uploadPriceRecordsChunk(response.getBatchRunId(), records);
                client.completeBatchRun(response.getBatchRunId());
            }
            START_BATCH_HISTOGRAM.reset();
            UPLOAD_CHUNK_HISTOGRAM.reset();
            COMPLETE_BATCH_HISTOGRAM.reset();
            for (int i = 0; i < 30000; i++) {
                final long start = System.nanoTime();
                final StartBatchRunResponse response = client.startBatchRun();
                START_BATCH_HISTOGRAM.recordValue(System.nanoTime() - start);

                final List<InstrumentPriceRecord> records = createChunk();
                final long uploadStart = System.nanoTime();
                client.uploadPriceRecordsChunk(response.getBatchRunId(), records);
                UPLOAD_CHUNK_HISTOGRAM.recordValue(System.nanoTime() - uploadStart);

                final long completeStart = System.nanoTime();
                client.completeBatchRun(response.getBatchRunId());
                COMPLETE_BATCH_HISTOGRAM.recordValue(System.nanoTime() - completeStart);
            }
            START_BATCH_HISTOGRAM.outputPercentileDistribution(System.out, 1000.0);
            UPLOAD_CHUNK_HISTOGRAM.outputPercentileDistribution(System.out, 1000.0);
            COMPLETE_BATCH_HISTOGRAM.outputPercentileDistribution(System.out, 1000.0);
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static List<InstrumentPriceRecord> createChunk() {
        final ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        final List<InstrumentPriceRecord> records = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            buffer.putInt(0, i);
            records.add(InstrumentPriceRecord.newBuilder()
                .setAsOf(i)
                .setInstrument(INSTRUMENTS.get(RANDOM.nextInt(INSTRUMENTS.size())))
                .setPayload(ByteString.copyFrom(buffer))
                .build()
            );
        }
        return records;
    }

}
