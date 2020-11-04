package com.xxx.lastprice.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public class PriceRecordContainerTest {

    private static final String AAPL = "AAPL";
    private static final String AMZN = "AMZN";

    private final Lock readLock = Mockito.mock(Lock.class);
    private final Lock writeLock = Mockito.mock(Lock.class);
    private final ReadWriteLock readWriteLock = Mockito.mock(ReadWriteLock.class);

    private final Map<String, PriceRecord> records = Mockito.mock(Map.class);

    private final PriceRecordContainer priceRecordContainer = new PriceRecordContainer(readWriteLock, records);

    @BeforeEach
    public void init() {
        Mockito.reset(readLock, writeLock, readWriteLock);
        when(readWriteLock.readLock()).thenReturn(readLock);
        when(readWriteLock.writeLock()).thenReturn(writeLock);
    }

    @Test
    public void shouldReadRecordsCorrectly() {
        final PriceRecord priceRecord = new PriceRecord(AAPL, 100500L, new byte[10]);
        when(records.get(AAPL)).thenReturn(priceRecord);
        final PriceRecord foundPriceRecord = priceRecordContainer.getPriceRecord(AAPL);
        assertThat(foundPriceRecord.getInstrument(), is(priceRecord.getInstrument()));
        assertThat(foundPriceRecord.getAsOf(), is(priceRecord.getAsOf()));
        assertThat(foundPriceRecord.getPayload(), equalTo(priceRecord.getPayload()));
        InOrder inOrder = inOrder(records, readLock);
        inOrder.verify(readLock).lock();
        inOrder.verify(records).get(AAPL);
        inOrder.verify(readLock).unlock();
    }

    @Test
    public void shouldUpdateRecordsCorrectly() {
        final PriceRecord firstPriceRecord = new PriceRecord(AAPL, 100500L, new byte[10]);
        final PriceRecord secondPriceRecord = new PriceRecord(AMZN, 100501L, new byte[10]);
        priceRecordContainer.updatePriceRecords(List.of(firstPriceRecord, secondPriceRecord));
        InOrder inOrder = inOrder(records, writeLock);
        inOrder.verify(writeLock).lock();
        inOrder.verify(records).put(AAPL, firstPriceRecord);
        inOrder.verify(records).put(AMZN, secondPriceRecord);
        inOrder.verify(writeLock).unlock();
    }

    @Test
    public void shouldReadAllCorrectly() {
        final PriceRecord firstPriceRecord = new PriceRecord(AAPL, 100500L, new byte[10]);
        final PriceRecord secondPriceRecord = new PriceRecord(AMZN, 100501L, new byte[10]);
        final List<PriceRecord> values = List.of(firstPriceRecord, secondPriceRecord);
        when(records.values()).thenReturn(values);
        final Consumer<PriceRecord> priceRecordConsumer = Mockito.mock(Consumer.class);
        priceRecordContainer.readAll(priceRecordConsumer);
        InOrder inOrder = inOrder(readLock, records, priceRecordConsumer);
        inOrder.verify(readLock).lock();
        inOrder.verify(priceRecordConsumer).accept(firstPriceRecord);
        inOrder.verify(priceRecordConsumer).accept(secondPriceRecord);
        inOrder.verify(readLock).unlock();
    }

    @Test
    public void shouldMergeAllRecordsCorrectly() {
        final Lock otherWriteLock = Mockito.mock(Lock.class);
        final ReadWriteLock otherReadWriteLock = Mockito.mock(ReadWriteLock.class);
        final Map<String, PriceRecord> otherRecords = Mockito.mock(Map.class);
        final PriceRecordContainer otherPriceRecordContainer = new PriceRecordContainer(otherReadWriteLock, otherRecords);
        when(otherReadWriteLock.writeLock()).thenReturn(otherWriteLock);
        final PriceRecord firstPriceRecord = new PriceRecord(AAPL, 100500L, new byte[10]);
        final PriceRecord secondPriceRecord = new PriceRecord(AMZN, 100501L, new byte[10]);
        when(records.values()).thenReturn(List.of(firstPriceRecord, secondPriceRecord));
        priceRecordContainer.mergeTo(otherPriceRecordContainer);
        InOrder inOrder = inOrder(readLock, records, otherWriteLock, otherRecords);
        inOrder.verify(otherWriteLock).lock();
        inOrder.verify(readLock).lock();
        inOrder.verify(otherRecords).put(AAPL, firstPriceRecord);
        inOrder.verify(otherRecords).put(AMZN, secondPriceRecord);
        inOrder.verify(readLock).unlock();
        inOrder.verify(otherWriteLock).unlock();
    }

}
