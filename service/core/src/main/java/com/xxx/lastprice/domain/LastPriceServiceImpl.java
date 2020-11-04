package com.xxx.lastprice.domain;

import com.xxx.lastprice.domain.LastPriceServiceException.BatchNotFoundException;
import com.xxx.lastprice.domain.LastPriceServiceException.TooManyActiveBatchRunsException;
import com.xxx.lastprice.domain.LastPriceServiceException.WrongInstrumentException;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

/**
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public class LastPriceServiceImpl implements LastPriceService {

    private final PriceRecordContainer marketState;
    private final BatchRunRepository batchRunRepository;
    private final Set<String> supportedInstruments;
    private final int maxActiveBatchRunsTarget;

    public LastPriceServiceImpl(
        PriceRecordContainer marketState,
        BatchRunRepository batchRunRepository,
        Collection<String> supportedInstruments,
        int maxActiveBatchRunsTarget,
        long batchRunsCleanUpIntervalMs,
        long abandonedBatchRunTimeout,
        ScheduledExecutorService executorService) {

        this.marketState = marketState;
        this.batchRunRepository = batchRunRepository;
        this.supportedInstruments = Set.copyOf(supportedInstruments);
        this.maxActiveBatchRunsTarget = maxActiveBatchRunsTarget;
        startCleanUpJob(executorService, abandonedBatchRunTimeout, batchRunsCleanUpIntervalMs);
    }

    @Override
    public Optional<PriceRecord> findLastPrice(String instrument) throws WrongInstrumentException {
        checkInstrumentSupported(instrument);
        return ofNullable(marketState.getPriceRecord(instrument));
    }

    @Override
    public long startBatchRun() throws TooManyActiveBatchRunsException {
        if (batchRunRepository.size() >= maxActiveBatchRunsTarget) {
            throw new TooManyActiveBatchRunsException(maxActiveBatchRunsTarget);
        }
        // In high contended environment we can get here in several threads even if number
        // of batch runs is (maxActiveBatchRunsTarget - 1), in such case eventually we will have
        // size of the repository more then maxActiveBatchRunsTarget.
        // It is not critical for our service.
        return batchRunRepository.create().getId();
    }

    @Override
    public void uploadPriceRecordsChunk(long batchRunId, Collection<PriceRecord> priceRecords)
        throws BatchNotFoundException, WrongInstrumentException {
        final Optional<PriceRecord> priceRecord = priceRecords.stream()
            .filter(record -> !supportedInstruments.contains(record.getInstrument()))
            .findAny();
        if (priceRecord.isPresent()) {
            throw new WrongInstrumentException(priceRecord.get().getInstrument());
        }
        final BatchRun updatedBatchRun = batchRunRepository.update(batchRunId, batchRun -> {
            batchRun.updatePriceRecords(priceRecords);
        });
        checkBatchRunFound(batchRunId, updatedBatchRun);
    }

    @Override
    public void cancelBatchRun(long batchRunId) throws BatchNotFoundException {
        checkBatchRunFound(batchRunId, batchRunRepository.remove(batchRunId));
    }

    @Override
    public void completeBatchRun(long batchRunId) throws BatchNotFoundException {
        final BatchRun batchRun = batchRunRepository.remove(batchRunId);
        checkBatchRunFound(batchRunId, batchRun);
        batchRun.mergeTo(marketState);
    }

    private void checkInstrumentSupported(String instrument) throws WrongInstrumentException {
        if (!supportedInstruments.contains(instrument)) {
            throw new WrongInstrumentException(instrument);
        }
    }

    private void checkBatchRunFound(long batchRunId, BatchRun batchRun) throws BatchNotFoundException {
        if (isNull(batchRun)) {
            throw new BatchNotFoundException(batchRunId);
        }
    }

    private void startCleanUpJob(
        ScheduledExecutorService executorService,
        long abandonedBatchRunTimeout,
        long batchRunsCleanUpIntervalMs) {

        executorService.scheduleWithFixedDelay(
            new AbandonedBatchRunCleanUpJob(abandonedBatchRunTimeout),
            batchRunsCleanUpIntervalMs,
            batchRunsCleanUpIntervalMs,
            TimeUnit.MILLISECONDS
        );
    }

    private final class AbandonedBatchRunCleanUpJob implements Runnable {

        private final long abandonedBatchRunTimeout;

        private AbandonedBatchRunCleanUpJob(long abandonedBatchRunTimeout) {
            this.abandonedBatchRunTimeout = abandonedBatchRunTimeout;
        }

        @Override
        public void run() {
            LastPriceServiceImpl.this.batchRunRepository.removeOutdated(abandonedBatchRunTimeout);
        }
    }

}
