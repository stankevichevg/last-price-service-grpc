package com.xxx.lastprice.domain;

/**
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public class BatchRun extends PriceRecordContainer {

    private final long id;

    public BatchRun(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

}
