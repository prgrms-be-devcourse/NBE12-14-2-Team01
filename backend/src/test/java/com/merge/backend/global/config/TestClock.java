package com.merge.backend.global.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class TestClock extends Clock {

    private Instant instant;
    private final ZoneId zone;

    public TestClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new TestClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }
}
