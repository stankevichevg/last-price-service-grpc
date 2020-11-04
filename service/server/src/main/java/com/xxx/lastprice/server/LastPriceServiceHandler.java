package com.xxx.lastprice.server;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.xxx.lastprice.domain.LastPriceService;
import com.xxx.lastprice.domain.LastPriceServiceException.BatchNotFoundException;
import com.xxx.lastprice.domain.LastPriceServiceException.TooManyActiveBatchRunsException;
import com.xxx.lastprice.domain.LastPriceServiceException.WrongInstrumentException;
import com.xxx.lastprice.domain.PriceRecord;
import com.xxx.lastprice.transport.CancelBatchRunRequest;
import com.xxx.lastprice.transport.CancelBatchRunResponse;
import com.xxx.lastprice.transport.CompleteBatchRunRequest;
import com.xxx.lastprice.transport.CompleteBatchRunResponse;
import com.xxx.lastprice.transport.InstrumentPriceRecord;
import com.xxx.lastprice.transport.LastPriceGrpc;
import com.xxx.lastprice.transport.LastPriceRequest;
import com.xxx.lastprice.transport.LastPriceResponse;
import com.xxx.lastprice.transport.StartBatchRunResponse;
import com.xxx.lastprice.transport.UploadChunkRequest;
import com.xxx.lastprice.transport.UploadChunkResponse;
import io.grpc.stub.StreamObserver;

import static com.xxx.lastprice.server.StaticResponses.BATCH_NOT_FOUND_UPLOAD_CHUNK;
import static com.xxx.lastprice.server.StaticResponses.BATCH_RUN_CANCELED;
import static com.xxx.lastprice.server.StaticResponses.BATCH_RUN_COMPLETED;
import static com.xxx.lastprice.server.StaticResponses.BATCH_RUN_NOT_FOUND_CANCEL_BATCH;
import static com.xxx.lastprice.server.StaticResponses.BATCH_RUN_NOT_FOUND_COMPLETE_BATCH;
import static com.xxx.lastprice.server.StaticResponses.CHUNK_UPLOADED;
import static com.xxx.lastprice.server.StaticResponses.PRICE_IS_NOT_AVAILABLE;
import static com.xxx.lastprice.server.StaticResponses.TOO_MANY_ACTIVE_BATCH_RUNS;
import static com.xxx.lastprice.server.StaticResponses.WRONG_INSTRUMENT_LAST_PRICE_RESPONSE;
import static com.xxx.lastprice.server.StaticResponses.WRONG_INSTRUMENT_UPLOAD_CHUNK;
import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public class LastPriceServiceHandler extends LastPriceGrpc.LastPriceImplBase {

    private final LastPriceService lastPriceService;

    public LastPriceServiceHandler(LastPriceService lastPriceService) {
        this.lastPriceService = lastPriceService;
    }

    @Override
    public void requestLastPrice(LastPriceRequest request, StreamObserver<LastPriceResponse> responseObserver) {
        try {
            responseObserver.onNext(
                lastPriceService
                    .findLastPrice(request.getInstrument())
                    .map(this::buildLastPriceResponse)
                    .orElse(PRICE_IS_NOT_AVAILABLE)
            );
        } catch (WrongInstrumentException e) {
            responseObserver.onNext(WRONG_INSTRUMENT_LAST_PRICE_RESPONSE);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void startBatchRun(Empty request, StreamObserver<StartBatchRunResponse> responseObserver) {
        try {
            final long batchId = lastPriceService.startBatchRun();
            responseObserver.onNext(
                StartBatchRunResponse.newBuilder()
                    .setStatus(StartBatchRunResponse.Status.SUCCESS)
                    .setBatchRunId(batchId)
                    .build()
            );
        } catch (TooManyActiveBatchRunsException e) {
            responseObserver.onNext(TOO_MANY_ACTIVE_BATCH_RUNS);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void uploadChunk(UploadChunkRequest request, StreamObserver<UploadChunkResponse> responseObserver) {
        try {
            lastPriceService.uploadPriceRecordsChunk(
                request.getBatchRunId(),
                request.getPriceRecordsList().stream()
                    .map(record -> new PriceRecord(record.getInstrument(), record.getAsOf(), record.toByteArray()))
                    .collect(toUnmodifiableList())
            );
            responseObserver.onNext(CHUNK_UPLOADED);
        } catch (BatchNotFoundException e) {
            responseObserver.onNext(BATCH_NOT_FOUND_UPLOAD_CHUNK);
        } catch (WrongInstrumentException e) {
            responseObserver.onNext(WRONG_INSTRUMENT_UPLOAD_CHUNK);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void cancelBatchRun(CancelBatchRunRequest request, StreamObserver<CancelBatchRunResponse> responseObserver) {
        try {
            lastPriceService.cancelBatchRun(request.getBatchRunId());
            responseObserver.onNext(BATCH_RUN_CANCELED);
        } catch (BatchNotFoundException e) {
            responseObserver.onNext(BATCH_RUN_NOT_FOUND_CANCEL_BATCH);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void completeBatchRun(CompleteBatchRunRequest request, StreamObserver<CompleteBatchRunResponse> responseObserver) {
        try {
            lastPriceService.completeBatchRun(request.getBatchRunId());
            responseObserver.onNext(BATCH_RUN_COMPLETED);
        } catch (BatchNotFoundException e) {
            responseObserver.onNext(BATCH_RUN_NOT_FOUND_COMPLETE_BATCH);
        }
        responseObserver.onCompleted();
    }

    private LastPriceResponse buildLastPriceResponse(PriceRecord priceRecord) {
        return LastPriceResponse.newBuilder()
            .setStatus(LastPriceResponse.Status.SUCCESS)
            .setPriceRecord(
                InstrumentPriceRecord.newBuilder()
                    .setInstrument(priceRecord.getInstrument())
                    .setAsOf(priceRecord.getAsOf())
                    .setPayload(ByteString.copyFrom(priceRecord.getPayload()))
            ).build();
    }
}
