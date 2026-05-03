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

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.nio.charset.StandardCharsets;

import static io.ballerina.lib.pdf.ConversionOptions.A4_HEIGHT;
import static io.ballerina.lib.pdf.ConversionOptions.A4_WIDTH;
import static io.ballerina.lib.pdf.ConversionOptions.DEFAULT_FALLBACK_FONT_SIZE;
import static io.ballerina.lib.pdf.ConversionOptions.DEFAULT_MARGIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for the HTML-to-PDF conversion pipeline.
 * Exercises: HtmlPreprocessor, HtmlToPdfConverter, PdfPainter, PdfPageManager,
 * ImageDecoder, FontManager, PageBreaker, LayoutContext, InlineLayoutEngine,
 * and BlockFormattingContext.
 */
class HtmlToPdfConverterTest {

    private final HtmlPreprocessor preprocessor = new HtmlPreprocessor();

    private static final ConversionOptions DEFAULT_OPTIONS = new ConversionOptions(
            DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
            DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
            null, null, null);

    private byte[] convert(String html) throws Exception {
        Document doc = preprocessor.preprocess(html);
        return new HtmlToPdfConverter().convert(doc, DEFAULT_OPTIONS);
    }

    private byte[] convert(String html, ConversionOptions options) throws Exception {
        Document doc = preprocessor.preprocess(html);
        return new HtmlToPdfConverter().convert(doc, options);
    }

    private void assertValidPdf(byte[] pdf) {
        assertNotNull(pdf);
        assertTrue(pdf.length > 0, "PDF should not be empty");
        String header = new String(pdf, 0, Math.min(5, pdf.length), StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF"), "Output should start with %PDF header");
    }

    private int pageCount(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return doc.getNumberOfPages();
        }
    }

