package com.mgl.hazelcast.manager;

import com.mgl.hazelcast.manager.operation.ManagementMessage;
import com.mgl.hazelcast.manager.operation.QuitOperation;
import com.mgl.hazelcast.manager.operation.QuitResult;
import com.google.common.base.Preconditions;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleListener;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import io.airlift.airline.OptionType;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Command(name = "stop", description = "Stop a Hazelcast instance")
public class Quitter
extends BaseCommand
implements MessageListener<ManagementMessage>, LifecycleListener {

    @Option(name = {"-conf-file", "-c"},
            description = "Path to configuration file",
            required = false,
            type = OptionType.COMMAND)
    @Getter @Setter private String configFile = "etc/hazelcast-client-config.xml";

    private HazelcastInstance hazelcastClient;
    private String lifecycleRegistrationId;

    private QuitOperation quitOperation;

    @Override
    @SneakyThrows
    public void run() {
        log.info("Stoping a Hazelcast instance as per '{}'", getConfigFile());
        ClientConfig clientConfig = new XmlClientConfigBuilder(getConfigFile()).build();
        hazelcastClient = HazelcastClient.newHazelcastClient(clientConfig);
        try {
            lifecycleRegistrationId = hazelcastClient.getLifecycleService().addLifecycleListener(this);
            ITopic<ManagementMessage> managementTopic = hazelcastClient.getTopic(getManagementTopicName());
            quitOperation = new QuitOperation(Instant.now(), getInstanceName());
            managementTopic.publish(quitOperation);
            awaitTermination();
            log.info("Stopped a Hazelcast instance");
        } finally {
            hazelcastClient.shutdown();
        }
    }

    @Override
    public void onMessage(Message<ManagementMessage> message) {
        ManagementMessage mmessage = message.getMessageObject();
        if (mmessage instanceof QuitResult) {
            QuitResult quitResponse = (QuitResult) mmessage;
            if (quitOperation.isResponse(quitResponse)) {
                signalTermination();
            } else {
                log.info("Listened irrelevant to me quit response (may be ok): {}", quitResponse);
            }
        } else {
            log.info("Listened an unexpected message (may be ok): {}", mmessage);
        }
    }

    @Override
    public void stateChanged(LifecycleEvent event) {
        switch (event.getState()) {
            case SHUTTING_DOWN:
            {
                if (lifecycleRegistrationId == null) {
                    log.warn("Hazelcast client shut down, but not registered to lifecycle topic '{}'",
                            getManagementTopicName());
                } else {
                    log.info("Hazelcast client shut down, unregistering from lifecycle topic '{}'",
                            getManagementTopicName());
                    boolean unregistered = hazelcastClient.getLifecycleService()
                            .removeLifecycleListener(lifecycleRegistrationId);
                    Preconditions.checkState(unregistered, "Not unregistered from lifecycle service");
                }
                signalTermination();
                break;
            }
            default:
                log.info("Hazelcast client lifecycle state change: {}", event.getState());
        }
    }

}
