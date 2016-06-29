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

import java.util.concurrent.TimeUnit;

import com.googlecode.lanterna.input.KeyStroke;

public class Thop {
    public Thop(int pid) throws Exception {
        JvmConn conn = new JvmConn(pid);
        Snapshot snapshot = new Snapshot(conn);
        Context ctx = new Context();

        snapshot.refresh(ctx);

        ThopScreen screen = new ThopScreen();

        while (true) {
            KeyStroke key = screen.inputKey.poll(ctx.interval, TimeUnit.MILLISECONDS);

            synchronized (screen) {
                if (key != null) {
                    while (key != null) {
                        screen.handleKeystroke(key);
                        key = screen.pollInput();
                    }
                    screen.notify();
                }
            }

            try {
                snapshot.refresh(ctx);
            } catch ( Exception e ) {
                screen.close();
                System.out.println("*** Unable to update JMX status");
                e.printStackTrace();
                System.exit(1);
            }
            screen.refresh(snapshot, ctx);
        }
    }

    public static void main(String args[]) {
        /*System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");*/

        try {
            if ( args.length == 0 ) {
                System.out.println("usage: java <pid>");
                System.exit(1);
            }
            new Thop(Integer.parseInt(args[0]));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
