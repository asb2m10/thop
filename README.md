Thop - Java Thread Top
======================

Thop is a Java thread monitoring tool similar to the UNIX utility top. It lists the top CPU thread usage and the current
stack traces. Keep in mind that "stack traces" are like quantum physics were everything is a photo/snapshot so you will
need more than one photo to understand the behaviour. For thop, a thread is running if it has consumed CPU between the
two snapshots. Don't take these numbers for granted, eg: don't use thop to profile your application. But use it if you
have a Java application with an odd behaviour.

I've made this tool to understand what the JVM is doing and what it is waiting for. My stuff (especially in production)
is always behind multiples VPNs so X (visualvm) is not an option. It uses the
[Lanterna](https://github.com/mabe02/lanterna) pure java library as ssh friendly terminal UI. There are still issues
with the terminal IO and I am waiting for the final 3.0 version of lanterna before adressing them.

Todo
----
* Custom gradle startup script to make a out of the box dist and remove tools.jar from the distribution

Usage
-----

./thop [pid]



