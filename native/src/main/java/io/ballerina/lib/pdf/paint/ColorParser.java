/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.lib.pdf.paint;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses CSS color values: #hex, rgb(), named colors → RgbColor.
 */
public class ColorParser {

    private static final Pattern HEX3 = Pattern.compile("#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])");
    private static final Pattern HEX6 = Pattern.compile("#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})");
    private static final Pattern RGB = Pattern.compile(
            "rgb\\s*\\(\\s*([\\d.]+%?)\\s*,\\s*([\\d.]+%?)\\s*,\\s*([\\d.]+%?)\\s*\\)");
    private static final Pattern RGBA = Pattern.compile(
            "rgba\\s*\\(\\s*([\\d.]+%?)\\s*,\\s*([\\d.]+%?)\\s*,\\s*([\\d.]+%?)\\s*,\\s*([\\d.]+%?)\\s*\\)");

    // All 148 CSS named colors (147 keywords + transparent) per CSS Color Module Level 4.
    private static final Map<String, RgbColor> NAMED_COLORS = Map.ofEntries(
            Map.entry("aliceblue", new RgbColor(240, 248, 255)),
            Map.entry("antiquewhite", new RgbColor(250, 235, 215)),
            Map.entry("aqua", new RgbColor(0, 255, 255)),
            Map.entry("aquamarine", new RgbColor(127, 255, 212)),
            Map.entry("azure", new RgbColor(240, 255, 255)),
            Map.entry("beige", new RgbColor(245, 245, 220)),
            Map.entry("bisque", new RgbColor(255, 228, 196)),
            Map.entry("black", new RgbColor(0, 0, 0)),
            Map.entry("blanchedalmond", new RgbColor(255, 235, 205)),
            Map.entry("blue", new RgbColor(0, 0, 255)),
            Map.entry("blueviolet", new RgbColor(138, 43, 226)),
            Map.entry("brown", new RgbColor(165, 42, 42)),
            Map.entry("burlywood", new RgbColor(222, 184, 135)),
            Map.entry("cadetblue", new RgbColor(95, 158, 160)),
            Map.entry("chartreuse", new RgbColor(127, 255, 0)),
            Map.entry("chocolate", new RgbColor(210, 105, 30)),
            Map.entry("coral", new RgbColor(255, 127, 80)),
            Map.entry("cornflowerblue", new RgbColor(100, 149, 237)),
            Map.entry("cornsilk", new RgbColor(255, 248, 220)),
            Map.entry("crimson", new RgbColor(220, 20, 60)),
            Map.entry("cyan", new RgbColor(0, 255, 255)),
            Map.entry("darkblue", new RgbColor(0, 0, 139)),
            Map.entry("darkcyan", new RgbColor(0, 139, 139)),
            Map.entry("darkgoldenrod", new RgbColor(184, 134, 11)),
            Map.entry("darkgray", new RgbColor(169, 169, 169)),
            Map.entry("darkgreen", new RgbColor(0, 100, 0)),
            Map.entry("darkgrey", new RgbColor(169, 169, 169)),
            Map.entry("darkkhaki", new RgbColor(189, 183, 107)),
            Map.entry("darkmagenta", new RgbColor(139, 0, 139)),
            Map.entry("darkolivegreen", new RgbColor(85, 107, 47)),
            Map.entry("darkorange", new RgbColor(255, 140, 0)),
            Map.entry("darkorchid", new RgbColor(153, 50, 204)),
            Map.entry("darkred", new RgbColor(139, 0, 0)),
            Map.entry("darksalmon", new RgbColor(233, 150, 122)),
            Map.entry("darkseagreen", new RgbColor(143, 188, 143)),
            Map.entry("darkslateblue", new RgbColor(72, 61, 139)),
            Map.entry("darkslategray", new RgbColor(47, 79, 79)),
            Map.entry("darkslategrey", new RgbColor(47, 79, 79)),
            Map.entry("darkturquoise", new RgbColor(0, 206, 209)),
            Map.entry("darkviolet", new RgbColor(148, 0, 211)),
            Map.entry("deeppink", new RgbColor(255, 20, 147)),
            Map.entry("deepskyblue", new RgbColor(0, 191, 255)),
            Map.entry("dimgray", new RgbColor(105, 105, 105)),
            Map.entry("dimgrey", new RgbColor(105, 105, 105)),
            Map.entry("dodgerblue", new RgbColor(30, 144, 255)),
            Map.entry("firebrick", new RgbColor(178, 34, 34)),
            Map.entry("floralwhite", new RgbColor(255, 250, 240)),
            Map.entry("forestgreen", new RgbColor(34, 139, 34)),
            Map.entry("fuchsia", new RgbColor(255, 0, 255)),
            Map.entry("gainsboro", new RgbColor(220, 220, 220)),
            Map.entry("ghostwhite", new RgbColor(248, 248, 255)),
            Map.entry("gold", new RgbColor(255, 215, 0)),
            Map.entry("goldenrod", new RgbColor(218, 165, 32)),
            Map.entry("gray", new RgbColor(128, 128, 128)),
            Map.entry("green", new RgbColor(0, 128, 0)),
            Map.entry("greenyellow", new RgbColor(173, 255, 47)),
            Map.entry("grey", new RgbColor(128, 128, 128)),
            Map.entry("honeydew", new RgbColor(240, 255, 240)),
            Map.entry("hotpink", new RgbColor(255, 105, 180)),
            Map.entry("indianred", new RgbColor(205, 92, 92)),
            Map.entry("indigo", new RgbColor(75, 0, 130)),
            Map.entry("ivory", new RgbColor(255, 255, 240)),
            Map.entry("khaki", new RgbColor(240, 230, 140)),
            Map.entry("lavender", new RgbColor(230, 230, 250)),
            Map.entry("lavenderblush", new RgbColor(255, 240, 245)),
            Map.entry("lawngreen", new RgbColor(124, 252, 0)),
            Map.entry("lemonchiffon", new RgbColor(255, 250, 205)),
            Map.entry("lightblue", new RgbColor(173, 216, 230)),
            Map.entry("lightcoral", new RgbColor(240, 128, 128)),
            Map.entry("lightcyan", new RgbColor(224, 255, 255)),
            Map.entry("lightgoldenrodyellow", new RgbColor(250, 250, 210)),
            Map.entry("lightgray", new RgbColor(211, 211, 211)),
            Map.entry("lightgreen", new RgbColor(144, 238, 144)),
            Map.entry("lightgrey", new RgbColor(211, 211, 211)),
            Map.entry("lightpink", new RgbColor(255, 182, 193)),
            Map.entry("lightsalmon", new RgbColor(255, 160, 122)),
            Map.entry("lightseagreen", new RgbColor(32, 178, 170)),
            Map.entry("lightskyblue", new RgbColor(135, 206, 250)),
            Map.entry("lightslategray", new RgbColor(119, 136, 153)),
            Map.entry("lightslategrey", new RgbColor(119, 136, 153)),
            Map.entry("lightsteelblue", new RgbColor(176, 196, 222)),
            Map.entry("lightyellow", new RgbColor(255, 255, 224)),
            Map.entry("lime", new RgbColor(0, 255, 0)),
            Map.entry("limegreen", new RgbColor(50, 205, 50)),
            Map.entry("linen", new RgbColor(250, 240, 230)),
            Map.entry("magenta", new RgbColor(255, 0, 255)),
            Map.entry("maroon", new RgbColor(128, 0, 0)),
            Map.entry("mediumaquamarine", new RgbColor(102, 205, 170)),
            Map.entry("mediumblue", new RgbColor(0, 0, 205)),
            Map.entry("mediumorchid", new RgbColor(186, 85, 211)),
            Map.entry("mediumpurple", new RgbColor(147, 112, 219)),
            Map.entry("mediumseagreen", new RgbColor(60, 179, 113)),
            Map.entry("mediumslateblue", new RgbColor(123, 104, 238)),
            Map.entry("mediumspringgreen", new RgbColor(0, 250, 154)),
            Map.entry("mediumturquoise", new RgbColor(72, 209, 204)),
            Map.entry("mediumvioletred", new RgbColor(199, 21, 133)),
            Map.entry("midnightblue", new RgbColor(25, 25, 112)),
            Map.entry("mintcream", new RgbColor(245, 255, 250)),
            Map.entry("mistyrose", new RgbColor(255, 228, 225)),
            Map.entry("moccasin", new RgbColor(255, 228, 181)),
            Map.entry("navajowhite", new RgbColor(255, 222, 173)),
            Map.entry("navy", new RgbColor(0, 0, 128)),
            Map.entry("oldlace", new RgbColor(253, 245, 230)),
            Map.entry("olive", new RgbColor(128, 128, 0)),
            Map.entry("olivedrab", new RgbColor(107, 142, 35)),
            Map.entry("orange", new RgbColor(255, 165, 0)),
            Map.entry("orangered", new RgbColor(255, 69, 0)),
            Map.entry("orchid", new RgbColor(218, 112, 214)),
            Map.entry("palegoldenrod", new RgbColor(238, 232, 170)),
            Map.entry("palegreen", new RgbColor(152, 251, 152)),
            Map.entry("paleturquoise", new RgbColor(175, 238, 238)),
            Map.entry("palevioletred", new RgbColor(219, 112, 147)),
            Map.entry("papayawhip", new RgbColor(255, 239, 213)),
            Map.entry("peachpuff", new RgbColor(255, 218, 185)),
            Map.entry("peru", new RgbColor(205, 133, 63)),
            Map.entry("pink", new RgbColor(255, 192, 203)),
            Map.entry("plum", new RgbColor(221, 160, 221)),
            Map.entry("powderblue", new RgbColor(176, 224, 230)),
            Map.entry("purple", new RgbColor(128, 0, 128)),
            Map.entry("rebeccapurple", new RgbColor(102, 51, 153)),
            Map.entry("red", new RgbColor(255, 0, 0)),
            Map.entry("rosybrown", new RgbColor(188, 143, 143)),
            Map.entry("royalblue", new RgbColor(65, 105, 225)),
            Map.entry("saddlebrown", new RgbColor(139, 69, 19)),
            Map.entry("salmon", new RgbColor(250, 128, 114)),
            Map.entry("sandybrown", new RgbColor(244, 164, 96)),
            Map.entry("seagreen", new RgbColor(46, 139, 87)),
            Map.entry("seashell", new RgbColor(255, 245, 238)),
            Map.entry("sienna", new RgbColor(160, 82, 45)),
            Map.entry("silver", new RgbColor(192, 192, 192)),
            Map.entry("skyblue", new RgbColor(135, 206, 235)),
            Map.entry("slateblue", new RgbColor(106, 90, 205)),
            Map.entry("slategray", new RgbColor(112, 128, 144)),
            Map.entry("slategrey", new RgbColor(112, 128, 144)),
            Map.entry("snow", new RgbColor(255, 250, 250)),
            Map.entry("springgreen", new RgbColor(0, 255, 127)),
            Map.entry("steelblue", new RgbColor(70, 130, 180)),
            Map.entry("tan", new RgbColor(210, 180, 140)),
            Map.entry("teal", new RgbColor(0, 128, 128)),
            Map.entry("thistle", new RgbColor(216, 191, 216)),
            Map.entry("tomato", new RgbColor(255, 99, 71)),
            Map.entry("transparent", new RgbColor(0, 0, 0, 0)),
            Map.entry("turquoise", new RgbColor(64, 224, 208)),
            Map.entry("violet", new RgbColor(238, 130, 238)),
            Map.entry("wheat", new RgbColor(245, 222, 179)),
            Map.entry("white", new RgbColor(255, 255, 255)),
            Map.entry("whitesmoke", new RgbColor(245, 245, 245)),
            Map.entry("yellow", new RgbColor(255, 255, 0)),
            Map.entry("yellowgreen", new RgbColor(154, 205, 50))
    );

