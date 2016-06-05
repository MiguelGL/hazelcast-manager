package com.mgl.hazelcast.manager.operation;

import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data @EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
public class QuitResult extends ManagementResponse {

    private static final long serialVersionUID = 1L;

    public QuitResult(Instant ts, QuitOperation quitOperation) {
        super(ts, quitOperation.getUuid());
    }

}
