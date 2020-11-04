package com.xxx.lastprice.domain;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static java.util.Objects.isNull;

/**
 * Thread safe price records container. Wraps {@link Map} to provide atomic semantic
 * for for multiple update methods.
 * This structure uses single {@link ReadWriteLock} to synchronise access to the internal map.
 *
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public class PriceRecordContainer {

    private final ReadWriteLock lock;
    private final Map<String, PriceRecord> records;

    public PriceRecordContainer() {
        this(new ReentrantReadWriteLock(), new HashMap<>());
    }

    protected PriceRecordContainer(ReadWriteLock lock, Map<String, PriceRecord> records) {
        this.lock = lock;
        this.records = records;
    }

    /**
     * Retrieves price record for the given instrument.
     *
     * @param instrument to retrieve price record for
     * @return price record
     */
    public PriceRecord getPriceRecord(String instrument) {
        lock.readLock().lock();
        try {
            return records.get(instrument);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Update price records. This operation is atomic, acquiring write lock it
     * ensures nobody can read intermediate state.
     *
     * @param priceRecords actual prices records info to update internal state
     */
    public void updatePriceRecords(Collection<PriceRecord> priceRecords) {
        lock.writeLock().lock();
        try {
            priceRecords.forEach(this::unsafeUpdatePriceRecord);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Feeds all registered records to the given consumer.
     *
     * @param reader to consume records
     */
    public void readAll(Consumer<PriceRecord> reader) {
        lock.readLock().lock();
        try {
            records.values().forEach(reader);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Updates records of the given price record container feeding all records of the current container.
     * This operation require holding of two locks: write lock for the container we want to write to and
     * read lock of this container.
     *
     * @param other price record container to write records to
     */
    public void mergeTo(PriceRecordContainer other) {
        other.lock.writeLock().lock();
        try {
            readAll(other::unsafeUpdatePriceRecord);
        } finally {
            other.lock.writeLock().unlock();
        }
    }

    private void unsafeUpdatePriceRecord(PriceRecord priceRecord) {
        final PriceRecord existingRecord = records.get(priceRecord.getInstrument());
        if (isNull(existingRecord) || priceRecord.getAsOf() >= existingRecord.getAsOf()) {
            records.put(priceRecord.getInstrument(), priceRecord);
        }
    }

}
