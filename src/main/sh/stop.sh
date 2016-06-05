#!/bin/bash

BASEDIR="../../.."

pushd $BASEDIR > /dev/null

mvn "-Dexec.args=-Dlogback.configurationFile=src/main/config/logback.xml
                 -Dhazelcast.logging.type=slf4j
                 -classpath %classpath com.mgl.hazelcast.manager.Main
                     stop
                     -conf-file src/main/config/hazelcast-client-config.xml
                     -mgmnt-topic hz-management-topic
                     -name our-hazelcast-instance" \
    -Dexec.executable=java \
    -DskipTests=true org.codehaus.mojo:exec-maven-plugin:exec

popd > /dev/null

exit $?
