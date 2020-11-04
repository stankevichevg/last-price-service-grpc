package com.xxx.lastprice.domain;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import static java.util.Comparator.comparingLong;
import static java.util.Objects.isNull;

/**
 * Thread safe implementation of batch run repository.
 * To synchronise access simple read/write locking strategy is used.
 * Only one thread is allowed to write at a moment, multiple readers are allowed.
 *
 * For high contended environments the parallelism level can be increased for write operations
 * distributing batch runs to separate buckets and use read/write lock on a bucket level.
 * At the moment it was decided to keep it as simple as possible.
 *
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public class InMemoryBatchRunRepository implements BatchRunRepository {

    private final EpochClock epochClock;

    private final ReadWriteLock lock;
    private final Map<Long, BatchRun> batchRuns;
    private final Map<Long, CleanUpEntity> cleanUpEntities;
    private final PriorityQueue<CleanUpEntity> cleanUpQueue;

    private LongSupplier batchIdSequence;

    public InMemoryBatchRunRepository(EpochClock epochClock, LongSupplier batchIdSequence) {
        this(
            epochClock,
            new ReentrantReadWriteLock(),
            new HashMap<>(),
            new HashMap<>(),
            new PriorityQueue<>(comparingLong(CleanUpEntity::getLastUpdateTimestamp)),
            batchIdSequence
        );
    }

    // just for testing
    InMemoryBatchRunRepository(
        EpochClock epochClock,
        ReadWriteLock lock,
        Map<Long, BatchRun> batchRuns,
        Map<Long, CleanUpEntity> cleanUpEntities,
        PriorityQueue<CleanUpEntity> cleanUpQueue,
        LongSupplier batchIdSequence) {

        this.epochClock = epochClock;
        this.lock = lock;
        this.batchRuns = batchRuns;
        this.cleanUpEntities = cleanUpEntities;
        this.cleanUpQueue = cleanUpQueue;
        this.batchIdSequence = batchIdSequence;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BatchRun create() {
        lock.writeLock().lock();
        try {
            final long id = batchIdSequence.getAsLong();
            final BatchRun batchRun = new BatchRun(id);
            final CleanUpEntity cleanUpEntity = new CleanUpEntity(batchRun, epochClock.time());
            batchRuns.put(id, batchRun);
            cleanUpEntities.put(id, cleanUpEntity);
            cleanUpQueue.add(cleanUpEntity);
            return batchRun;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BatchRun get(long id) {
        lock.readLock().lock();
        try {
            return batchRuns.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BatchRun remove(long id) {
        lock.writeLock().lock();
        try {
            return unsafeRemove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BatchRun update(long id, Consumer<BatchRun> batchRunUpdater) {
        lock.writeLock().lock();
        try {
            final BatchRun batchRun = batchRuns.get(id);
            if (isNull(batchRun)) {
                return null;
            }
            batchRunUpdater.accept(batchRun);
            unsafeSave(batchRun);
            return batchRun;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return batchRuns.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeOutdated(long evictionTime, int limit) {
        lock.writeLock().lock();
        try {
            final long lastAllowedMoment = epochClock.time() - evictionTime;
            int removedCounter = 0;
            while (removedCounter < limit && !cleanUpQueue.isEmpty()) {
                final CleanUpEntity cleanUpEntity = cleanUpQueue.peek();
                if (lastAllowedMoment < cleanUpEntity.lastUpdateTimestamp) {
                    break;
                }
                unsafeRemove(cleanUpEntity.batchRun.getId());
                removedCounter++;
            }
            return removedCounter;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll() {
        lock.writeLock().lock();
        try {
            while (!cleanUpQueue.isEmpty()) {
                unsafeRemove(cleanUpQueue.poll().batchRun.getId());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void unsafeSave(BatchRun batchRun) {
        batchRuns.put(batchRun.getId(), batchRun);
        final CleanUpEntity oldCleanUpEntity = cleanUpEntities.remove(batchRun.getId());
        if (null != oldCleanUpEntity) {
            cleanUpQueue.remove(oldCleanUpEntity);
        }
        final CleanUpEntity cleanUpEntity = new CleanUpEntity(batchRun, epochClock.time());
        cleanUpEntities.put(batchRun.getId(), cleanUpEntity);
        cleanUpQueue.add(cleanUpEntity);
    }

    private BatchRun unsafeRemove(long id) {
        final BatchRun removedBatchRun = batchRuns.remove(id);
        if (null != removedBatchRun) {
            cleanUpQueue.remove(cleanUpEntities.remove(id));
        }
        return removedBatchRun;
    }

    /**
     * Entity to track batch runs last update times. Used to remove abandoned batch runs.
     */
    static final class CleanUpEntity {

        private final BatchRun batchRun;
        private final long lastUpdateTimestamp;

        public CleanUpEntity(BatchRun batchRun, long lastUpdateTimestamp) {
            this.batchRun = batchRun;
            this.lastUpdateTimestamp = lastUpdateTimestamp;
        }

        public BatchRun getBatchRun() {
            return batchRun;
        }

        public long getLastUpdateTimestamp() {
            return lastUpdateTimestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CleanUpEntity that = (CleanUpEntity) o;
            return Objects.equals(batchRun.getId(), that.batchRun.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(batchRun.getId());
        }
    }

}
