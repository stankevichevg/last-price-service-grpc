package com.xxx.lastprice.domain;

import com.xxx.lastprice.domain.InMemoryBatchRunRepository.CleanUpEntity;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.LongSupplier;

import static java.util.Comparator.comparingLong;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public class InMemoryBatchRunRepositoryTest {

    private final EpochClock epochClock = Mockito.mock(EpochClock.class);
    private final Lock readLock = Mockito.mock(Lock.class);
    private final Lock writeLock = Mockito.mock(Lock.class);
    private final ReadWriteLock readWriteLock = Mockito.mock(ReadWriteLock.class);
    private final LongSupplier batchIdSequence = Mockito.mock(LongSupplier.class);

    private final Map<Long, BatchRun> batchRuns = Mockito.spy(new HashMap<>());
    private final Map<Long, CleanUpEntity> cleanUpEntities = new HashMap<>();
    private final PriorityQueue<CleanUpEntity> cleanUpQueue =
        new PriorityQueue<>(comparingLong(CleanUpEntity::getLastUpdateTimestamp));

    private final InMemoryBatchRunRepository repository = new InMemoryBatchRunRepository(
        epochClock, readWriteLock, batchRuns, cleanUpEntities, cleanUpQueue, batchIdSequence
    );

    @BeforeEach
    public void init() {
        batchRuns.clear();
        cleanUpEntities.clear();
        cleanUpQueue.clear();
        reset(epochClock, readWriteLock, readLock, writeLock, batchRuns, batchIdSequence);
        when(readWriteLock.readLock()).thenReturn(readLock);
        when(readWriteLock.writeLock()).thenReturn(writeLock);
    }

    @Test
    public void shouldCreateBatchRunCorrectly() {
        when(batchIdSequence.getAsLong()).thenReturn(123L);
        when(epochClock.time()).thenReturn(321L);
        final BatchRun batchRun = repository.create();
        assertThat(batchRun.getId(), Matchers.is(123L));
        assertThat(batchRuns.values(), contains(batchRun));
        final CleanUpEntity cleanUpEntity = new CleanUpEntity(batchRun, 321L);
        assertThat(cleanUpEntities.values(), contains(cleanUpEntity));
        assertThat(cleanUpQueue, contains(cleanUpEntity));
        final InOrder inOrder = inOrder(batchRuns, writeLock);
        inOrder.verify(writeLock).lock();
        inOrder.verify(batchRuns).put(123L, batchRun);
        inOrder.verify(writeLock).unlock();
    }

    @Test
    public void shouldUseReadLockForGet() {
        repository.get(123L);
        final InOrder inOrder = inOrder(batchRuns, readLock);
        inOrder.verify(readLock).lock();
        inOrder.verify(batchRuns).get(123L);
        inOrder.verify(readLock).unlock();
    }

}
