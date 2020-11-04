package com.xxx.lastprice.domain;

/**
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public abstract class LastPriceServiceException extends Exception {

    protected LastPriceServiceException(String message) {
        super(message);
    }

    public static final class WrongInstrumentException extends LastPriceServiceException {

        private final String instrument;

        protected WrongInstrumentException(String instrument) {
            super("Instrument is not supported by the service: " + instrument);
            this.instrument = instrument;
        }

        public String getInstrument() {
            return instrument;
        }
    }

    public static final class TooManyActiveBatchRunsException extends LastPriceServiceException {

        private final int maxBatchRunsNumber;

        protected TooManyActiveBatchRunsException(int maxBatchRunsNumber) {
            super("Max number of active batches achieved: " + maxBatchRunsNumber);
            this.maxBatchRunsNumber = maxBatchRunsNumber;
        }

        public int getMaxBatchRunsNumber() {
            return maxBatchRunsNumber;
        }
    }

    public static final class BatchNotFoundException extends LastPriceServiceException {

        private final long batchId;

        protected BatchNotFoundException(long batchId) {
            super("Batch was not found, id: " + batchId);
            this.batchId = batchId;
        }

        public long getBatchId() {
            return batchId;
        }
    }

}
