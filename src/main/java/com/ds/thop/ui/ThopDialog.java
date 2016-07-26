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

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.ds.thop.Context;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.screen.TerminalScreen;

public class ThopDialog {

    static public void showConfig(TerminalScreen screen, final Context ctx) {
        Panel panel = new Panel();
        panel.setLayoutManager(new GridLayout(2));

        panel.addComponent(new Label("Refresh interval (ms)"));
        final TextBox interval = new TextBox(new TerminalSize(30, 1), ""+ctx.interval);
        panel.addComponent(interval);

        panel.addComponent(new Label("Sort type"));
        final ComboBox<String> sortType = new ComboBox<String>();

        sortType.addItem("CPU usage");
        sortType.addItem("thread name");

        if ( ctx.sorttype == Context.SortType.name ) {
            sortType.setSelectedIndex(1);
        }

        panel.addComponent(sortType);

        panel.addComponent(new Label("Hide threads that starts with (regex)"));
        final TextBox blacklist = new TextBox(new TerminalSize(30, 1), ctx.blacklist.pattern());
        panel.addComponent(blacklist);

        panel.addComponent(new Label("CPU threshold (ns)"));
        final TextBox threshold =  new TextBox(new TerminalSize(30, 1), ""+ctx.cpuUsageThreshold);
        panel.addComponent(threshold);

        final BasicWindow window = new BasicWindow("Thop Config");

        panel.addComponent(new EmptySpace(new TerminalSize(30, 1)));
        panel.addComponent(new EmptySpace(new TerminalSize(30, 1)));

        final MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));

        new Button("OK", new Runnable() {
            @Override
            public void run() {
                int temp = Integer.parseInt(interval.getText());
                if (temp < 500)
                    temp = 500;
                ctx.interval = temp;
                if (sortType.getSelectedIndex() == 0)
                    ctx.sorttype = Context.SortType.cpu;
                else
                    ctx.sorttype = Context.SortType.name;
                ctx.cpuUsageThreshold = Integer.parseInt(threshold.getText());

                try {
                    ctx.blacklist = Pattern.compile(blacklist.getText());
                } catch ( PatternSyntaxException e ) {
                    MessageDialog.showMessageDialog(gui, "Error", "Wrong regex: " + blacklist.getText());
                }

                try {
                    ctx.saveContext();
                } catch ( IOException e ) {
                    MessageDialog.showMessageDialog(gui, "Error", "Unable to save ~/.thop.properties: " + e.getMessage());
                }
                window.close();
            }
        }).addTo(panel);

        new Button("Cancel", new Runnable() {
            @Override
            public void run() {
                window.close();
            }
        }).addTo(panel);

        window.setComponent(panel);
        window.setHints(Arrays.asList(Window.Hint.CENTERED));
        gui.addWindowAndWait(window);
    }

    static public void showHelp(TerminalScreen screen) {
        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
        MessageDialog.showMessageDialog(gui, "", "Thop Help\n--------------------------------\n" +
                "c - config settings dialog\nh - this minimalist help dialog\nq - quits\ns - browse trough stacktrace with current snapshot");
    }
}
