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

import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.logging.*;

public class Snapshot {
    private static Logger logger = Logger.getLogger("thop");

    private JvmConn conn;

    /**
     * Memory currently used by the heap
     */
    public long heapused;

    /**
     * Maximum heap memory usage
     */
    public long heapmax;

    /**
     * Difference of the heap between the last snapshot
     */
    public long heapdelta;

    /**
     * JVM Name
     */
    public String jvmName;
    public int pid;

    /**
     * Currently running threads
     */
    Hashtable<Long, ThreadDesc> threads;

    /**
     * Thread sorted
     */
    public List<ThreadDesc> activeThread;

    /**
     * Uptime of the JVM
     */
    public String uptime;

    /**
     * Time when the last snapshot was taken. We used this to determine the CPU usage.
     */
    long lastSnapshot;

    /**
     *  Number of running thread
     */
    public int running;

    /**
     * Number of blocked thread
     */
    public int blocked;

    /**
     * Number of waiting threads
     */
    public int waiting;

    /**
     * CPU usage for all thread (less JMX and GC)
     */
    public  double cpuPer;

    /**
     * Global CPU for the JVM
     */
    public double cpuProcess;

    Snapshot(JvmConn conn) {
        this.conn = conn;
        pid = conn.pid;
        jvmName = conn.jvmName;

        MemoryUsage memusage = conn.mxmem.getHeapMemoryUsage();
        heapmax = memusage.getMax();

        /*
        try {
            FileHandler fh = new FileHandler("mylog.txt");
            fh.setFormatter(new java.util.logging.SimpleFormatter());
            //logger.removeHandler(logger.getHandlers()[0]);
            logger.addHandler(fh);
        } catch( IOException e) {

        }*/
    }

    void refresh(Context context, boolean fullStack) {
        long tm = System.currentTimeMillis();
        Hashtable<Long, ThreadDesc> fresh = conn.getThreads(fullStack);
        long deltaTm = tm - lastSnapshot;
        lastSnapshot = tm;

        uptime = conn.getUptime();

        if ( threads != null ) {
            for(Long i: fresh.keySet()) {
                if ( threads.containsKey(i) ) {
                    double delta =  fresh.get(i).cpuTm - threads.get(i).cpuTm;
                    fresh.get(i).cpuDelta = delta;

                    double per = delta / (deltaTm * 10000);

                    // if this is over 100%, it's probably because the lastSnapshot was slightly delayed
                    // and causes an (impossible) over 100% cpu
                    if ( per > 100 )
                        per = 99.999;
                    fresh.get(i).per = per;
                }
            }
        }

        activeThread = Collections.list(fresh.elements());
        Collections.sort(activeThread, context);
        threads = fresh;

        double totalCpu = 0;
        running = 0;
        waiting = 0;
        blocked = 0;

        for (ThreadDesc i: activeThread) {
            switch (i.state) {
                case BLOCKED:
                    blocked++;
                    break;
                case WAITING:
                case TIMED_WAITING:
                    waiting++;
            }
            if ( i.cpuDelta > 0 ) {
                running++;
                totalCpu += i.cpuDelta;
            }
        }

        cpuPer = totalCpu / (deltaTm * 10000);

        MemoryUsage memusage = conn.mxmem.getHeapMemoryUsage();
        long curheap = memusage.getUsed();

        if ( heapused != 0 ) {
            heapdelta = curheap - heapused;
        }
        heapused = curheap;

        cpuProcess = conn.getProcessCpu();
    }
}
