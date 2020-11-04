package com.xxx.lastprice.server;


import com.xxx.lastprice.transport.CancelBatchRunResponse;
import com.xxx.lastprice.transport.CompleteBatchRunResponse;
import com.xxx.lastprice.transport.LastPriceResponse;
import com.xxx.lastprice.transport.StartBatchRunResponse;
import com.xxx.lastprice.transport.UploadChunkResponse;

/**
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public final class StaticResponses {

    private StaticResponses() {
    }

    static final LastPriceResponse PRICE_IS_NOT_AVAILABLE =
        LastPriceResponse.newBuilder().setStatus(LastPriceResponse.Status.PRICE_NOT_AVAILABLE).build();

    static final LastPriceResponse WRONG_INSTRUMENT_LAST_PRICE_RESPONSE =
        LastPriceResponse.newBuilder().setStatus(LastPriceResponse.Status.WRONG_INSTRUMENT).build();

    static final StartBatchRunResponse TOO_MANY_ACTIVE_BATCH_RUNS =
        StartBatchRunResponse.newBuilder().setStatus(StartBatchRunResponse.Status.TOO_MANY_ACTIVE_BATCH_RUNS).build();

    static final UploadChunkResponse CHUNK_UPLOADED =
        UploadChunkResponse.newBuilder().setStatus(UploadChunkResponse.Status.SUCCESS).build();

    static final UploadChunkResponse WRONG_INSTRUMENT_UPLOAD_CHUNK =
        UploadChunkResponse.newBuilder().setStatus(UploadChunkResponse.Status.WRONG_INSTRUMENT).build();

    static final UploadChunkResponse BATCH_NOT_FOUND_UPLOAD_CHUNK =
        UploadChunkResponse.newBuilder().setStatus(UploadChunkResponse.Status.BATCH_RUN_NOT_FOUND).build();

    static final CancelBatchRunResponse BATCH_RUN_CANCELED =
        CancelBatchRunResponse.newBuilder().setStatus(CancelBatchRunResponse.Status.SUCCESS).build();

    static final CancelBatchRunResponse BATCH_RUN_NOT_FOUND_CANCEL_BATCH =
        CancelBatchRunResponse.newBuilder().setStatus(CancelBatchRunResponse.Status.BATCH_RUN_NOT_FOUND).build();

    static final CompleteBatchRunResponse BATCH_RUN_COMPLETED =
        CompleteBatchRunResponse.newBuilder().setStatus(CompleteBatchRunResponse.Status.SUCCESS).build();

    static final CompleteBatchRunResponse BATCH_RUN_NOT_FOUND_COMPLETE_BATCH =
        CompleteBatchRunResponse.newBuilder().setStatus(CompleteBatchRunResponse.Status.BATCH_RUN_NOT_FOUND).build();

}
