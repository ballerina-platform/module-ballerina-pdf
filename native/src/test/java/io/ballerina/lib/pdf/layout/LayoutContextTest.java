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

package io.ballerina.lib.pdf.layout;

import io.ballerina.lib.pdf.ConversionOptions;
import io.ballerina.lib.pdf.paint.FontManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.ballerina.lib.pdf.ConversionOptions.A4_HEIGHT;
import static io.ballerina.lib.pdf.ConversionOptions.A4_WIDTH;
import static io.ballerina.lib.pdf.ConversionOptions.DEFAULT_FALLBACK_FONT_SIZE;
import static io.ballerina.lib.pdf.ConversionOptions.DEFAULT_MARGIN;
import static io.ballerina.lib.pdf.ConversionOptions.LEGAL_HEIGHT;
import static io.ballerina.lib.pdf.ConversionOptions.LEGAL_WIDTH;
import static io.ballerina.lib.pdf.ConversionOptions.LETTER_HEIGHT;
import static io.ballerina.lib.pdf.ConversionOptions.LETTER_WIDTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link LayoutContext}.
 * Tests constructor initialization, page rule configuration, and computed dimensions.
 */
class LayoutContextTest {

    private static PDDocument document;
    private static FontManager fontManager;

    @BeforeAll
    static void setUp() throws IOException {
        document = new PDDocument();
        fontManager = new FontManager();
        fontManager.loadFonts(document);
    }

    @AfterAll
    static void tearDown() throws IOException {
        document.close();
    }

    private LayoutContext createDefault() {
        ConversionOptions opts = new ConversionOptions(
                DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                null, null, null);
        return new LayoutContext(fontManager, opts);
    }

    private LayoutContext createWithMargins(float top, float right, float bottom, float left) {
        ConversionOptions opts = new ConversionOptions(
                DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                top, right, bottom, left,
                null, null, null);
        return new LayoutContext(fontManager, opts);
    }

    // ===== Constructor =====

    @Test
    void constructorSetsOptionsValues() {
        ConversionOptions opts = new ConversionOptions(
                14f, 600f, 800f,
                10f, 20f, 30f, 40f,
                null, null, null);
        LayoutContext ctx = new LayoutContext(fontManager, opts);

        assertEquals(14f, ctx.getFallbackFontSize(), 0.01f);
        assertEquals(600f, ctx.getPageWidth(), 0.01f);
        assertEquals(800f, ctx.getPageHeight(), 0.01f);
        assertEquals(10f, ctx.getMarginTop(), 0.01f);
        assertEquals(20f, ctx.getMarginRight(), 0.01f);
        assertEquals(30f, ctx.getMarginBottom(), 0.01f);
        assertEquals(40f, ctx.getMarginLeft(), 0.01f);
        assertNotNull(ctx.getFontManager());
    }

    // ===== configureFromPageRule — page sizes =====

    @Test
    void configureFromPageRuleA4() {
        LayoutContext ctx = createDefault();
        ctx.configureFromPageRule("a4", null);
        assertEquals(A4_WIDTH, ctx.getPageWidth(), 0.01f);
        assertEquals(A4_HEIGHT, ctx.getPageHeight(), 0.01f);
    }

    @Test
    void configureFromPageRuleLetter() {
        LayoutContext ctx = createDefault();
        ctx.configureFromPageRule("letter", null);
        assertEquals(LETTER_WIDTH, ctx.getPageWidth(), 0.01f);
        assertEquals(LETTER_HEIGHT, ctx.getPageHeight(), 0.01f);
    }

    @Test
    void configureFromPageRuleLegal() {
        LayoutContext ctx = createDefault();
        ctx.configureFromPageRule("legal", null);
        assertEquals(LEGAL_WIDTH, ctx.getPageWidth(), 0.01f);
        assertEquals(LEGAL_HEIGHT, ctx.getPageHeight(), 0.01f);
    }

    @Test
    void configureFromPageRuleCaseInsensitive() {
        LayoutContext ctx = createDefault();
        ctx.configureFromPageRule("A4", null);
        assertEquals(A4_WIDTH, ctx.getPageWidth(), 0.01f);
        assertEquals(A4_HEIGHT, ctx.getPageHeight(), 0.01f);
    }

    @Test
    void configureFromPageRuleNullPreservesDefaults() {
        LayoutContext ctx = createDefault();
        ctx.configureFromPageRule(null, null);
        assertEquals(A4_WIDTH, ctx.getPageWidth(), 0.01f);
        assertEquals(A4_HEIGHT, ctx.getPageHeight(), 0.01f);
    }

    // ===== configureFromPageRule — margin shorthand =====

    @Test
    void configureFromPageRuleMarginShorthand1() {
        // "20px" → all four margins = 20 * 0.75 = 15pt
        LayoutContext ctx = createDefault();
        ctx.configureFromPageRule(null, "20px");
        float expected = 20f * 0.75f;
        assertEquals(expected, ctx.getMarginTop(), 0.01f);
        assertEquals(expected, ctx.getMarginRight(), 0.01f);
        assertEquals(expected, ctx.getMarginBottom(), 0.01f);
        assertEquals(expected, ctx.getMarginLeft(), 0.01f);
    }