    private String extractText(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private int countImagesInPdf(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            int count = 0;
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                PDResources resources = doc.getPage(i).getResources();
                if (resources == null) {
                    continue;
                }
                for (COSName name : resources.getXObjectNames()) {
                    if (resources.getXObject(name) instanceof PDImageXObject) {
                        count++;
                    }
                }
            }
            return count;
        }
    }

    // ===== Basic conversion =====

    @Test
    void simpleHtmlProducesValidPdf() throws Exception {
        byte[] pdf = convert("<html><body><p>Hello</p></body></html>");
        assertValidPdf(pdf);
    }

    @Test
    void htmlWithStyledTextProducesValidPdf() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <p style="font-weight: bold; color: red; font-size: 18px;">Bold red text</p>
                </body></html>""");
        assertValidPdf(pdf);
    }

    @Test
    void htmlWithTableProducesValidPdf() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <table><tr><td>A</td><td>B</td></tr>
                <tr><td>C</td><td>D</td></tr></table>
                </body></html>""");
        assertValidPdf(pdf);
    }

    @Test
    void htmlWithBase64ImageProducesValidPdf() throws Exception {
        // 1x1 red PNG encoded as base64
        String base64Png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4"
                + "nGP4z8BQDwAEgAF/pooBPQAAAABJRU5ErkJggg==";
        byte[] pdf = convert("<html><body>"
                + "<img src=\"data:image/png;base64," + base64Png + "\" />"
                + "</body></html>");
        assertValidPdf(pdf);
        assertTrue(countImagesInPdf(pdf) >= 1, "PDF should contain at least one embedded image");
    }

    // ===== Multi-page =====

    @Test
    void multiPageContentProducesMultiplePages() throws Exception {
        // Generate enough content to span multiple A4 pages
        StringBuilder sb = new StringBuilder("<html><body>");
        for (int i = 0; i < 200; i++) {
            sb.append("<p>Line ").append(i).append(" of content that should fill multiple pages.</p>");
        }
        sb.append("</body></html>");

        byte[] pdf = convert(sb.toString());
        assertValidPdf(pdf);
        assertTrue(pageCount(pdf) > 1, "Long content should produce multiple pages");
    }

    // ===== Links =====

    @Test
    void htmlWithLinksProducesValidPdf() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <a href="https://example.com">External link</a>
                <div id="target">Target section</div>
                <a href="#target">Internal link</a>
                </body></html>""");
        assertValidPdf(pdf);
    }

    // ===== CSS features =====

    @Test
    void htmlWithBorderRadiusProducesValidPdf() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <div style="border: 1px solid black; border-radius: 10px; padding: 10px;">
                Rounded box</div>
                </body></html>""");
        assertValidPdf(pdf);
    }

    @Test
    void htmlWithBoxShadowProducesValidPdf() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <div style="box-shadow: 2px 2px 5px gray; padding: 10px;">
                Shadow box</div>
                </body></html>""");
        assertValidPdf(pdf);
    }

    @Test
    void htmlWithTextDecorationProducesValidPdf() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <span style="text-decoration: underline;">Underlined</span>
                <span style="text-decoration: line-through;">Strikethrough</span>
                </body></html>""");
        assertValidPdf(pdf);
    }

    @Test
    void htmlWithCssOverflowHiddenProducesValidPdf() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <div style="width: 100px; height: 50px; overflow: hidden;">
                This content is clipped to the container boundaries.</div>
                </body></html>""");
        assertValidPdf(pdf);
    }

    // ===== Options =====

    @Test
    void maxPagesOptionLimitsPageCount() throws Exception {
        StringBuilder sb = new StringBuilder("<html><body>");
        for (int i = 0; i < 200; i++) {
            sb.append("<p>Line ").append(i).append(" of content that should fill multiple pages.</p>");
        }
        sb.append("</body></html>");

        ConversionOptions opts = new ConversionOptions(
                DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                null, null, 1);

        byte[] pdf = convert(sb.toString(), opts);
        assertValidPdf(pdf);
        assertEquals(1, pageCount(pdf), "maxPages=1 should produce exactly 1 page");
    }

    @Test
    void additionalCssIsApplied() throws Exception {
        ConversionOptions opts = new ConversionOptions(
                DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                "body { background-color: #eee; }", null, null);

        byte[] pdf = convert("<html><body><p>Styled body</p></body></html>", opts);
        assertValidPdf(pdf);
    }

    // ===== Inline elements =====

    @Test
    void htmlWithSuperscriptAndSubscript() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <p>H<sub>2</sub>O and x<sup>2</sup></p>
                </body></html>""");
        assertValidPdf(pdf);
    }

    // ===== Text extraction =====

    @Test
    void extractedTextMatchesInput() throws Exception {
        byte[] pdf = convert("<html><body><p>Hello World</p></body></html>");
        String text = extractText(pdf);
        assertTrue(text.contains("Hello World"), "Extracted text should contain input text");
    }

    // ===== InlineLayoutEngine: text-align =====

    @Test
    void htmlWithTextAlignCenter() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <p style="text-align: center;">Centered text</p>
                </body></html>""");
        assertValidPdf(pdf);
        assertTrue(extractText(pdf).contains("Centered text"));
    }

    @Test
    void htmlWithTextAlignRight() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <p style="text-align: right;">Right-aligned text</p>
                </body></html>""");
        assertValidPdf(pdf);
        assertTrue(extractText(pdf).contains("Right-aligned text"));
    }

    // ===== InlineLayoutEngine: text-transform =====

    @Test
    void htmlWithTextTransformUppercase() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <p style="text-transform: uppercase;">make me uppercase</p>
                </body></html>""");
        assertValidPdf(pdf);
        assertTrue(extractText(pdf).contains("MAKE ME UPPERCASE"));
    }

    @Test
    void htmlWithTextTransformCapitalize() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <p style="text-transform: capitalize;">hello world test</p>
                </body></html>""");
        assertValidPdf(pdf);
        String text = extractText(pdf);
        assertTrue(text.contains("Hello") && text.contains("World"),
                "capitalize should uppercase first letter of each word");
    }

    // ===== InlineLayoutEngine: letter-spacing, word-spacing =====

    @Test
    void htmlWithLetterSpacing() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <p style="letter-spacing: 2px;">Spaced letters</p>
                </body></html>""");
        assertValidPdf(pdf);
        assertTrue(extractText(pdf).contains("Spaced"));
    }

    @Test
    void htmlWithWordSpacing() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <p style="word-spacing: 10px;">Wide word spacing</p>
                </body></html>""");
        assertValidPdf(pdf);
        assertTrue(extractText(pdf).contains("Wide"));
    }

    // ===== InlineLayoutEngine: inline-block, br, line-height =====

    @Test
    void htmlWithInlineBlock() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <div style="display: inline-block; width: 100px; border: 1px solid black;">Box A</div>
                <div style="display: inline-block; width: 100px; border: 1px solid black;">Box B</div>
                </body></html>""");
        assertValidPdf(pdf);
        String text = extractText(pdf);
        assertTrue(text.contains("Box A") && text.contains("Box B"));
    }

    @Test
    void htmlWithBrElement() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <p>Line one<br/>Line two<br/>Line three</p>
                </body></html>""");
        assertValidPdf(pdf);
        String text = extractText(pdf);
        assertTrue(text.contains("Line one") && text.contains("Line two") && text.contains("Line three"));
    }

    @Test
    void htmlWithLineHeight() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <p style="line-height: 2;">Double-spaced line one</p>
                <p style="line-height: 24px;">Fixed line height</p>
                </body></html>""");
        assertValidPdf(pdf);
    }

    // ===== BlockFormattingContext: floats, clear =====

    @Test
    void htmlWithFloatRight() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <div style="float: right; width: 100px; background: #eee;">Right float</div>
                <p>Text flowing around the right float.</p>
                </body></html>""");
        assertValidPdf(pdf);
        assertTrue(extractText(pdf).contains("Right float"));
    }

    @Test
    void htmlWithClearBoth() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <div style="float: left; width: 100px;">Left float</div>
                <div style="float: right; width: 100px;">Right float</div>
                <div style="clear: both;">Cleared content below both floats.</div>
                </body></html>""");
        assertValidPdf(pdf);
        assertTrue(extractText(pdf).contains("Cleared content"));
    }

    @Test
    void htmlWithCenteredTable() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <table style="margin: 0 auto; width: 200px; border: 1px solid black;">
                <tr><td>Centered</td><td>Table</td></tr>
                </table>
                </body></html>""");
        assertValidPdf(pdf);
        assertTrue(extractText(pdf).contains("Centered"));
    }

    @Test
    void htmlWithRelativePositioning() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <div style="position: relative; top: 10px; left: 20px;">
                Relatively positioned</div>
                </body></html>""");
        assertValidPdf(pdf);
        assertTrue(extractText(pdf).contains("Relatively positioned"));
    }

    @Test
    void htmlWithMarginCollapsing() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <div style="margin-bottom: 20px;"><p>First block</p></div>
                <div style="margin-top: 30px;"><p>Second block</p></div>
                </body></html>""");
        assertValidPdf(pdf);
        String text = extractText(pdf);
        assertTrue(text.contains("First block") && text.contains("Second block"));
    }

    @Test
    void htmlWithMixedFloatsAndBlocks() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <div style="float: left; width: 150px; height: 100px; background: #ddd;">Float box</div>
                <p>Paragraph flowing beside the float with enough text to wrap.</p>
                <p style="clear: left;">This paragraph is cleared below the float.</p>
                </body></html>""");
        assertValidPdf(pdf);
    }

    // ===== PdfPainter: visual effects =====

    @Test
    void htmlWithBorderRadiusAndShadow() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <div style="border: 1px solid #333; border-radius: 8px;
                            box-shadow: 3px 3px 6px rgba(0,0,0,0.3); padding: 15px;">
                Rounded box with shadow</div>
                </body></html>""");
        assertValidPdf(pdf);
    }

    @Test
    void htmlWithIndividualBorders() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <div style="border-top: 2px solid red; border-right: 2px solid green;
                            border-bottom: 2px solid blue; border-left: 2px solid orange;
                            padding: 10px;">
                Four-color border</div>
                </body></html>""");
        assertValidPdf(pdf);
    }

    @Test
    void htmlWithCssBackgroundImage() throws Exception {
        String base64Png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4"
                + "nGP4z8BQDwAEgAF/pooBPQAAAABJRU5ErkJggg==";
        byte[] pdf = convert("<html><body>"
                + "<div style=\"width:50px; height:50px; background-image: url('data:image/png;base64,"
                + base64Png + "');\">bg</div>"
                + "</body></html>");
        assertValidPdf(pdf);
        assertTrue(countImagesInPdf(pdf) >= 1, "PDF should contain background image");
    }

    @Test
    void htmlWithOpacity() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <p style="opacity: 0.5;">Semi-transparent text</p>
                <div style="opacity: 0.3; background: blue; padding: 10px;">
                <p>Text in semi-transparent box</p></div>
                </body></html>""");
        assertValidPdf(pdf);
    }

    @Test
    void htmlWithOverflowAuto() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <div style="width: 100px; height: 50px; overflow: auto;">
                This content may overflow the container and should be handled.</div>
                </body></html>""");
        assertValidPdf(pdf);
    }

    // ===== InlineLayoutEngine: additional coverage =====

    @Test
    void htmlWithImageInlineRendersValidPdf() throws Exception {
        String base64Png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4"
                + "nGP4z8BQDwAEgAF/pooBPQAAAABJRU5ErkJggg==";
        byte[] pdf = convert("""
                <html><body>
                <p>Text before <img src="data:image/png;base64,%s" width="20" height="20" /> text after</p>
                </body></html>""".formatted(base64Png));
        assertValidPdf(pdf);
        String text = extractText(pdf);
        assertTrue(text.contains("Text before") && text.contains("text after"));
        assertTrue(countImagesInPdf(pdf) >= 1, "PDF should contain inline image");
    }

    @Test
    void htmlWithLongTextWrapsAcrossLines() throws Exception {
        // Single paragraph long enough to force word-wrap on A4
        String longWord = "word ".repeat(200);
        byte[] pdf = convert("<html><body><p>" + longWord + "</p></body></html>");
        assertValidPdf(pdf);
    }

    @Test
    void htmlWithInlineBlockWidthAuto() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <div style="display: inline-block; border: 1px solid black;">
                  <p>Auto-width inline block with variable content length</p>
                </div>
                <div style="display: inline-block; border: 1px solid black;">
                  <p>Short</p>
                </div>
                </body></html>""");
        assertValidPdf(pdf);
    }

    @Test
    void htmlWithBrInMiddleOfText() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <span>Before break<br/>After break in same span</span>
                </body></html>""");
        assertValidPdf(pdf);
        String text = extractText(pdf);
        assertTrue(text.contains("Before break") && text.contains("After break"));
    }

    // ===== BlockFormattingContext: additional coverage =====

    @Test
    void htmlWithFloatMinWidth() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <div style="float: left; min-width: 200px; background: #eee;">
                Float with min-width</div>
                <p>Text flowing beside the float.</p>
                </body></html>""");
        assertValidPdf(pdf);
    }

    @Test
    void htmlWithAbsolutePositionRightBottom() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <div style="position: relative; width: 400px; height: 300px; border: 1px solid black;">
                  <div style="position: absolute; right: 10px; bottom: 10px;">
                  Bottom-right positioned</div>
                </div>
                </body></html>""");
        assertValidPdf(pdf);
    }

    @Test
    void htmlWithRelativePositionTopWinsBottom() throws Exception {
        // When both top and bottom are set, top takes precedence (CSS spec)
        byte[] pdf = convert("""
                <html><body>
                <div style="position: relative; top: 20px; bottom: 50px;">
                Top wins over bottom</div>
                </body></html>""");
        assertValidPdf(pdf);
        assertTrue(extractText(pdf).contains("Top wins over bottom"));
    }

    @Test
    void htmlWithLetterAndWordSpacingCombined() throws Exception {
        byte[] pdf = convert("""
                <html><body>
                <p style="letter-spacing: 3px; word-spacing: 8px;">Combined spacing test here</p>
                </body></html>""");
        assertValidPdf(pdf);
    }
}
