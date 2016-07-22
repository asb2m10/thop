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

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

public class UITheme {
    static void setHighlighText(TextGraphics g) {
        g.setBackgroundColor(TextColor.ANSI.WHITE);
        g.setForegroundColor(TextColor.ANSI.BLACK);
    }

    static void setNormalText(TextGraphics g) {
        g.setBackgroundColor(TextColor.ANSI.DEFAULT);
        g.setForegroundColor(TextColor.ANSI.DEFAULT);
    }
}
