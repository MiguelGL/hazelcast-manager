package com.mgl.hazelcast.manager.operation;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Data @EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
public abstract class ManagementResponse extends ManagementMessage {

    private static final long serialVersionUID = 1L;

    @Getter(AccessLevel.PACKAGE) private final String requestUuid;

    protected ManagementResponse(Instant ts, String requestUuid) {
        super(ts);
        this.requestUuid = requestUuid;
    }

}
