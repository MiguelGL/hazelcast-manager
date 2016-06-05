package com.mgl.hazelcast.manager;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.mgl.hazelcast.manager.operation.ManagementMessage;
import com.mgl.hazelcast.manager.operation.QuitOperation;
import com.mgl.hazelcast.manager.operation.QuitResult;
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
import java.util.Timer;
import java.util.TimerTask;

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

    @Option(name = {"-pre-quit-wait-secs", "-w"},
            description = "Time to wait (seconds) before terminating so that quitter process "
                          + "can terminate before and outputs no reconnection error trace",
            required = false,
            type = OptionType.COMMAND)
    @Getter @Setter private int preQuitWaitSecs = 5;

    @Override
    @SneakyThrows
    public void run() {
        log.info("Starting Hazelcast instance '{}' as per '{}'", getInstanceName(), getConfigFile());
        checkArgument(preQuitWaitSecs >= 0, "Illegal 'preQuitWaitSecs' value %s", preQuitWaitSecs);
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
            log.debug("Ignoring self-emmited message {}", message);
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

    @SneakyThrows
    private void quit(QuitOperation quitOperation) {
        log.info("Quitting Hazelcast instance '{}' [order as per {}] in {} seconds",
                hazelcastInstance.getName(), quitOperation.getTs(), preQuitWaitSecs);
        ITopic<ManagementMessage> managementTopic =
                hazelcastInstance.getTopic(getManagementTopicName());
        // Stop listening management topic so that I do no longer receive its messages
        managementTopic.removeMessageListener(managementRegistrationId);
        managementRegistrationId = null;
        managementTopic.publish(new QuitResult(Instant.now(), quitOperation));
        Timer quitTimer = new Timer(format("%s-quit-timer", hazelcastInstance.getName()), false);
        quitTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                signalTermination();
            }
        }, MILLISECONDS.convert(preQuitWaitSecs, SECONDS));
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
                    log.info("Hazelcast instance '{}' shutting down, and already unregistered from management topic '{}'",
                            getInstanceName(), getManagementTopicName());
                } else {
                    log.info("Hazelcast instance '{}' shutting down, unregistering from management topic '{}'",
                            getInstanceName(), getManagementTopicName());
                    ITopic<ManagementMessage> managementTopic =
                            hazelcastInstance.getTopic(getManagementTopicName());
                    boolean unregistered = managementTopic
                            .removeMessageListener(managementRegistrationId);
                    checkState(unregistered, "Not unregistered from management topic");
                }
                if (lifecycleRegistrationId == null) {
                    log.warn("Hazelcast instance '{}' shut down, but not registered to lifecycle topic '{}'",
                            getInstanceName(), getManagementTopicName());
                } else {
                    log.info("Hazelcast instance '{}' shut down, unregistering from lifecycle topic '{}'",
                            getInstanceName(), getManagementTopicName());
                    boolean unregistered = hazelcastInstance.getLifecycleService()
                            .removeLifecycleListener(lifecycleRegistrationId);
                    checkState(unregistered, "Not unregistered from lifecycle service");
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
