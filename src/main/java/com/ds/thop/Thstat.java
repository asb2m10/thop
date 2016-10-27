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

import com.ds.thop.ui.ThopScreen;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

public class Thstat implements Runnable {
    String threadStringFormat;
    int strSzThreadName = 80;
    int strSzStack = 35;

    public Thstat(int pid) throws Exception {
        String strFormat = "%-" + strSzThreadName + "." + strSzThreadName + "s %-" + strSzStack + "." +
                strSzStack + "s";
        threadStringFormat = "%6d %7.3f " + strFormat;

        new Thread(this).start();
        JvmConn conn = new JvmConn(pid);
        Snapshot snapshot = new Snapshot(conn);
        Context ctx = new Context();
        ctx.sorttype = Context.SortType.name;

        snapshot.refresh(ctx, false);
        Thread.sleep(ctx.interval);
        snapshot.refresh(ctx, false);

        System.out.println(String.format("TCPU %6.2f %% PCPU %.2f %% %4d running, %4d waiting, %4d blocked",
                snapshot.cpuPer, snapshot.cpuProcess, snapshot.running, snapshot.waiting, snapshot.blocked));
        System.out.println(String.format("MEM %.1fM/%.0fM >> %+.3fM",
                ((float)snapshot.heapused)/1024000, ((float)snapshot.heapmax)/1024000,
                ((float)snapshot.heapdelta/1024000)));

        synchronized (this) {
            String f = "   TID     CPU %-" + strSzThreadName + "s %-" + strSzStack + "s";
            System.out.println(String.format(f, "NAME", "STACK"));

            for (int i = 0; i < snapshot.activeThread.size(); i++) {
                ThreadDesc desc = snapshot.activeThread.get(i);
                if (ctx.discard(desc))
                    continue;
                System.out.println(String.format(threadStringFormat, desc.id, desc.per, desc.name, desc.stack[0]));
            }
        }
    }

    public void run() {
        // Since some terminal can be quite slow, we initialize the terminal while the snapshot is running
        // this gives some time for the terminal to answer. If it takes more time than the snapshot it self,
        // we simply discard the data/and exceptions
        try {
            DefaultTerminalFactory factory = new DefaultTerminalFactory();
            factory.setForceTextTerminal(true);
            Terminal term = factory.createTerminal();
            if ( ! (term instanceof com.googlecode.lanterna.terminal.swing.SwingTerminal ) ) {
                TerminalScreen screen = new TerminalScreen(term);
                TerminalSize terminalSize = screen.getTerminalSize();
                synchronized (this) {
                    strSzThreadName = (terminalSize.getColumns() - 17) - (strSzStack - 1);
                    String strFormat = "%-" + strSzThreadName + "." + strSzThreadName + "s %-" + strSzStack + "." +
                            strSzStack + "s";
                    threadStringFormat = "%6d %7.3f " + strFormat;
                }
            }
        } catch (Exception e) {
        } catch (Error e) {
            // Tried to launch the SwingTerminal on XWindow; we don't care
        }
    }

    public static void main(String args[]) {
        try {
            if ( args.length == 0 ) {
                System.out.println("usage: java <pid>");
                System.exit(1);
            }
            new Thstat(Integer.parseInt(args[0]));
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
