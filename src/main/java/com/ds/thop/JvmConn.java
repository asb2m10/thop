/*
 * Copyright 2016 Pascal Gauthier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ds.thop;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import com.sun.management.OperatingSystemMXBean;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.management.*;
import java.util.Hashtable;

public class JvmConn {
    private long startTime;

    JMXConnector jmxc;
    ThreadMXBean mxthread;
    MemoryMXBean mxmem;
    OperatingSystemMXBean mxos;

    int pid;
    String jvmName;

    JvmConn(int pid) throws Exception {
        String address = sun.management.ConnectorAddressLink.importFrom(pid);
        if ( address == null ) {
            String java_home = java.lang.System.getProperty("java.home");
            File path = new File(java_home + "/lib/management-agent.jar");
            String agentPath = path.getCanonicalPath();
            if ( ! path.exists() )
                throw new FileNotFoundException(agentPath);
            com.sun.tools.attach.VirtualMachine.attach("" + pid).loadAgent(agentPath);
            address = sun.management.ConnectorAddressLink.importFrom(pid);
        }

        JMXServiceURL jmx = new JMXServiceURL(address);
        jmxc = JMXConnectorFactory.connect(jmx);
        MBeanServerConnection server = jmxc.getMBeanServerConnection();
        mxthread = ManagementFactory.newPlatformMXBeanProxy(server,java.lang.management.ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
        mxmem = ManagementFactory.newPlatformMXBeanProxy(server, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);

        RuntimeMXBean runtime = ManagementFactory.newPlatformMXBeanProxy(server, ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
        startTime = runtime.getStartTime();
        jvmName = runtime.getName();
        this.pid = pid;

        mxos = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    }

    public Hashtable<Long, ThreadDesc> getThreads(boolean fullStack) {
        Hashtable<Long, ThreadDesc> ret = new Hashtable<Long, ThreadDesc>();

        long tid[] = mxthread.getAllThreadIds();
        ThreadInfo tinfo[] = mxthread.getThreadInfo(tid, fullStack ? Integer.MAX_VALUE : 1);

        for (int i = 0; i < tid.length; i++) {
            try {
                String name = tinfo[i].getThreadName();

                /** avoid listing ourselves */
                if (name.startsWith("JMX server connection"))
                    continue;
                if (name.startsWith("RMI TCP Connection"))
                    continue;

                StackTraceElement[] st = tinfo[i].getStackTrace();
                if (st.length > 0) {
                    String stack = st[0].toString();
                    ThreadDesc item = new ThreadDesc();
                    item.state = tinfo[i].getThreadState();

                    switch (item.state) {
                        case WAITING:
                        case TIMED_WAITING:
                            stack = "WAIT";
                            break;
                        case BLOCKED:
                            stack = "BLOCKED";
                            break;
                        default:
                            if (stack.startsWith("java.net.PlainSocketImpl.accept") || stack.startsWith("java.net.PlainSocketImpl.socketAccept")) {
                                stack = "SOCKET ACCEPT";
                            } else if (stack.startsWith("java.net.SocketInputStream.socketRead")) {
                                stack = "SOCKET READ";
                            }
                    }

                    item.cpuTm = mxthread.getThreadCpuTime(tid[i]);
                    item.name = name;
                    item.id = tid[i];

                    item.stack = new String[st.length];
                    item.stack[0] = stack;
                    if ( st.length > 1 ) {
                        for(int j=1;j<st.length;j++) {
                            item.stack[j] = st[j].toString();
                        }
                    }
                    ret.put(tid[i], item);
                }
            } catch (NullPointerException e) {
                // on rare times, the thread that as been picked with the getThreadInfo is gone once we read it. Simply
                // pass, and discard it.
            }
        }

        return ret;
    }

    public String getUptime() {
        long delta = System.currentTimeMillis() - startTime;
        return dateDiff(delta);
    }

    public double getProcessCpu() {
        return mxos.getProcessCpuLoad() * 100;
    }

    private String dateDiff(long diff) {
        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = diff / daysInMilli;
        diff = diff % daysInMilli;
        long elapsedHours = diff / hoursInMilli;
        diff = diff % hoursInMilli;
        long elapsedMinutes = diff / minutesInMilli;
        diff = diff % minutesInMilli;
        long elapsedSeconds = diff / secondsInMilli;

        StringBuffer ret = new StringBuffer();

        if ( elapsedDays != 0 )
            ret.append("" + elapsedDays + " day(s) ");

        if ( elapsedHours != 0 )
            ret.append("" + elapsedHours + " hour(s) ");

        if ( elapsedMinutes != 0 )
            ret.append("" + elapsedMinutes + " minute(s) ");

        if ( elapsedSeconds != 0 )
            ret.append("" + elapsedSeconds + " second(s)");

        return ret.toString();
    }

    void close() {
        try {
            jmxc.close();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}
