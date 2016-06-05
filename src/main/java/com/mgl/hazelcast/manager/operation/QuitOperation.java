package com.mgl.hazelcast.manager.operation;

import com.google.common.base.Preconditions;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
public class QuitOperation extends ManagementRequest {

    private static final long serialVersionUID = 1L;

    @Getter(AccessLevel.PRIVATE) private final String instanceName;

    public QuitOperation(Instant ts, String instanceName) {
        super(ts);
        this.instanceName = Preconditions.checkNotNull(instanceName, "instanceName");
    }

    public boolean isForInstance(String instanceName) {
        return getInstanceName().equals(instanceName);
    }

}