    @Test
    void configureFromPageRuleMarginShorthand2() {
        // "20px 10px" → top/bottom = 15pt, left/right = 7.5pt
        LayoutContext ctx = createDefault();
        ctx.configureFromPageRule(null, "20px 10px");
        assertEquals(20f * 0.75f, ctx.getMarginTop(), 0.01f);
        assertEquals(10f * 0.75f, ctx.getMarginRight(), 0.01f);
        assertEquals(20f * 0.75f, ctx.getMarginBottom(), 0.01f);
        assertEquals(10f * 0.75f, ctx.getMarginLeft(), 0.01f);
    }

    @Test
    void configureFromPageRuleMarginShorthand3() {
        // "20px 10px 15px" → top = 15pt, right/left = 7.5pt, bottom = 11.25pt
        LayoutContext ctx = createDefault();
        ctx.configureFromPageRule(null, "20px 10px 15px");
        assertEquals(20f * 0.75f, ctx.getMarginTop(), 0.01f);
        assertEquals(10f * 0.75f, ctx.getMarginRight(), 0.01f);
        assertEquals(15f * 0.75f, ctx.getMarginBottom(), 0.01f);
        assertEquals(10f * 0.75f, ctx.getMarginLeft(), 0.01f);
    }

    @Test
    void configureFromPageRuleMarginShorthand4() {
        // "20px 10px 15px 5px" → all four individually
        LayoutContext ctx = createDefault();
        ctx.configureFromPageRule(null, "20px 10px 15px 5px");
        assertEquals(20f * 0.75f, ctx.getMarginTop(), 0.01f);
        assertEquals(10f * 0.75f, ctx.getMarginRight(), 0.01f);
        assertEquals(15f * 0.75f, ctx.getMarginBottom(), 0.01f);
        assertEquals(5f * 0.75f, ctx.getMarginLeft(), 0.01f);
    }

    @Test
    void configureFromPageRuleUnrecognizedSizePreservesDefaults() {
        LayoutContext ctx = createDefault();
        ctx.configureFromPageRule("custom-unknown", null);
        // Unrecognized size string should leave dimensions unchanged
        assertEquals(A4_WIDTH, ctx.getPageWidth(), 0.01f);
        assertEquals(A4_HEIGHT, ctx.getPageHeight(), 0.01f);
    }

    // ===== configureFromPageRule — margin unit conversions =====

    @Test
    void configureFromPageRuleMarginInPt() {
        LayoutContext ctx = createDefault();
        ctx.configureFromPageRule(null, "36pt");
        assertEquals(36f, ctx.getMarginTop(), 0.01f);
        assertEquals(36f, ctx.getMarginRight(), 0.01f);
        assertEquals(36f, ctx.getMarginBottom(), 0.01f);
        assertEquals(36f, ctx.getMarginLeft(), 0.01f);
    }

    @Test
    void configureFromPageRuleMarginInMm() {
        LayoutContext ctx = createDefault();
        // 10mm = 10 * 2.8346 ≈ 28.346pt
        ctx.configureFromPageRule(null, "10mm");
        float expected = 10f * 2.8346f;
        assertEquals(expected, ctx.getMarginTop(), 0.1f);
        assertEquals(expected, ctx.getMarginRight(), 0.1f);
    }

    @Test
    void configureFromPageRuleMarginMixedUnits() {
        LayoutContext ctx = createDefault();
        // "20px 10mm 15pt 5px"
        ctx.configureFromPageRule(null, "20px 10mm 15pt 5px");
        assertEquals(20f * 0.75f, ctx.getMarginTop(), 0.1f);
        assertEquals(10f * 2.8346f, ctx.getMarginRight(), 0.1f);
        assertEquals(15f, ctx.getMarginBottom(), 0.1f);
        assertEquals(5f * 0.75f, ctx.getMarginLeft(), 0.1f);
    }

    // ===== Computed dimensions =====

    @Test
    void contentWidthSubtractsMargins() {
        LayoutContext ctx = createWithMargins(0, 50, 0, 30);
        assertEquals(A4_WIDTH - 50 - 30, ctx.getContentWidth(), 0.01f);
    }

    @Test
    void contentHeightSubtractsMargins() {
        LayoutContext ctx = createWithMargins(40, 0, 60, 0);
        assertEquals(A4_HEIGHT - 40 - 60, ctx.getContentHeight(), 0.01f);
    }

    @Test
    void zeroMarginsGiveFullPageDimensions() {
        LayoutContext ctx = createDefault();
        assertEquals(A4_WIDTH, ctx.getContentWidth(), 0.01f);
        assertEquals(A4_HEIGHT, ctx.getContentHeight(), 0.01f);
    }
}
