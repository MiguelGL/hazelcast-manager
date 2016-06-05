package com.mgl.hazelcast.manager.operation;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
public abstract class ManagementRequest extends ManagementMessage {

    private static final long serialVersionUID = 1L;

    @Getter(AccessLevel.PACKAGE) private final String uuid;

    protected ManagementRequest(Instant ts) {
        super(ts);
        this.uuid = UUID.randomUUID().toString();
    }

    public <R extends ManagementResponse> boolean isResponse(R response) {
        return getUuid().equals(response.getRequestUuid());
    }

}
