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

import io.ballerina.runtime.api.values.BString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static io.ballerina.lib.pdf.ConversionOptions.A4_HEIGHT;
import static io.ballerina.lib.pdf.ConversionOptions.A4_WIDTH;
import static io.ballerina.lib.pdf.ConversionOptions.DEFAULT_FALLBACK_FONT_SIZE;
import static io.ballerina.lib.pdf.ConversionOptions.DEFAULT_MARGIN;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfProcessorTest {

    // --- loadFromUrl: scheme validation ---

    @Test
    void loadFromUrlRejectsFtpScheme() {
        IOException ex = assertThrows(IOException.class,
                () -> PdfProcessor.loadFromUrl("ftp://example.com/file.pdf"));
        assertTrue(ex.getMessage().contains("Unsupported URL scheme"));
    }

    @Test
    void loadFromUrlRejectsFileScheme() {
        IOException ex = assertThrows(IOException.class,
                () -> PdfProcessor.loadFromUrl("file:///etc/passwd"));
        assertTrue(ex.getMessage().contains("Unsupported URL scheme"));
    }

    @Test
    void loadFromUrlRejectsInvalidUri() {
        IOException ex = assertThrows(IOException.class,
                () -> PdfProcessor.loadFromUrl("not a valid uri at all"));
        assertTrue(ex.getMessage().contains("Invalid URL"));
    }

    // --- loadFromBytes ---

    @Test
    void loadFromBytesValidPdf() throws Exception {
        // Generate a real PDF via the converter, then load it back
        ConversionOptions opts = new ConversionOptions(
                DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                null, null, null);
        Document doc = new HtmlPreprocessor().preprocess("<html><body><p>Test</p></body></html>");
        byte[] pdfBytes = new HtmlToPdfConverter().convert(doc, opts);

        try (PDDocument loaded = PdfProcessor.loadFromBytes(pdfBytes)) {
            assertNotNull(loaded);
            assertTrue(loaded.getNumberOfPages() > 0);
        }
    }

    @Test
    void loadFromBytesRejectsTooShort() {
        IOException ex = assertThrows(IOException.class,
                () -> PdfProcessor.loadFromBytes(new byte[]{0x01, 0x02}));
        assertTrue(ex.getMessage().contains("empty or too short"));
    }

    @Test
    void loadFromBytesRejectsNull() {
        IOException ex = assertThrows(IOException.class,
                () -> PdfProcessor.loadFromBytes(null));
        assertTrue(ex.getMessage().contains("empty or too short"));
    }

    @Test
    void loadFromBytesRejectsNonPdf() {
        byte[] notPdf = "NOT A PDF FILE".getBytes(StandardCharsets.UTF_8);
        IOException ex = assertThrows(IOException.class,
                () -> PdfProcessor.loadFromBytes(notPdf));
        assertTrue(ex.getMessage().contains("missing %PDF header"));
    }

    // --- toImages ---

    @Test
    void toImagesReturnsBase64PngPerPage() throws Exception {
        ConversionOptions opts = new ConversionOptions(
                DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                null, null, null);
        Document doc = new HtmlPreprocessor().preprocess("<html><body><p>Image test</p></body></html>");
        byte[] pdfBytes = new HtmlToPdfConverter().convert(doc, opts);

        try (PDDocument loaded = PdfProcessor.loadFromBytes(pdfBytes)) {
            BString[] images = PdfProcessor.toImages(loaded);
            assertNotNull(images);
            assertTrue(images.length > 0, "Should produce at least one image");
            // Verify each entry is valid Base64
            for (BString img : images) {
                assertNotNull(img);
                byte[] decoded = java.util.Base64.getDecoder().decode(img.getValue());
                assertTrue(decoded.length > 0, "Decoded image bytes should not be empty");
            }
        }
    }

    @Test
    void toImagesMultiplePages() throws Exception {
        StringBuilder sb = new StringBuilder("<html><body>");
        for (int i = 0; i < 200; i++) {
            sb.append("<p>Line ").append(i).append(" to fill pages.</p>");
        }
        sb.append("</body></html>");

        ConversionOptions opts = new ConversionOptions(
                DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                null, null, null);
        Document doc = new HtmlPreprocessor().preprocess(sb.toString());
        byte[] pdfBytes = new HtmlToPdfConverter().convert(doc, opts);

        try (PDDocument loaded = PdfProcessor.loadFromBytes(pdfBytes)) {
            int pageCount = loaded.getNumberOfPages();
            assertTrue(pageCount > 1, "Should be multi-page");
            BString[] images = PdfProcessor.toImages(loaded);
            assertTrue(images.length == pageCount, "Image count should match page count");
        }
    }

    // --- extractText ---

    @Test
    void extractTextReturnsTextPerPage() throws Exception {
        ConversionOptions opts = new ConversionOptions(
                DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                null, null, null);
        Document doc = new HtmlPreprocessor().preprocess(
                "<html><body><p>Extract this text</p></body></html>");
        byte[] pdfBytes = new HtmlToPdfConverter().convert(doc, opts);

        try (PDDocument loaded = PdfProcessor.loadFromBytes(pdfBytes)) {
            BString[] pages = PdfProcessor.extractText(loaded);
            assertNotNull(pages);
            assertTrue(pages.length > 0, "Should have at least one page");
            assertTrue(pages[0].getValue().contains("Extract this text"));
        }
    }

    @Test
    void extractTextMultiplePages() throws Exception {
        StringBuilder sb = new StringBuilder("<html><body>");
        for (int i = 0; i < 200; i++) {
            sb.append("<p>Line ").append(i).append(" to fill pages.</p>");
        }
        sb.append("</body></html>");

        ConversionOptions opts = new ConversionOptions(
                DEFAULT_FALLBACK_FONT_SIZE, A4_WIDTH, A4_HEIGHT,
                DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                null, null, null);
        Document doc = new HtmlPreprocessor().preprocess(sb.toString());
        byte[] pdfBytes = new HtmlToPdfConverter().convert(doc, opts);

        try (PDDocument loaded = PdfProcessor.loadFromBytes(pdfBytes)) {
            int pageCount = loaded.getNumberOfPages();
            assertTrue(pageCount > 1, "Should be multi-page");
            BString[] pages = PdfProcessor.extractText(loaded);
            assertTrue(pages.length == pageCount, "Text array length should match page count");
        }
    }

    // --- loadFromFile ---

    @Test
    void loadFromFileRejectsNonPdfExtension() {
        IOException ex = assertThrows(IOException.class,
                () -> PdfProcessor.loadFromFile("/tmp/test.txt"));
        assertTrue(ex.getMessage().contains("Unsupported file type"));
    }

    @Test
    void loadFromFileRejectsMissingFile() {
        IOException ex = assertThrows(IOException.class,
                () -> PdfProcessor.loadFromFile("/tmp/nonexistent_file_12345.pdf"));
        assertTrue(ex.getMessage().contains("File not found"));
    }
}
