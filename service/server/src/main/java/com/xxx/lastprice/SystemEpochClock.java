package com.xxx.lastprice;

import com.xxx.lastprice.domain.EpochClock;

/**
 * Implementation that calls {@link System#currentTimeMillis()}.
 */
public class SystemEpochClock implements EpochClock
{
    /**
     * As there is no instance state then this object can be used to save on allocation.
     */
    public static final SystemEpochClock INSTANCE = new SystemEpochClock();

    public long time()
    {
        return System.currentTimeMillis();
    }
}
