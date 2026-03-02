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

package io.ballerina.lib.pdf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.ballerina.lib.pdf.ConversionOptions.A4_HEIGHT;
import static io.ballerina.lib.pdf.ConversionOptions.A4_WIDTH;
import static io.ballerina.lib.pdf.ConversionOptions.DEFAULT_FALLBACK_FONT_SIZE;
import static io.ballerina.lib.pdf.ConversionOptions.DEFAULT_MARGIN;
import static io.ballerina.lib.pdf.ConversionOptions.LEGAL_HEIGHT;
import static io.ballerina.lib.pdf.ConversionOptions.LEGAL_WIDTH;
import static io.ballerina.lib.pdf.ConversionOptions.LETTER_HEIGHT;
import static io.ballerina.lib.pdf.ConversionOptions.LETTER_WIDTH;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversionOptionsTest {

    // --- pageDimensions ---

    @Test
    void pageDimensionsA4() {
        float[] dims = ConversionOptions.pageDimensions("A4");
        assertArrayEquals(new float[]{A4_WIDTH, A4_HEIGHT}, dims);
    }

    @Test
    void pageDimensionsLetter() {
        float[] dims = ConversionOptions.pageDimensions("LETTER");
        assertArrayEquals(new float[]{LETTER_WIDTH, LETTER_HEIGHT}, dims);
    }

    @Test
    void pageDimensionsLetterLowerCase() {
        float[] dims = ConversionOptions.pageDimensions("letter");
        assertArrayEquals(new float[]{LETTER_WIDTH, LETTER_HEIGHT}, dims);
    }

    @Test
    void pageDimensionsLegal() {
        float[] dims = ConversionOptions.pageDimensions("LEGAL");
        assertArrayEquals(new float[]{LEGAL_WIDTH, LEGAL_HEIGHT}, dims);
    }

    @Test
    void pageDimensionsUnknownDefaultsToA4() {
        float[] dims = ConversionOptions.pageDimensions("TABLOID");
        assertArrayEquals(new float[]{A4_WIDTH, A4_HEIGHT}, dims);
    }

    // --- Validation: fallbackFontSize ---

    @Test
    void rejectsZeroFontSize() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConversionOptions(0, A4_WIDTH, A4_HEIGHT,
                        DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                        null, null, null));
    }

    @Test
    void rejectsNegativeFontSize() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConversionOptions(-1, A4_WIDTH, A4_HEIGHT,
                        DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                        null, null, null));
    }

    // --- Validation: page dimensions ---

    @Test
    void rejectsZeroPageWidth() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConversionOptions(DEFAULT_FALLBACK_FONT_SIZE, 0, A4_HEIGHT,
                        DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                        null, null, null));
    }

    @Test
    void rejectsNegativePageHeight() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConversionOptions(DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, -1,
                        DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                        null, null, null));
    }

    // --- Validation: margins ---

    @Test
    void rejectsNegativeMarginTop() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConversionOptions(DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                        -1, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                        null, null, null));
    }

    @Test
    void rejectsNegativeMarginRight() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConversionOptions(DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                        DEFAULT_MARGIN, -1, DEFAULT_MARGIN, DEFAULT_MARGIN,
                        null, null, null));
    }

    @Test
    void rejectsNegativeMarginBottom() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConversionOptions(DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                        DEFAULT_MARGIN, DEFAULT_MARGIN, -1, DEFAULT_MARGIN,
                        null, null, null));
    }

    @Test
    void rejectsNegativeMarginLeft() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConversionOptions(DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                        DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, -1,
                        null, null, null));
    }

    // --- Validation: maxPages ---

    @Test
    void rejectsZeroMaxPages() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConversionOptions(DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                        DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                        null, null, 0));
    }

    @Test
    void rejectsNegativeMaxPages() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConversionOptions(DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                        DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                        null, null, -5));
    }

    @Test
    void nullMaxPagesIsAllowed() {
        ConversionOptions opts = new ConversionOptions(
                DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                null, null, null);
        assertNull(opts.getMaxPages());
    }

    // --- Getters ---

    @Test
    void gettersReturnConstructorValues() {
        List<ConversionOptions.FontEntry> fonts = List.of(
                new ConversionOptions.FontEntry("TestFont", new byte[]{1, 2, 3}, true, false));
        ConversionOptions opts = new ConversionOptions(
                14f, 500f, 700f, 10f, 20f, 30f, 40f,
                "body { color: red; }", fonts, 5);

        assertEquals(14f, opts.getFallbackFontSize());
        assertEquals(500f, opts.getPageWidth());
        assertEquals(700f, opts.getPageHeight());
        assertEquals(10f, opts.getMarginTop());
        assertEquals(20f, opts.getMarginRight());
        assertEquals(30f, opts.getMarginBottom());
        assertEquals(40f, opts.getMarginLeft());
        assertEquals("body { color: red; }", opts.getAdditionalCss());
        assertEquals(fonts, opts.getCustomFonts());
        assertEquals(5, opts.getMaxPages());
    }
}
