package com.mgl.hazelcast.manager;

import io.airlift.airline.Option;
import io.airlift.airline.OptionType;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@NoArgsConstructor
@ToString(exclude = {"terminationLock"})
@EqualsAndHashCode(exclude = {"terminationLock"})
public abstract class BaseCommand implements Runnable {

    @Option(name = {"-name", "-n"},
            description = "Instance name",
            required = false,
            type = OptionType.COMMAND)
    private String instanceName = "our-hazelcast-instance";

    @Option(name = {"-mgmnt-topic", "-t"},
            description = "Management topic name",
            required = true,
            type = OptionType.COMMAND)
    private String managementTopicName = "hz-management-topic";

    @Getter(AccessLevel.NONE) private final Object terminationLock = new Object();

    protected void awaitTermination() throws InterruptedException {
        synchronized (terminationLock) {
            terminationLock.wait();
        }
    }

    protected void signalTermination() {
        synchronized (terminationLock) {
            terminationLock.notifyAll();
        }
    }

}
