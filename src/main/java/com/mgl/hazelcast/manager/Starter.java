package com.mgl.hazelcast.manager;

import com.mgl.hazelcast.manager.operation.ManagementMessage;
import com.mgl.hazelcast.manager.operation.QuitOperation;
import com.mgl.hazelcast.manager.operation.QuitResult;
import com.google.common.base.Preconditions;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
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
@Command(name = "start", description = "Start a Hazelcast instance")
public class Starter
extends BaseCommand
implements MessageListener<ManagementMessage>, LifecycleListener {

    private HazelcastInstance hazelcastInstance;
    private String lifecycleRegistrationId;
    private String managementRegistrationId;

    @Option(name = {"-conf-file", "-c"},
            description = "Path to configuration file",
            required = false,
            type = OptionType.COMMAND)
    @Getter @Setter private String configFile = "etc/hazelcast-config.xml";

    @Override
    @SneakyThrows
    public void run() {
        log.info("Starting Hazelcast instance '{}' as per '{}'", getInstanceName(), getConfigFile());
        XmlConfigBuilder configBuilder = new XmlConfigBuilder(getConfigFile());
        Config config = configBuilder.build();
        config.setInstanceName(getInstanceName());
        hazelcastInstance = Hazelcast.getOrCreateHazelcastInstance(config);
        try {
            lifecycleRegistrationId = hazelcastInstance.getLifecycleService().addLifecycleListener(this);
            ITopic<ManagementMessage> managementTopic = hazelcastInstance.getTopic(getManagementTopicName());
            managementRegistrationId = managementTopic.addMessageListener(this);
            log.info("Started Hazelcast instance '{}', now awaiting termination", hazelcastInstance.getName());
            awaitTermination();
        } finally {
            hazelcastInstance.shutdown();
        }
        clearFields();
        log.info("Hazelcast instance '{}' terminated", getInstanceName());
    }

    @Override
    public void onMessage(Message<ManagementMessage> message) {
        if (message.getPublishingMember().localMember()) {
            log.debug("Ignoring self-emmited message {}", message.getMessageObject());
        }
        ManagementMessage operation = message.getMessageObject();
        log.debug("Got operation {}", operation);
        if (operation instanceof QuitOperation) {
            QuitOperation quitOperation = (QuitOperation) operation;
            if (quitOperation.isForInstance(getInstanceName())) {
                quit(quitOperation);
            } else {
                log.info("Ignoring quit operation message not for this instance: {}", operation);
            }
        } else {
            log.info("Ignoring unsupported management message: {}", operation);
        }
    }

    private void quit(QuitOperation quitOperation) {
        log.info("Quitting Hazelcast instance '{}' [order as per {}]",
                hazelcastInstance.getName(), quitOperation.getTs());
        ITopic<ManagementMessage> managementTopic =
                hazelcastInstance.getTopic(getManagementTopicName());
        managementTopic.publish(new QuitResult(Instant.now(), quitOperation));
        signalTermination();
    }

    private void clearFields() {
        managementRegistrationId = null;
        lifecycleRegistrationId = null;
        hazelcastInstance = null;
    }

    @Override
    public void stateChanged(LifecycleEvent event) {
        switch (event.getState()) {
            case SHUTTING_DOWN:
            {
                if (managementRegistrationId == null) {
                    log.warn("Hazelcast instance '{}' shutting down, but not registered to management topic '{}'",
                            getInstanceName(), getManagementTopicName());
                } else {
                    log.info("Hazelcast instance '{}' shutting down, unregistering from management topic '{}'",
                            getInstanceName(), getManagementTopicName());
                    ITopic<ManagementMessage> managementTopic =
                            hazelcastInstance.getTopic(getManagementTopicName());
                    boolean unregistered = managementTopic
                            .removeMessageListener(managementRegistrationId);
                    Preconditions.checkState(unregistered, "Not unregistered from management topic");
                }
                if (lifecycleRegistrationId == null) {
                    log.warn("Hazelcast instance '{}' shut down, but not registered to lifecycle topic '{}'",
                            getInstanceName(), getManagementTopicName());
                } else {
                    log.info("Hazelcast instance '{}' shut down, unregistering from lifecycle topic '{}'",
                            getInstanceName(), getManagementTopicName());
                    boolean unregistered = hazelcastInstance.getLifecycleService()
                            .removeLifecycleListener(lifecycleRegistrationId);
                    Preconditions.checkState(unregistered, "Not unregistered from lifecycle service");
                }
                signalTermination();
                break;
            }
            default:
                log.info("Hazelcast instance '{}' lifecycle state change: {}",
                        getInstanceName(), event.getState());
        }
    }

}
