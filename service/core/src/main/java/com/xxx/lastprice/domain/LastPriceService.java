package com.xxx.lastprice.domain;

import com.xxx.lastprice.domain.LastPriceServiceException.BatchNotFoundException;
import com.xxx.lastprice.domain.LastPriceServiceException.TooManyActiveBatchRunsException;
import com.xxx.lastprice.domain.LastPriceServiceException.WrongInstrumentException;

import java.util.Collection;
import java.util.Optional;

/**
 * Domain service to execute operations of the core service.
 *
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public interface LastPriceService {

    /**
     * Retrieves last price record for the given instrument.
     *
     * @param instrument to retrieve record for
     * @return price instrument
     * @throws WrongInstrumentException if given instrument is not supported
     */
    Optional<PriceRecord> findLastPrice(String instrument) throws WrongInstrumentException;

    /**
     * Starts new batch run.
     *
     * @return id of the started batch run
     * @throws TooManyActiveBatchRunsException if there are too many active batch runs
     */
    long startBatchRun() throws TooManyActiveBatchRunsException;

    /**
     * Uploads given collection of price records to the batch with the given id
     *
     * @param batchRunId batch id to upload records to
     * @param priceRecords records to upload
     * @throws BatchNotFoundException if batch with the given id was not found
     * @throws WrongInstrumentException if given instrument is not supported
     */
    void uploadPriceRecordsChunk(long batchRunId, Collection<PriceRecord> priceRecords)
        throws BatchNotFoundException, WrongInstrumentException;

    /**
     * Cancels batch run with the given id.
     *
     * @param batchRunId id of the batch run to cancel
     * @throws BatchNotFoundException if batch with the given id was not found
     */
    void cancelBatchRun(long batchRunId) throws BatchNotFoundException;

    /**
     * Completes batch run with the given id.
     *
     * @param batchRunId id of the batch run to complete
     * @throws BatchNotFoundException if batch with the given id was not found
     */
    void completeBatchRun(long batchRunId) throws BatchNotFoundException;

}