    /**
     * Parses a CSS color string. Returns null if unparseable.
     */
    public static RgbColor parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        value = value.trim().toLowerCase();

        // Named color
        RgbColor named = NAMED_COLORS.get(value);
        if (named != null) {
            return named;
        }

        // #RRGGBB
        Matcher m6 = HEX6.matcher(value);
        if (m6.matches()) {
            return new RgbColor(
                    Integer.parseInt(m6.group(1), 16),
                    Integer.parseInt(m6.group(2), 16),
                    Integer.parseInt(m6.group(3), 16)
            );
        }

        // #RGB
        Matcher m3 = HEX3.matcher(value);
        if (m3.matches()) {
            return new RgbColor(
                    Integer.parseInt(m3.group(1) + m3.group(1), 16),
                    Integer.parseInt(m3.group(2) + m3.group(2), 16),
                    Integer.parseInt(m3.group(3) + m3.group(3), 16)
            );
        }

        // rgb(r, g, b) — accepts integers (0–255) or percentages (0%–100%)
        Matcher mRgb = RGB.matcher(value);
        if (mRgb.matches()) {
            try {
                return new RgbColor(
                        parseChannel(mRgb.group(1)),
                        parseChannel(mRgb.group(2)),
                        parseChannel(mRgb.group(3))
                );
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // rgba(r, g, b, a) — alpha accepts 0.0–1.0 or 0%–100%
        Matcher mRgba = RGBA.matcher(value);
        if (mRgba.matches()) {
            try {
                float alpha = parseAlpha(mRgba.group(4));
                return new RgbColor(
                        parseChannel(mRgba.group(1)),
                        parseChannel(mRgba.group(2)),
                        parseChannel(mRgba.group(3)),
                        clamp((int) (alpha * 255))
                );
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    private static int parseChannel(String token) {
        if (token.endsWith("%")) {
            float pct = Float.parseFloat(token.substring(0, token.length() - 1));
            return clamp(Math.round(pct * 255 / 100));
        }
        return clamp(Integer.parseInt(token));
    }

    private static float parseAlpha(String token) {
        if (token.endsWith("%")) {
            return Float.parseFloat(token.substring(0, token.length() - 1)) / 100f;
        }
        return Float.parseFloat(token);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
