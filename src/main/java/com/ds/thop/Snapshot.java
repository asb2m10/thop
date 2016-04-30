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

public class Snapshot {
    private JvmConn conn;
    long heapused;
    long heapmax;
    long heapdelta;

    int pid;
    String jvmName;

    Hashtable<Long, ThreadDesc> threads;
    List<ThreadDesc> activeThread;

    String uptime;
    long lastSnapshot;

    /**/
    int running;
    int blocked;
    int waiting;
    double cpuPer;

    Snapshot(JvmConn conn) {
        this.conn = conn;
        pid = conn.pid;
        jvmName = conn.jvmName;

        MemoryUsage memusage = conn.mxmem.getHeapMemoryUsage();
        heapmax = memusage.getMax();
    }

    void refresh() {
        long tm = System.currentTimeMillis();
        Hashtable<Long, ThreadDesc> fresh = conn.getThreads();
        long deltaTm = tm - lastSnapshot;
        lastSnapshot = tm;

        uptime = conn.getUptime();

        if ( threads != null ) {
            for(Long i: fresh.keySet()) {
                if ( threads.containsKey(i) ) {
                    double delta =  fresh.get(i).cpuTm - threads.get(i).cpuTm;
                    fresh.get(i).cpuDelta = delta;
                    fresh.get(i).per = delta / (deltaTm * 10000);
                }
            }
        }

        activeThread = Collections.list(fresh.elements());
        Collections.sort(activeThread);
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
    }
}
