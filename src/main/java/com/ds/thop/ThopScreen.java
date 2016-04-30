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

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.terminal.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.TerminalSize;

import java.io.IOException;

public class ThopScreen implements Runnable {
    TerminalScreen screen;

    public ThopScreen() {
        try {
            DefaultTerminalFactory factory = new DefaultTerminalFactory();
            Terminal term = factory.createTerminal();
            screen = new TerminalScreen(term);
            screen.startScreen();

            TextGraphics g = screen.newTextGraphics();
            g.putString(0,0, "Connected to JVM, waiting for second thread snapshot...");
            screen.refresh();
        } catch (IOException e) {
            System.out.println("Unable to start terminal");
            e.printStackTrace();
            System.exit(1);
        }

        Thread keyListener = new Thread(this);
        keyListener.start();
    }

    public void refresh(Snapshot snapshot) {
        try {
            screen.doResizeIfNecessary();
            TerminalSize terminalSize = screen.getTerminalSize();
            int c = terminalSize.getColumns();

            int thSpace = (c-17) / 2;
            screen.clear();
            TextGraphics g = screen.newTextGraphics();

            g.putString(0, 0, String.format("thop [%s] uptime %s", snapshot.jvmName, snapshot.uptime));
            g.putString(0, 1, String.format("CPU %5.3f %% %4d running, %4d waiting, %4d blocked",
                    snapshot.cpuPer, snapshot.running, snapshot.waiting, snapshot.blocked));

            g.putString(0, 2, String.format("MEM %.1fM/%.0fM >> %+.3fM",
                    ((float)snapshot.heapused)/1024000, ((float)snapshot.heapmax)/1024000,
                    ((float)snapshot.heapdelta/1024000)));

            g.setBackgroundColor(TextColor.ANSI.WHITE);
            g.setForegroundColor(TextColor.ANSI.BLACK);

            String f = "  TID     CPU  %-" + thSpace + "s %-" + thSpace + "s" ;
            String text = String.format(f, "NAME", "STACK");
            g.putString(0,3, text);

            g.setBackgroundColor(TextColor.ANSI.DEFAULT);
            g.setForegroundColor(TextColor.ANSI.DEFAULT);

            int numThread = terminalSize.getRows() - 5;
            String strFormat = "%-" + thSpace + "." + thSpace + "s";
            f = "%5d %7.3f  " + strFormat + " " + strFormat;
            for(int i=0;i<numThread && i<snapshot.activeThread.size();i++) {
                ThreadDesc desc = snapshot.activeThread.get(i);
                g.putString(0,i+4, String.format(f, desc.id, desc.per, desc.name, desc.stack));
            }

            screen.refresh();
        } catch (Exception e) {
            handleTerminalException(e);
        }
    }

    public void close() {
        try {
            screen.stopScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handleKeystroke(KeyStroke key) {
        if ( key.getCharacter() == 'q' ) {
            close();
            System.exit(0);
        }
    }

    public void run() {
        KeyStroke key;
        try {
            while ((key = screen.readInput()) != null) {
                handleKeystroke(key);
            }
        } catch (Exception e) {
            handleTerminalException(e);
        }
    }

    /**
     * Handles Terminal Exceptions and quit gracefully.
     * @param e Orignal exception
     */
    public void handleTerminalException(Exception e) {
        try {
            screen.stopScreen();
        } catch (IOException ie) {
        }
        e.printStackTrace();
        System.exit(1);
    }
}
