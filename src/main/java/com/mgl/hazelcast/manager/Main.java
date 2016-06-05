package com.mgl.hazelcast.manager;

import io.airlift.airline.Cli;
import io.airlift.airline.Cli.CliBuilder;
import io.airlift.airline.Help;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    private static final int EXIT_OK = 0;
    private static final int EXIT_ERROR = 1;

    public static void main(String args[]) {

        @SuppressWarnings("unchecked")
        CliBuilder<Runnable> cliBuilder = Cli.<Runnable>builder("hazelcast-manager")
                .withDescription("Hazelcast instances manager")
                .withDefaultCommand(Help.class)
                .withCommands(Starter.class, Quitter.class);

        Cli<Runnable> cli = cliBuilder.build();
        try {
            cli.parse(args).run();
            System.exit(EXIT_OK);
            log.info("Hazelcast instances manager terminating normally");
        } catch (Exception e) {
            log.error("Hazelcast instances manager terminating with error", e);
            System.exit(EXIT_ERROR);
        }
    }

}
