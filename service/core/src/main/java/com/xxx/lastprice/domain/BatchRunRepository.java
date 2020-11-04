package com.xxx.lastprice.domain;

import java.util.function.Consumer;

/**
 * Repository to manage lifecycle of {@link BatchRun} objects.
 *
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public interface BatchRunRepository {

    /**
     * Creates batch run.
     *
     * @return created batch run
     */
    BatchRun create();

    /**
     * Retrieves existing batch run.
     *
     * @param id identifier of the requested batch run
     * @return batch run with the specified id or {@code null} if there was no such batch found
     */
    BatchRun get(long id);

    /**
     * Removes batch run with the given id.
     *
     * @param id of the batch run to delete.
     * @return removed batch run or {@code null} if there was not a batch run found
     */
    BatchRun remove(long id);

    /**
     * Updates batch run with the given id.
     *
     * @param batchRunUpdater batch run to save
     * @return new state of the batch run or {@code null} if there was not a batch run found
     */
    BatchRun update(long id, Consumer<BatchRun> batchRunUpdater);

    /**
     * Returns current number of instances.
     *
     * @return number of active batch runs
     */
    int size();

    /**
     * Removes batch runs which were updated more than specified time ago.
     *
     * @param evictionTime records eviction time
     * @param limit maximum number of records to delete
     * @return number of removed records
     */
    int removeOutdated(long evictionTime, int limit);

    /**
     * Removes batch runs which were updated more than specified time ago.
     *
     * @param evictionTime records eviction time
     * @return number of removed records
     */
    default int removeOutdated(long evictionTime) {
        return removeOutdated(evictionTime, Integer.MAX_VALUE);
    }

    /**
     * Removes all repository records.
     */
    void removeAll();

}
