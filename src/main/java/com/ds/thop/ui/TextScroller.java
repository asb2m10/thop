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

import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.TerminalScreen;

import java.io.IOException;

public class TextScroller {
    TerminalScreen screen;
    TextGraphics g;

    String currentText[];

    private int selected;
    private int scrollPos;
    private int rows;
    private int headerSize;

    public TextScroller(TerminalScreen screen, TextGraphics g, int headerSize, int rows) {
        this.screen = screen;
        this.g = g;
        this.rows = rows - headerSize;
        this.headerSize = headerSize;
    }

    public void setText(String text[]) throws IOException {
        currentText = text;
        selected = 0;
        scrollPos = 0;
        redraw();
    }

    public int handleKeystroke() throws IOException {
        while (true) {
            KeyStroke key = screen.readInput();
            switch (key.getKeyType()) {
                case ArrowUp:
                    if ( selected - 1 < 0 )
                        continue;

                    g.putString(0, (selected + headerSize) - scrollPos, currentText[selected]);
                    selected--;

                    if ( selected - scrollPos < 0 ) {
                        screen.scrollLines(headerSize, (headerSize-1)+rows, -1);
                        scrollPos--;
                    }

                    UITheme.setHighlighText(g);
                    g.putString(0, (selected + headerSize) - scrollPos, currentText[selected]);
                    UITheme.setNormalText(g);

                    screen.refresh();
                    break;
                case ArrowDown:
                    if ( selected + 1 >= currentText.length )
                        continue;

                    g.putString(0, (selected + headerSize) - scrollPos, currentText[selected]);
                    selected++;

                    if ( selected - scrollPos >= rows ) {
                        screen.scrollLines(headerSize, headerSize+rows, 1);
                        scrollPos++;
                    }

                    UITheme.setHighlighText(g);
                    g.putString(0, (selected + headerSize) - scrollPos, currentText[selected]);
                    UITheme.setNormalText(g);

                    screen.refresh();
                    break;
                case ArrowLeft:
                    return -1;
                case ArrowRight:
                case Enter:
                    return selected;
                case Escape:
                    return -1;
                case Character:
                    switch (key.getCharacter()) {
                        case 'h':
                            break;
                        case 'q':
                            return -2;

                    }
            }
        }
    }

    public void redraw() throws IOException {
        for (int i = scrollPos; (i- scrollPos) < rows && i < currentText.length; i++) {
            if (i == selected) {
                UITheme.setHighlighText(g);
                g.putString(0, i + scrollPos + headerSize, currentText[i]);
                UITheme.setNormalText(g);
            } else {
                g.putString(0, i + scrollPos + headerSize, currentText[i]);
            }
        }
        screen.refresh();
    }
}

