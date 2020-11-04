package com.xxx.lastprice.domain;

/**
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public interface EpochClock {

    /**
     * Time in milliseconds since 1 Jan 1970 UTC.
     *
     * @return the number of milliseconds since 1 Jan 1970 UTC.
     */
    long time();
}