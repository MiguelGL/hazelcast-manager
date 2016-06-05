package com.mgl.hazelcast.manager.operation;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode @ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class ManagementMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long ts;

    protected ManagementMessage(Instant ts) {
        this.ts = Preconditions.checkNotNull(ts, "ts").toEpochMilli();
    }

    public Instant getTs() {
        return Instant.ofEpochMilli(ts);
    }

}
