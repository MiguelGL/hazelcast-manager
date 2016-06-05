package com.mgl.hazelcast.manager;

import io.airlift.airline.Option;
import io.airlift.airline.OptionType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public abstract class BaseCommand implements Runnable {

    @Option(name = {"-name", "-n"},
            description = "Instance name",
            required = false,
            type = OptionType.COMMAND)
    @Getter @Setter private String instanceName = "our-hazelcast-instance";

    @Option(name = {"-mgmnt-topic", "-t"},
            description = "Management topic name",
            required = true,
            type = OptionType.COMMAND)
    private String managementTopicName = "hz-management-topic";

    private final Object terminationLock = new Object();

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
