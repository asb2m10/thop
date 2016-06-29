package com.ds.thop.test;

import junit.framework.TestCase;
import com.ds.thop.*;

import java.lang.management.ManagementFactory;

public class ThopTest extends TestCase {
    public void testThop() throws Exception {
        ActivityGenerator activity = new ActivityGenerator();
        Thread thread = new Thread(activity);
        thread.setDaemon(true);
        thread.start();

        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        new Thop(Integer.parseInt(pid));
    }
}
