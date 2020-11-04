package com.xxx.lastprice.client;

import com.google.protobuf.Empty;
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
import io.grpc.Channel;

import java.util.Collection;


/**
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public class LastPriceClient {

    private final LastPriceGrpc.LastPriceBlockingStub blockingStub;

    public LastPriceClient(Channel channel) {
        this.blockingStub = LastPriceGrpc.newBlockingStub(channel);
    }

    public LastPriceResponse requestLastPrice(String instrument) {
        return blockingStub.requestLastPrice(LastPriceRequest.newBuilder().setInstrument(instrument).build());
    }

    public StartBatchRunResponse startBatchRun() {
        return blockingStub.startBatchRun(Empty.newBuilder().build());
    }

    public UploadChunkResponse uploadPriceRecordsChunk(long batchRunId, Collection<InstrumentPriceRecord> priceRecords) {
        final UploadChunkRequest.Builder builder = UploadChunkRequest.newBuilder().setBatchRunId(batchRunId);
        priceRecords.forEach(builder::addPriceRecords);
        return blockingStub.uploadChunk(builder.build());
    }

    public CancelBatchRunResponse cancelBatchRun(long batchRunId) {
        return blockingStub.cancelBatchRun(CancelBatchRunRequest.newBuilder().setBatchRunId(batchRunId).build());
    }

    public CompleteBatchRunResponse completeBatchRun(long batchRunId) {
        return blockingStub.completeBatchRun(CompleteBatchRunRequest.newBuilder().setBatchRunId(batchRunId).build());
    }

}
