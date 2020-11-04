package com.xxx.lastprice.domain;

/**
 * Price record object holding some payload for specific instrument at specific {@link #asOf} moment of time.
 *
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public class PriceRecord {

    private final String instrument;
    private final long asOf;
    private final byte[] payload;

    public PriceRecord(String instrument, long asOf, byte[] payload) {
        this.instrument = instrument;
        this.asOf = asOf;
        this.payload = payload;
    }

    public String getInstrument() {
        return instrument;
    }

    public long getAsOf() {
        return asOf;
    }

    public byte[] getPayload() {
        return payload;
    }
}
