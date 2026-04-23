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

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;

import java.util.List;

/**
 * Configuration options for HTML-to-PDF conversion.
 */
public class ConversionOptions {

    /**
     * A custom font entry with explicit family, weight, and style.
     *
     * @param family  the CSS font-family name to register this font under
     * @param content the raw TTF file bytes
     * @param bold    whether this variant is bold
     * @param italic  whether this variant is italic
     */
    public record FontEntry(String family, byte[] content, boolean bold, boolean italic) {
    }

    /** CSS-spec default: medium = 16px = 12pt. */
    public static final float DEFAULT_FALLBACK_FONT_SIZE = 12f;

    // A4 dimensions in points (210mm x 297mm)
    public static final float A4_WIDTH = 595.276f;
    public static final float A4_HEIGHT = 841.89f;

    // US Letter dimensions in points (8.5" x 11")
    public static final float LETTER_WIDTH = 612f;
    public static final float LETTER_HEIGHT = 792f;

    // US Legal dimensions in points (8.5" x 14")
    public static final float LEGAL_WIDTH = 612f;
    public static final float LEGAL_HEIGHT = 1008f;

    // Default margin: 0pt (no page margin; CSS controls spacing)
    public static final float DEFAULT_MARGIN = 0f;

    // BMap field keys — must match Ballerina record field names in types.bal
    public static final BString KEY_FALLBACK_FONT_SIZE = StringUtils.fromString("fallbackFontSize");
    public static final BString KEY_PAGE_SIZE = StringUtils.fromString("pageSize");
    public static final BString KEY_ADDITIONAL_CSS = StringUtils.fromString("additionalCss");
    public static final BString KEY_MAX_PAGES = StringUtils.fromString("maxPages");
    public static final BString KEY_CUSTOM_FONTS = StringUtils.fromString("customFonts");
    public static final BString KEY_MARGINS = StringUtils.fromString("margins");

    // Font record field keys
    public static final BString KEY_FONT_FAMILY = StringUtils.fromString("family");
    public static final BString KEY_FONT_CONTENT = StringUtils.fromString("content");
    public static final BString KEY_FONT_BOLD = StringUtils.fromString("bold");
    public static final BString KEY_FONT_ITALIC = StringUtils.fromString("italic");

    // PageMargins record field keys
    public static final BString KEY_MARGIN_TOP = StringUtils.fromString("top");
    public static final BString KEY_MARGIN_RIGHT = StringUtils.fromString("right");
    public static final BString KEY_MARGIN_BOTTOM = StringUtils.fromString("bottom");
    public static final BString KEY_MARGIN_LEFT = StringUtils.fromString("left");

    // CustomPageSize record field keys
    public static final BString KEY_PAGE_WIDTH = StringUtils.fromString("width");
    public static final BString KEY_PAGE_HEIGHT = StringUtils.fromString("height");

    private final float fallbackFontSize;
    private final float pageWidth;
    private final float pageHeight;
    private final float marginTop;
    private final float marginRight;
    private final float marginBottom;
    private final float marginLeft;
    private final String additionalCss;
    private final List<FontEntry> customFonts;
    private final Integer maxPages;
    private final boolean customPageSizeSet;
    private final boolean customMarginsSet;

    /** Full constructor with all options. */
    public ConversionOptions(float fallbackFontSize,
                            float pageWidth, float pageHeight,
                            float marginTop, float marginRight,
                            float marginBottom, float marginLeft,
                            String additionalCss,
                            List<FontEntry> customFonts, Integer maxPages,
                            boolean customPageSizeSet, boolean customMarginsSet) {
        if (fallbackFontSize <= 0) {
            throw new IllegalArgumentException("fallbackFontSize must be positive, got: " + fallbackFontSize);
        }
        if (pageWidth <= 0) {
            throw new IllegalArgumentException("pageWidth must be positive, got: " + pageWidth);
        }
        if (pageHeight <= 0) {
            throw new IllegalArgumentException("pageHeight must be positive, got: " + pageHeight);
        }
        if (marginTop < 0) {
            throw new IllegalArgumentException("marginTop must be non-negative, got: " + marginTop);
        }
        if (marginRight < 0) {
            throw new IllegalArgumentException("marginRight must be non-negative, got: " + marginRight);
        }
        if (marginBottom < 0) {
            throw new IllegalArgumentException("marginBottom must be non-negative, got: " + marginBottom);
        }
        if (marginLeft < 0) {
            throw new IllegalArgumentException("marginLeft must be non-negative, got: " + marginLeft);
        }
        if (maxPages != null && maxPages <= 0) {
            throw new IllegalArgumentException("maxPages must be greater than 0, got: " + maxPages);
        }
        this.fallbackFontSize = fallbackFontSize;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.marginTop = marginTop;
        this.marginRight = marginRight;
        this.marginBottom = marginBottom;
        this.marginLeft = marginLeft;
        this.additionalCss = additionalCss;
        this.customFonts = customFonts;
        this.maxPages = maxPages;
        this.customPageSizeSet = customPageSizeSet;
        this.customMarginsSet = customMarginsSet;
    }

    /** Resolves page dimensions from a page size name (A4, LETTER, LEGAL). */
    public static float[] pageDimensions(String pageSize) {
        return switch (pageSize.toUpperCase()) {
            case "LETTER" -> new float[]{LETTER_WIDTH, LETTER_HEIGHT};
            case "LEGAL" -> new float[]{LEGAL_WIDTH, LEGAL_HEIGHT};
            default -> new float[]{A4_WIDTH, A4_HEIGHT};
        };
    }

    /** Returns the fallback font size in points. */
    public float getFallbackFontSize() {
        return fallbackFontSize;
    }

    /** Returns the page width in points. */
    public float getPageWidth() {
        return pageWidth;
    }

    /** Returns the page height in points. */
    public float getPageHeight() {
        return pageHeight;
    }

    /** Returns the top margin in points. */
    public float getMarginTop() {
        return marginTop;
    }

    /** Returns the right margin in points. */
    public float getMarginRight() {
        return marginRight;
    }

    /** Returns the bottom margin in points. */
    public float getMarginBottom() {
        return marginBottom;
    }

    /** Returns the left margin in points. */
    public float getMarginLeft() {
        return marginLeft;
    }

    /** Returns the additional CSS to inject. */
    public String getAdditionalCss() {
        return additionalCss;
    }

    /** Returns the custom font entries. */
    public List<FontEntry> getCustomFonts() {
        return customFonts;
    }

    /** Returns the maximum number of pages, or null for no limit. */
    public Integer getMaxPages() {
        return maxPages;
    }

    /** Returns true if the page size was explicitly set by the user via API. */
    public boolean isCustomPageSizeSet() {
        return customPageSizeSet;
    }

    /** Returns true if the margins were explicitly set by the user via API. */
    public boolean isCustomMarginsSet() {
        return customMarginsSet;
    }

}
