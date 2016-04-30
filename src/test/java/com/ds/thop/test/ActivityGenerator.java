package com.ds.thop.test;

/**
 * Created by asb2m10 on 2016-04-23.
 */
public class ActivityGenerator implements Runnable {

    @Override
    public void run() {
        try {
            while (true) {
                Math.acos(Math.random());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
