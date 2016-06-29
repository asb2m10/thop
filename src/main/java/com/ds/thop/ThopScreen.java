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
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.TerminalSize;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import java.io.IOException;
import java.util.logging.Logger;

public class ThopScreen implements Runnable {
    //private static Logger logger = Logger.getLogger("thop");
    BlockingQueue<KeyStroke> inputKey = new ArrayBlockingQueue<KeyStroke>(128);
    private boolean quitting = false;
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

    public void refresh(Snapshot snapshot, Context ctx) {
        try {
            screen.doResizeIfNecessary();
            TerminalSize terminalSize = screen.getTerminalSize();
            int c = terminalSize.getColumns();

            int stackinfoSpace = 25;
            int thSpace = (c-17) - stackinfoSpace;
            screen.clear();
            TextGraphics g = screen.newTextGraphics();

            g.putString(0, 0, String.format("thop [%s] uptime %s", snapshot.jvmName, snapshot.uptime));
            g.putString(0, 1, String.format("TCPU %6.2f %% PCPU %.2f %% %4d running, %4d waiting, %4d blocked",
                    snapshot.cpuPer, snapshot.cpuProcess, snapshot.running, snapshot.waiting, snapshot.blocked));

            g.putString(0, 2, String.format("MEM %.1fM/%.0fM >> %+.3fM",
                    ((float)snapshot.heapused)/1024000, ((float)snapshot.heapmax)/1024000,
                    ((float)snapshot.heapdelta/1024000)));

            g.setBackgroundColor(TextColor.ANSI.WHITE);
            g.setForegroundColor(TextColor.ANSI.BLACK);

            String f = "  TID     CPU  %-" + thSpace + "s %-" + stackinfoSpace + "s" ;
            String text = String.format(f, "NAME", "STACK");
            g.putString(0,3, text);

            g.setBackgroundColor(TextColor.ANSI.DEFAULT);
            g.setForegroundColor(TextColor.ANSI.DEFAULT);

            int rows = terminalSize.getRows() - 5;
            String strFormat = "%-" + thSpace + "s %-" + stackinfoSpace + "s";
            f = "%5d %7.3f " + strFormat;
            int shownThreads = 0;

            for(int i=0;shownThreads<rows && i<snapshot.activeThread.size();i++) {
                ThreadDesc desc = snapshot.activeThread.get(i);
                if ( ctx.discard(desc) )
                    continue;

                g.putString(0, (shownThreads++)+4, String.format(f, desc.id, desc.per, desc.name, desc.stack));
            }
            screen.refresh();
        } catch (Exception e) {
            handleTerminalException(e);
        }
    }

    public void close() {
        quitting = true;
        try {
            screen.stopScreen();
        } catch (IOException e) {
        }
    }

    void handleKeystroke(KeyStroke key) {
        if ( key.getKeyType() != KeyType.Character )
            return;

        if ( key.getCharacter() == 'c' ) {
            showConfig();
        }

        if ( key.getCharacter() == 'q' ) {
            close();
            System.exit(0);
        }
    }

    KeyStroke pollInput() throws IOException {
        return screen.pollInput();
    }

    public void run() {
        KeyStroke key;
        try {
            while ((key = screen.readInput()) != null) {
                synchronized (this) {
                    inputKey.put(key);
                    wait();
                }
            }
        } catch (Exception e) {
            if ( !quitting )
                handleTerminalException(e);
        }
    }

    /**
     * Handles Terminal Exceptions and quit gracefully.
     * @param e Orignal exception
     */
    public void handleTerminalException(Exception e) {
        close();
        e.printStackTrace();
        System.exit(1);
    }

    public void showConfig() {
        // TODO ...

        // Create panel to hold components
        Panel panel = new Panel();
        panel.setLayoutManager(new GridLayout(2));

        panel.addComponent(new Label("Interval"));
        panel.addComponent(new TextBox());

        panel.addComponent(new Label("CPU Threshold"));
        panel.addComponent(new TextBox());

        panel.addComponent(new EmptySpace(new TerminalSize(0,0))); // Empty space underneath labels
        panel.addComponent(new Button("Submit"));

        // Create window to hold the panel
        BasicWindow window = new BasicWindow();
        window.setComponent(panel);

        // Create gui and start gui
        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
        gui.addWindowAndWait(window);

    }
}
