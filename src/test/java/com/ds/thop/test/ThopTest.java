package com.ds.thop.test;

import junit.framework.TestCase;
import com.ds.thop.*;

import java.lang.management.ManagementFactory;

public class ThopTest extends TestCase {
    int pid;

    public void setUp() {
        ActivityGenerator activity = new ActivityGenerator();
        Thread thread = new Thread(activity);
        thread.setDaemon(true);
        thread.start();
        pid = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
    }

    public void testThop() throws Exception {
        // This test actually is actually interactive (it open a window)
        //new Thop(pid);
    }

    public void testThstat() throws Exception {
        new Thstat(pid);
    }
}
