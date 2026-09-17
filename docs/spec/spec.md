_Owners_: @yashodhanmohandevan \
_Reviewers_: @yashodhanmohandevan \
_Created_: 2025/01/01 \
_Updated_: 2026/08/16 \
_Edition_: Swan Lake

# Specification: Ballerina PDF Library

## Introduction

This is the specification for the `pdf` standard library of the [Ballerina language](https://ballerina.io/), which provides functionality for HTML-to-PDF conversion and PDF reading operations.

The `pdf` library specification has evolved over time. This specification is written to describe the functionality available from version 0.9.0 onwards.

If you have any feedback or suggestions about the library, start a discussion via a [GitHub issue](https://github.com/ballerina-platform/ballerina-library/issues) or in the [Discord server](https://discord.gg/ballerinalang). Based on the outcome of the discussion, the specification and implementation can be updated. Community contributions are also encouraged. If you notice an implementation that deviates from the specification, please raise an issue.

## Contents

1. [Overview](#1-overview)
2. [HTML-to-PDF Conversion](#2-html-to-pdf-conversion)
   - 2.1. [The `parseHtml()` Function](#21-the-parsehtml-function)
   - 2.2. [Conversion Options](#22-conversion-options)
     - 2.2.1. [Fallback Font Size](#221-fallback-font-size)
     - 2.2.2. [Page Size](#222-page-size)
     - 2.2.3. [Page Margins](#223-page-margins)
     - 2.2.4. [Additional CSS](#224-additional-css)
     - 2.2.5. [Custom Fonts](#225-custom-fonts)
     - 2.2.6. [Maximum Pages](#226-maximum-pages)
3. [PDF Reading](#3-pdf-reading)
   - 3.1. [Text Extraction](#31-text-extraction)
   - 3.2. [Image Conversion](#32-image-conversion)
4. [Errors](#4-errors)

## 1. Overview

The `pdf` library provides three capabilities:

- **HTML-to-PDF conversion**: Converting HTML content to a PDF document.
- **Text extraction**: Extracting the text content of an existing PDF document, page by page.
- **Image conversion**: Rendering the pages of an existing PDF document as images.

All processing is performed locally within the Ballerina runtime. The library does not depend on any external service, browser, or system-installed tool, which makes it suitable for environments with data-compliance restrictions on sending content to third parties.

## 2. HTML-to-PDF Conversion

### 2.1. The `parseHtml()` Function

The `pdf:parseHtml()` function converts an HTML string to a PDF document and returns its content as a byte array.

The input can be a complete HTML document or a fragment. Real-world HTML is often malformed — encoding mismatches, duplicate CSS properties, unclosed or self-closing elements — so the input is sanitized into a well-formed document before rendering. A conversion fails only when the input cannot be interpreted as HTML at all, in which case a `pdf:HtmlParseError` is returned.

The rendered output aims to visually match the same HTML rendered in a browser. Styling is taken from the document's own CSS (inline styles, `<style>` blocks, and `@page` rules), and can be extended or overridden through the conversion options described below. Any failure in the layout or rendering stage is returned as a `pdf:RenderError`.

###### Example: Converting HTML to PDF

```ballerina
byte[] pdfContent = check pdf:parseHtml("<h1>Hello World</h1>");
```

### 2.2. Conversion Options

The conversion behavior is controlled through the `pdf:ConversionOptions` record. It is defined as an included record parameter of `pdf:parseHtml()`, so each option can be passed directly as a named argument. Every option has a default and may be omitted; calling `pdf:parseHtml()` with only the HTML string is valid. Providing an invalid option value (such as a non-positive page dimension) causes the conversion to fail with a `pdf:RenderError`.

###### Example: Converting with Options

```ballerina
byte[] pdfContent = check pdf:parseHtml(html,
    pageSize = pdf:LETTER,
    margins = {top: 72, right: 54, bottom: 72, left: 54}
);
```

#### 2.2.1. Fallback Font Size

The `fallbackFontSize` option sets the font size (in points) used for elements whose size is not determined by the document's CSS. It defaults to `12.0`, which corresponds to the CSS `medium` keyword. A `font-size` declared in the document's CSS always takes precedence over this value; the option only fills the gap when the CSS is silent. The value must be positive.

#### 2.2.2. Page Size

The `pageSize` option sets the page dimensions of the output PDF. It accepts either a standard preset — `A4`, `LETTER`, or `LEGAL`, defined by the `pdf:StandardPageSize` enum — or arbitrary dimensions given as a `pdf:CustomPageSize` value in points (1 point = 1/72 inch). The presets correspond to `A4` (595 × 842 pt), `LETTER` (612 × 792 pt), and `LEGAL` (612 × 1008 pt).

The page size is resolved with the following precedence:

1. An explicitly provided `pageSize` option.
2. A `size` declared in the document's CSS `@page` rule.
3. The default, `A4`.

#### 2.2.3. Page Margins

The `margins` option sets the top, right, bottom, and left page margins in points, using the `pdf:PageMargins` record. Margin values must be non-negative. Margins follow the same precedence as the page size: an explicitly provided option overrides the document's CSS `@page` margins, and when neither is present, all margins default to zero so the content spans the full page.

#### 2.2.4. Additional CSS

The `additionalCss` option injects extra CSS into the document before conversion. The injected styles are applied on top of the document's own styles, so they can override them. This allows a consumer to restyle a document — adjust fonts, hide elements, fix layout issues — without modifying the HTML source, which is useful when the HTML comes from an external system.

#### 2.2.5. Custom Fonts

The renderer only uses fonts that are registered with it; it does not fall back to fonts installed on the host system. This keeps the output identical across environments. The library bundles the Liberation Sans and Liberation Serif font families, which are metrically compatible with Arial and Times New Roman respectively, so common documents render correctly with no configuration.

The `customFonts` option registers additional TrueType fonts for a conversion. Each `pdf:Font` entry carries the font-family name, the TTF file content, and flags marking it as a bold and/or italic variant; each variant of a family is registered as a separate entry. The document's CSS then selects a registered font through the standard `font-family` property.

#### 2.2.6. Maximum Pages

The `maxPages` option caps the number of pages in the output PDF. When the laid-out content would exceed the cap, the content is scaled down uniformly so that it fits within exactly that many pages; content is never truncated. The value must be greater than zero. When the option is omitted, the output has as many pages as the content requires.

## 3. PDF Reading

The library provides operations for reading existing PDF documents. Each operation has three variants that differ only in where the PDF is read from:

- from a byte array already in memory,
- from a file path on the local file system (the `file*` variants), or
- from a URL (the `url*` variants).

All reading operations are page-oriented: they return an array with one element per page, in page order. A failure — a corrupted or password-protected document, an unreadable file, or an unreachable URL — is returned as a `pdf:ReadError`.

### 3.1. Text Extraction

The `pdf:extractText()`, `pdf:fileExtractText()`, and `pdf:urlExtractText()` functions extract the text content of a PDF document. Each element of the returned array contains the text of one page.

###### Example: Extracting Text from a PDF File

```ballerina
string[] pages = check pdf:fileExtractText("document.pdf");
```

### 3.2. Image Conversion

The `pdf:toImages()`, `pdf:fileToImages()`, and `pdf:urlToImages()` functions render each page of a PDF document as a PNG image. Each element of the returned array is one page's image, encoded as a Base64 string.

## 4. Errors

All operations of the library return the `pdf:Error` type on failure. It is a distinct error type with three distinct subtypes, so a caller can handle failures broadly or narrow them to a specific stage:

- `pdf:HtmlParseError` — the input HTML could not be parsed or preprocessed into a document.
- `pdf:RenderError` — the rendering pipeline (layout, painting, or PDF generation) failed, or a conversion option value was invalid.
- `pdf:ReadError` — a PDF reading operation failed because the document was corrupted, invalid, or inaccessible.
