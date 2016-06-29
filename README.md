Thop - Java Thread Top
======================

Thop is a Java thread monitoring tool similar to the UNIX utility top. It lists the top CPU thread usage and the current stack traces and threads that are blocked.

I've made this tool to understand what the JVM is doing and what it is waiting for. My stuff (especially in production) is always behind multiples VPNs so X (visualvm) is not an option. It uses the  [Lanterna](https://github.com/mabe02/lanterna) pure java library as ssh friendly terminal UI. There are still issues with slow terminals and I  am waiting for the final 3.0 version of lanterna before addressing them.

Todo
----
* Custom gradle startup script to make a out of the box dist and remove tools.jar from the distribution

Usage
-----
Lanterna and tools.jar (from the jdk lib diretory) needs to be on the classpath.

./thop [pid]



