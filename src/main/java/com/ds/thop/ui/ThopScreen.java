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

package com.ds.thop.ui;

import com.ds.thop.Context;
import com.ds.thop.Snapshot;
import com.ds.thop.ThreadDesc;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.TerminalSize;

import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import java.io.IOException;
import java.util.logging.Logger;

public class ThopScreen implements Runnable {
    private static Logger logger = Logger.getLogger("thop");

    public BlockingQueue<KeyStroke> inputKey = new ArrayBlockingQueue<KeyStroke>(128);
    private boolean quitting = false;
    TerminalScreen screen;
    Context ctx;

    static int headerSize = 4;
    int strSzStack;
    int strSzThreadName;
    int terminalRows;
    int treminalColumns;

    private static String threadStringFormat;

    public ThopScreen(Context ctx) {
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

        this.ctx = ctx;

        Thread keyListener = new Thread(this);
        keyListener.start();
    }

    public void showHeader(Snapshot snapshot, TextGraphics g) {
        g.putString(0, 0, String.format("thop [%s] uptime %s", snapshot.jvmName, snapshot.uptime));
        g.putString(0, 1, String.format("TCPU %6.2f %% PCPU %.2f %% %4d running, %4d waiting, %4d blocked",
                snapshot.cpuPer, snapshot.cpuProcess, snapshot.running, snapshot.waiting, snapshot.blocked));

        g.putString(0, 2, String.format("MEM %.1fM/%.0fM >> %+.3fM",
                ((float)snapshot.heapused)/1024000, ((float)snapshot.heapmax)/1024000,
                ((float)snapshot.heapdelta/1024000)));
    }

    public void refresh(Snapshot snapshot, Context ctx) {
        try {
            resizeTerminal();
            screen.clear();
            TextGraphics g = screen.newTextGraphics();

            showHeader(snapshot, g);

            UITheme.setHighlighText(g);

            String f = "   TID     CPU %-" + strSzThreadName + "s %-" + strSzStack + "s" ;
            String text = String.format(f, "NAME", "STACK");
            g.putString(0,3, text);

            UITheme.setNormalText(g);

            int shownThreads = 0;
            int rows = terminalRows - headerSize;

            for(int i=0;shownThreads<rows && i<snapshot.activeThread.size();i++) {
                ThreadDesc desc = snapshot.activeThread.get(i);
                if ( ctx.discard(desc) )
                    continue;
                g.putString(0, (shownThreads++)+headerSize, String.format(threadStringFormat, desc.id, desc.per, desc.name, desc.stack[0]));
            }
            screen.refresh();
        } catch (Exception e) {
            handleTerminalException(e);
        }
    }

    void resizeTerminal() {
        screen.doResizeIfNecessary();
        TerminalSize terminalSize = screen.getTerminalSize();
        treminalColumns = terminalSize.getColumns();
        terminalRows = terminalSize.getRows();
        strSzStack = 35;
        strSzThreadName = (treminalColumns-17) - (strSzStack-1);

        String strFormat = "%-" + strSzThreadName + "." + strSzThreadName + "s %-" + strSzStack + "." +
                strSzStack + "s";
        threadStringFormat = "%6d %7.3f " + strFormat;
    }

    /**
     * Closes the current terminal session
     */
    public void close() {
        quitting = true;
        try {
            screen.stopScreen();
        } catch (IOException e) {
        }
    }

    public void quit() {
        close();
        System.exit(0);
    }

    /**
     * Handle auto-refresh keystroke
     * @param key the key pressed
     * @return true if we need to go into browsingMode;
     */
    public boolean handleKeystroke(KeyStroke key) {
        if ( key.getKeyType() == KeyType.Escape ) {
            close();
            System.exit(0);
        }

        if ( key.getKeyType() != KeyType.Character )
            return false;

        switch(key.getCharacter()) {
            case 'h':
                ThopDialog.showHelp(screen);
                return false;
            case 'c':
                ThopDialog.showConfig(screen, ctx);
                return false;
            case 'q':
                close();
                System.exit(0);
                return false;
            case 's':
                return true;
        }
        return false;
    }

    public KeyStroke pollInput() throws IOException {
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

    public void enterBrowsingMode(Snapshot snapshot) {
        String threadText[] = new String[snapshot.activeThread.size()];

        resizeTerminal();

        for(int i=0;i<threadText.length;i++) {
            ThreadDesc desc = snapshot.activeThread.get(i);
            threadText[i] = String.format(threadStringFormat, desc.id, desc.per, desc.name, desc.stack[0]);
        }

        try {
            screen.clear();
            TextGraphics g = screen.newTextGraphics();

            showHeader(snapshot, g);
            UITheme.setHighlighText(g);
            g.putString(treminalColumns-15, 0, "**SNAPSHOT**");
            UITheme.setNormalText(g);
            String f = "   TID     CPU %-" + strSzThreadName + "s %-" + (strSzStack) + "s" ;
            String text = String.format(f, "NAME", "STACK");
            g.putString(0,3, text);

            TextScroller threadScroll = new TextScroller(screen, g, headerSize, terminalRows);
            TextScroller stackScroll = new TextScroller(screen, g, headerSize+2, terminalRows);
            threadScroll.setText(threadText);

            int selectedThread = 0;

            while( (selectedThread = threadScroll.handleKeystroke()) >= 0 ) {
                String stack[] = snapshot.activeThread.get(selectedThread).stack;
                stack = stack.clone();

                for(int i=0;i<stack.length;i++) {
                    String fmt = "    %-" + treminalColumns + "s";
                    stack[i] = String.format(fmt, stack[i]);
                }

                screen.clear();
                showHeader(snapshot, g);
                UITheme.setHighlighText(g);
                g.putString(treminalColumns-15, 0, "**SNAPSHOT**");
                UITheme.setNormalText(g);

                g.putString(0, 3, text);
                g.putString(0, 4, threadText[selectedThread]);

                stackScroll.setText(stack);
                int rc;
                while( (rc = stackScroll.handleKeystroke()) >= 0 ) {
                    if ( rc == -2 )
                        quit();
                }

                screen.clear();
                showHeader(snapshot, g);

                UITheme.setHighlighText(g);
                g.putString(treminalColumns-15, 0, "**SNAPSHOT**");
                UITheme.setNormalText(g);
                g.putString(0,3, text);

                threadScroll.redraw();
            }

            if ( selectedThread == -2 )
                quit();
       } catch (Exception e) {
            handleTerminalException(e);
        }
    }

    public void showSnapshotInProgress() throws IOException {
        TextGraphics g =screen.newTextGraphics();
        g.putString(50, 0, "  *** SNAPHSOT IN PROGRESS ***  ", SGR.BLINK);
        screen.refresh();
    }

}
