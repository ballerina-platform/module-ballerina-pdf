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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ColorParserTest {

    @Test
    void parsesSixDigitHex() {
        RgbColor c = ColorParser.parse("#ff0000");
        assertNotNull(c);
        assertEquals(255, c.red());
        assertEquals(0, c.green());
        assertEquals(0, c.blue());
    }

    @Test
    void parsesThreeDigitHex() {
        RgbColor c = ColorParser.parse("#f00");
        assertNotNull(c);
        assertEquals(255, c.red());
        assertEquals(0, c.green());
        assertEquals(0, c.blue());
    }

    @Test
    void parsesRgb() {
        RgbColor c = ColorParser.parse("rgb(0, 128, 255)");
        assertNotNull(c);
        assertEquals(0, c.red());
        assertEquals(128, c.green());
        assertEquals(255, c.blue());
    }

    @Test
    void parsesRgba() {
        RgbColor c = ColorParser.parse("rgba(255, 0, 0, 0.5)");
        assertNotNull(c);
        assertEquals(255, c.red());
        assertEquals(0, c.green());
        assertEquals(0, c.blue());
        assertEquals(127, c.alpha());
    }

    @Test
    void parsesNamedColors() {
        assertEquals(new RgbColor(0, 0, 0), ColorParser.parse("black"));
        assertEquals(new RgbColor(255, 255, 255), ColorParser.parse("white"));

        RgbColor navy = ColorParser.parse("navy");
        assertNotNull(navy);
        assertEquals(0, navy.red());
        assertEquals(0, navy.green());
        assertEquals(128, navy.blue());

        RgbColor transparent = ColorParser.parse("transparent");
        assertNotNull(transparent);
        assertEquals(0, transparent.alpha());
    }

    @Test
    void parsesExtendedNamedColors() {
        RgbColor coral = ColorParser.parse("coral");
        assertNotNull(coral);
        assertEquals(255, coral.red());
        assertEquals(127, coral.green());
        assertEquals(80, coral.blue());

        RgbColor crimson = ColorParser.parse("crimson");
        assertNotNull(crimson);
        assertEquals(220, crimson.red());
        assertEquals(20, crimson.green());
        assertEquals(60, crimson.blue());

        RgbColor rebeccapurple = ColorParser.parse("rebeccapurple");
        assertNotNull(rebeccapurple);
        assertEquals(102, rebeccapurple.red());
        assertEquals(51, rebeccapurple.green());
        assertEquals(153, rebeccapurple.blue());

        // cyan and aqua are aliases (both 0, 255, 255)
        RgbColor cyan = ColorParser.parse("cyan");
        RgbColor aqua = ColorParser.parse("aqua");
        assertNotNull(cyan);
        assertNotNull(aqua);
        assertEquals(cyan, aqua);

        // magenta and fuchsia are aliases (both 255, 0, 255)
        RgbColor magenta = ColorParser.parse("magenta");
        RgbColor fuchsia = ColorParser.parse("fuchsia");
        assertNotNull(magenta);
        assertNotNull(fuchsia);
        assertEquals(magenta, fuchsia);

        // grey/gray variants
        RgbColor darkgray = ColorParser.parse("darkgray");
        RgbColor darkgrey = ColorParser.parse("darkgrey");
        assertNotNull(darkgray);
        assertEquals(darkgray, darkgrey);
        assertEquals(169, darkgray.red());

        RgbColor tomato = ColorParser.parse("tomato");
        assertNotNull(tomato);
        assertEquals(255, tomato.red());
        assertEquals(99, tomato.green());
        assertEquals(71, tomato.blue());
    }

    @Test
    void isCaseInsensitive() {
        RgbColor upper = ColorParser.parse("#FF0000");
        assertNotNull(upper);
        assertEquals(255, upper.red());

        RgbColor rgbUpper = ColorParser.parse("RGB(1,2,3)");
        assertNotNull(rgbUpper);
        assertEquals(1, rgbUpper.red());
    }

    @Test
    void returnsNullForInvalid() {
        assertNull(ColorParser.parse("notacolor"));
        assertNull(ColorParser.parse("#xyz"));
    }

    @Test
    void returnsNullForNullOrBlank() {
        assertNull(ColorParser.parse(null));
        assertNull(ColorParser.parse(""));
        assertNull(ColorParser.parse("   "));
    }

    @Test
    void clampsOutOfRangeValues() {
        // Regex only matches non-negative integers, so test with 300 > 255
        RgbColor c = ColorParser.parse("rgb(300, 0, 128)");
        assertNotNull(c);
        assertEquals(255, c.red());
        assertEquals(0, c.green());
        assertEquals(128, c.blue());
    }

    @Test
    void parsesRgbPercentages() {
        RgbColor c = ColorParser.parse("rgb(100%, 50%, 0%)");
        assertNotNull(c);
        assertEquals(255, c.red());
        assertEquals(128, c.green());
        assertEquals(0, c.blue());
    }

    @Test
    void parsesRgbaPercentages() {
        RgbColor c = ColorParser.parse("rgba(100%, 0%, 50%, 0.5)");
        assertNotNull(c);
        assertEquals(255, c.red());
        assertEquals(0, c.green());
        assertEquals(128, c.blue());
        assertEquals(127, c.alpha());
    }

    @Test
    void parsesRgbaAlphaPercentage() {
        RgbColor c = ColorParser.parse("rgba(255, 0, 0, 50%)");
        assertNotNull(c);
        assertEquals(255, c.red());
        assertEquals(127, c.alpha());
    }

    @Test
    void returnsNullForMalformedRgbaAlpha() {
        assertNull(ColorParser.parse("rgba(255, 0, 0, abc)"));
    }
}
