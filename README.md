# Bundle Hazelcast and your dependencies into a system service

Hazelcast is an awesome middleware providing distributed data
structures, caching, pub/sub mechanisms and much more.

It is perfectly possible to just download its distribution and start a
node via `java -jar ...` pointing it to your configuration etc.

This (maven) project takes an alternative approach and builds a
zipfile bundling Hazelcast (and other) dependencies, a directory
structure for libs, configs, etc. and SLF4J based logging using
logback as the underlying implementation.

## Motivation

The idea is to be able to run a cluster node as a UNIX `init.d` script that
can be _start_-ed and _stop_-ed and put into a OS startup process via
`update-rd.d` (Debian based distros) or a similar tool.

There is also a **convenient** feature: for several usages
such as IMap, IQueue, Topic, etc. a Hazelcast node does not need to
"know" the java classes used because it all works via
serialisation. For example, if you are using Hazelcast nodes as a
shared cache implementation, only clients need to have the data
classes in their classpath, not the cluster nodes themselves.

However, for other usages such as Distributed Executor Service or Map
Reduce, your code submitted to the cluster (JVM) may need to know the
classes at hand. If you are using maven for the rest of your build the
convenience here is that you can just add whichever of your other
dependencies to the POM as `runtime` dependencies and that'll make
nodes started with this project be able to use these classes.

## Building

You'll need JDK 8 and a recent stable version of maven in your path. I
am using the latest version of Hazelcast at the time of publishing
(3.6.3) but that should be easily changed in the `hazelcast.version`
POM property.

If you cannot use Java 8 it should not be difficult to port the code
to an older version; I am not using many new features other than the
`java.time` API.

So just:

    $ cd hazelcast-manager
    $ mvn clean install

You'll find the zipped bundle in
`hazelcast/target/hazelcast-manager-<version>-bin.zip`.

## Configuration and Usage

TODO
