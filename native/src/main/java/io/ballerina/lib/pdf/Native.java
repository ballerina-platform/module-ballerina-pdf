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

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for Ballerina Java interop.
 * <p>
 * Static methods in this class are called directly from Ballerina via {@code @java:Method} bindings.
 */
public final class Native {

    private Native() {
    }

    /**
     * Converts HTML to PDF bytes.
     * <p>
     * Called from Ballerina: {@code parseHtml(string html, *ConversionOptions options)}
     *
     * @param html    HTML content as a Ballerina string
     * @param options ConversionOptions record as a BMap
     * @return byte[] (as BArray) on success, BError on failure
     */
    @SuppressWarnings("unchecked")
    public static Object parseHtml(BString html, BMap<BString, Object> options) {
        try {
            // Read options from BMap
            float fontSize = getFloat(options, ConversionOptions.KEY_FALLBACK_FONT_SIZE);
            String additionalCss = getString(options, ConversionOptions.KEY_ADDITIONAL_CSS, null);

            Integer maxPages = getInt(options, ConversionOptions.KEY_MAX_PAGES);

            // Read custom fonts from Font[] array
            List<ConversionOptions.FontEntry> customFonts = null;
            Object customFontsObj = options.get(ConversionOptions.KEY_CUSTOM_FONTS);
            if (customFontsObj != null) {
                BArray fontsArray = (BArray) customFontsObj;
                customFonts = new ArrayList<>();
                for (int i = 0; i < fontsArray.size(); i++) {
                    BMap<BString, Object> fontRecord = (BMap<BString, Object>) fontsArray.get(i);
                    String family = fontRecord.get(ConversionOptions.KEY_FONT_FAMILY).toString();
                    byte[] content = ((BArray) fontRecord.get(
                            ConversionOptions.KEY_FONT_CONTENT)).getBytes();
                    boolean bold = getBool(fontRecord, ConversionOptions.KEY_FONT_BOLD);
                    boolean italic = getBool(fontRecord, ConversionOptions.KEY_FONT_ITALIC);
                    customFonts.add(new ConversionOptions.FontEntry(family, content, bold, italic));
                }
            }

            // Read margins from nested PageMargins record (optional — null means not set by user)
            BMap<BString, Object> margins = (BMap<BString, Object>) options.get(
                    ConversionOptions.KEY_MARGINS);
            float marginTop;
            float marginRight;
            float marginBottom;
            float marginLeft;
            boolean customMarginsSet = margins != null;
            if (customMarginsSet) {
                marginTop = getFloat(margins, ConversionOptions.KEY_MARGIN_TOP);
                marginRight = getFloat(margins, ConversionOptions.KEY_MARGIN_RIGHT);
                marginBottom = getFloat(margins, ConversionOptions.KEY_MARGIN_BOTTOM);
                marginLeft = getFloat(margins, ConversionOptions.KEY_MARGIN_LEFT);
            } else {
                marginTop = marginRight = marginBottom = marginLeft = ConversionOptions.DEFAULT_MARGIN;
            }

            // Resolve page dimensions: null means not set by user; string (preset) or record (custom {width, height})
            float pageWidth;
            float pageHeight;
            // Resolve page dimensions (optional — null means not set by user)
            Object pageSizeObj = options.get(ConversionOptions.KEY_PAGE_SIZE);
            boolean customPageSizeSet = pageSizeObj != null;
            if (customPageSizeSet) {
                if (TypeUtils.getType(pageSizeObj).getTag() == TypeTags.RECORD_TYPE_TAG) {
                    BMap<BString, Object> customSize = (BMap<BString, Object>) pageSizeObj;
                    pageWidth = getFloat(customSize, ConversionOptions.KEY_PAGE_WIDTH);
                    pageHeight = getFloat(customSize, ConversionOptions.KEY_PAGE_HEIGHT);
                } else {
                    String pageSizeName = ((BString) pageSizeObj).getValue();
                    float[] dims = ConversionOptions.pageDimensions(pageSizeName);
                    pageWidth = dims[0];
                    pageHeight = dims[1];
                }
            } else {
                pageWidth = ConversionOptions.A4_WIDTH;
                pageHeight = ConversionOptions.A4_HEIGHT;
            }

            // Build ConversionOptions
            ConversionOptions opts = new ConversionOptions(
                    fontSize, pageWidth, pageHeight,
                    marginTop, marginRight, marginBottom, marginLeft,
                    additionalCss, customFonts, maxPages,
                    customPageSizeSet, customMarginsSet);

            // Phase 1: Parse HTML
            HtmlPreprocessor preprocessor = new HtmlPreprocessor();
            Document doc;
            try {
                doc = preprocessor.preprocess(html.getValue());
            } catch (Exception e) {
                return PdfErrorCreator.htmlParseError(
                        "HTML parsing failed: " + e.getMessage(), e);
            }

            // Phase 2: Convert to PDF
            HtmlToPdfConverter converter = new HtmlToPdfConverter();
            byte[] pdf = converter.convert(doc, opts);

            return ValueCreator.createArrayValue(pdf);
        } catch (Exception e) {
            return PdfErrorCreator.renderError(
                    "PDF rendering failed: " + e.getMessage(), e);
        }
    }

