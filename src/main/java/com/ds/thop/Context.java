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

import java.util.*;
import java.util.regex.*;
import java.io.*;

public class Context implements Comparator<ThreadDesc> {
    /**
     * Interval between snapshots
     */
    public int interval = 5000;

    /**
     * Threads that should'nt be shown based on regex
     */
    public Pattern blacklist;

    /**
     * Thread cpu usage that should not be shown (in nanoseconds)
     */
    public int cpuUsageThreshold =  200;

    public enum SortType {
        cpu, name;
    }
    public SortType sorttype;

    public Context() throws Exception {
        sorttype = SortType.cpu;
        blacklist = Pattern.compile("^TIBCO EMS TCPLink Reader");       // TODO : remove this
        loadContext();
    }

    /**
     * Test if the thread should be showed base on the name / cpu usage.
     * @param desc the thread to test
     * @return true if the thread must be discard
     */
    public boolean discard(ThreadDesc desc) {
        Matcher m = blacklist.matcher(desc.name);
        if (m.find())
           return true;

        if ( desc.state == Thread.State.BLOCKED )
            return false;

        if ( desc.cpuDelta < cpuUsageThreshold ) {
            return true;
        }

        return false;
    }

    public int compare(ThreadDesc t1, ThreadDesc t2) {
        switch(sorttype) {
            case cpu:
                if (t1.state == Thread.State.BLOCKED && t2.state != Thread.State.BLOCKED)
                    return -1;
                if (t1.cpuDelta>t2.cpuDelta)
                    return -1;
                if (t1.cpuDelta<t2.cpuDelta)
                    return 1;
                return 0;
            case name :
                return t1.name.compareTo(t2.name);
            default :
                return 0;
        }
    }

    public void loadContext() throws Exception {
        Properties prop = new Properties();
        try {
            prop.load(new FileReader(getPropertyCfg()));

            String tmp = prop.getProperty("interval");
            if ( tmp != null )
                interval = Integer.parseInt(tmp);
            tmp = prop.getProperty("cpuUsageThreshold");
            if ( tmp != null )
                cpuUsageThreshold = Integer.parseInt(tmp);
            tmp = prop.getProperty("blacklist");
            if ( tmp != null )
                blacklist = Pattern.compile(tmp);

            tmp = prop.getProperty("sortType");
            if ( tmp != null ) {
                if (tmp.equals("cpu"))
                    sorttype = SortType.cpu;
                else if (tmp.equals("name"))
                    sorttype = SortType.name;
            }
        } catch (FileNotFoundException e) {
            return;
        }
    }

    public void saveContext() throws IOException {
        Properties prop = new Properties();

        prop.put("interval", ""+interval);
        prop.put("cpuUsageThreshold", ""+cpuUsageThreshold);
        prop.put("blacklist", blacklist.pattern());
        switch(sorttype) {
            case cpu :
                prop.put("sortType", "cpu");
                break;
            case name :
                prop.put("sortType", "name");
                break;
        }

        FileWriter fw = new FileWriter(getPropertyCfg());
        prop.store(fw, "");
        fw.close();
    }

    private static File getPropertyCfg() {
        File home = new File(System.getProperty("user.home"));
        return new File(home, ".thop.properties");
    }
}
