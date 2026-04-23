// Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

# Standard page size presets for PDF output.
public enum StandardPageSize {
    A4,
    LETTER,
    LEGAL
}

# Custom page dimensions in points (1 point = 1/72 inch).
#
# + width - Page width in points
# + height - Page height in points
public type CustomPageSize record {|
    float width;
    float height;
|};

# Page size for PDF output. Use a standard preset or specify custom dimensions in points.
public type PageSize StandardPageSize|CustomPageSize;

# Page margins in points (1 point = 1/72 inch). 
# This is the default margin for the PDF output.
#
# + top - Top margin
# + right - Right margin
# + bottom - Bottom margin
# + left - Left margin
public type PageMargins record {|
    float top = 0;
    float right = 0;
    float bottom = 0;
    float left = 0;
|};

# A custom font to register for PDF conversion.
#
# + family - CSS font-family name used to reference this font in HTML/CSS
# + content - TTF font file content as bytes
# + bold - Whether this is a bold variant (default: false)
# + italic - Whether this is an italic variant (default: false)
public type Font record {|
    string family;
    byte[] content;
    boolean bold = false;
    boolean italic = false;
|};

# Options controlling HTML-to-PDF conversion behavior.
#
# + fallbackFontSize - Fallback font size in points (default: 12.0, per CSS spec "medium" keyword).
#                     Used when CSS does not specify a font-size for an element. CSS `font-size`
#                     declarations in the HTML take precedence over this value.
# + pageSize - Page size for the PDF output. Use a standard preset (A4, LETTER, LEGAL)
#              or a CustomPageSize record with custom width/height in points. When omitted,
#              the CSS `@page` size rule is used if present, otherwise defaults to A4.
# + margins - Page margins in points (top, right, bottom, left). When omitted, the CSS
#             `@page` margin rule is used if present, otherwise defaults to 0 (no page margin).
# + additionalCss - Additional CSS to inject into the document before conversion.
#                   Use this for consumer-specific style overrides without modifying the HTML.
# + customFonts - Custom fonts to register for the conversion. Each entry specifies the font family,
#                 TTF content, and weight/style flags. CSS references the font via `font-family`.
# + maxPages - Maximum number of pages in the output PDF. Content is scaled to fit within
#              this page limit. Must be greater than 0 when provided.
public type ConversionOptions record {|
    float fallbackFontSize = 12.0;
    PageSize pageSize?;
    PageMargins margins?;
    string additionalCss?;
    Font[] customFonts?;
    int maxPages?;
|};