    // --- PDF reading methods ---

    /**
     * Converts each page of a PDF (as byte[]) to Base64-encoded PNG images.
     */
    public static Object toImages(BArray pdf) {
        try (PDDocument doc = PdfProcessor.loadFromBytes(pdf.getBytes())) {
            return ValueCreator.createArrayValue(PdfProcessor.toImages(doc));
        } catch (Exception e) {
            return PdfErrorCreator.readError("PDF image conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts each page of a PDF file to Base64-encoded PNG images.
     */
    public static Object fileToImages(BString filePath) {
        try (PDDocument doc = PdfProcessor.loadFromFile(filePath.getValue())) {
            return ValueCreator.createArrayValue(PdfProcessor.toImages(doc));
        } catch (Exception e) {
            return PdfErrorCreator.readError("PDF image conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts each page of a PDF at the given URL to Base64-encoded PNG images.
     */
    public static Object urlToImages(BString url) {
        try (PDDocument doc = PdfProcessor.loadFromUrl(url.getValue())) {
            return ValueCreator.createArrayValue(PdfProcessor.toImages(doc));
        } catch (Exception e) {
            return PdfErrorCreator.readError("PDF image conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts text content from each page of a PDF (as byte[]).
     */
    public static Object extractText(BArray pdf) {
        try (PDDocument doc = PdfProcessor.loadFromBytes(pdf.getBytes())) {
            return ValueCreator.createArrayValue(PdfProcessor.extractText(doc));
        } catch (Exception e) {
            return PdfErrorCreator.readError("PDF text extraction failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts text content from each page of a PDF file.
     */
    public static Object fileExtractText(BString filePath) {
        try (PDDocument doc = PdfProcessor.loadFromFile(filePath.getValue())) {
            return ValueCreator.createArrayValue(PdfProcessor.extractText(doc));
        } catch (Exception e) {
            return PdfErrorCreator.readError("PDF text extraction failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts text content from each page of a PDF at the given URL.
     */
    public static Object urlExtractText(BString url) {
        try (PDDocument doc = PdfProcessor.loadFromUrl(url.getValue())) {
            return ValueCreator.createArrayValue(PdfProcessor.extractText(doc));
        } catch (Exception e) {
            return PdfErrorCreator.readError("PDF text extraction failed: " + e.getMessage(), e);
        }
    }

    // --- BMap helper methods ---

    private static float getFloat(BMap<BString, Object> map, BString key) {
        return ((Double) map.get(key)).floatValue();
    }

    private static String getString(BMap<BString, Object> map, BString key, String defaultValue) {
        Object value = map.get(key);
        if (value != null) {
            return ((BString) value).getValue();
        }
        return defaultValue;
    }

    private static boolean getBool(BMap<BString, Object> map, BString key) {
        return (Boolean) map.get(key);
    }

    private static Integer getInt(BMap<BString, Object> map, BString key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        long longVal = (Long) value;
        if (longVal > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) longVal;
    }
}
