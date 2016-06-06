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
`hazelcast/target/hazelcast-manager-<version>-bin.zip`, comprising the
following directories:

- `bin/`: UNIX and Windows launcher script and other native
  dependencies to support the "system service" features.
- `lib/`: This is where Hazelcast's and your own jars, together with
  other dependencies are saved.
- `logs/`: Plain empty directory where the distribution will log to by
  default.
- `etc/`: Directory for all configuration files (Hazelcast, logging and
  system service). These will be explained next.

## Configuration and Usage

You can use this project to `start` several nodes pointing to specific
XML configuration file(s). In order to selectively `stop` a node you
need to assign different names to each node via the `-name` command
line option.

Every started node listens on a Topic named after `-mgmnt-topic` for a
quit message. When the information in this message includes a specific
node `-name`, it will itself terminate.

You can look into the `Starter` and `Quitter` classes (and the
`BaseCommand` parent for both to find out the specific parameters for
starting and terminating a node, respectively.

What I do is just symlink `bin/hazelcast-manager` into my server's
`/etc/init.d` directory and then use `udate-rc.d` to configure its
runlevels.

The configuration used for the system service is built via the
`<configuration/>` section of the `appassembler-maven-plugin` in the
POM. This ends up in the `etc/wrapper-hazelcast-manager.conf` file
bundled in the final zipfile and can also be edited directly in that
file for other deployments.

## Final

If you are interested in using this feature project and would like
more information please sure file an issue and I will be glad to help!
