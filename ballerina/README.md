## Overview

The `pdf` module provides functionality to convert HTML content to PDF documents and extract data from existing PDFs, with all processing done locally and no external service dependencies.

## Key Features

- Convert HTML strings to PDF documents, including custom fonts, page size, and margins
- Extract text content from existing PDFs
- Convert PDF pages to Base64-encoded images

## Quickstart

To use the `pdf` connector in your Ballerina application, modify the `.bal` file as follows:

### Step 1: Import the module

Import the `pdf` module.

```ballerina
import ballerina/pdf;
```

### Step 2: Invoke module functions

#### Extract text from a PDF

```ballerina
byte[] pdfBytes = check io:fileReadBytes("document.pdf");
string[] pages = check pdf:extractText(pdfBytes);
foreach int i in 0 ..< pages.length() {
   io:println("Page ", i + 1, ": ", pages[i]);
}
```

#### Convert PDF pages to images

```ballerina
byte[] pdfBytes = check io:fileReadBytes("document.pdf");
string[] base64Images = check pdf:toImages(pdfBytes);
// Each element is a Base64-encoded PNG string (one per page)
```

#### Convert html to pdf

Convert an HTML string to a PDF document.

```ballerina
byte[] pdfBytes = check pdf:parseHtml("<h1>Hello World</h1><p>Generated with Ballerina.</p>");
check io:fileWriteBytes("output.pdf", pdfBytes);
```

#### Convert with custom options

```ballerina
string html = check io:fileReadString("report.html");
byte[] pdfBytes = check pdf:parseHtml(html,
   fallbackFontSize = 10.0,
   pageSize = pdf:LETTER,
   margins = {top: 72, right: 54, bottom: 72, left: 54},
   additionalCss = "body { font-family: sans-serif; } .container { width: 100% !important; }"
);
check io:fileWriteBytes("report.pdf", pdfBytes);
```

### Step 3: Run the Ballerina application

```bash
bal run
```

## Examples

The `pdf` module provides practical examples illustrating usage in various scenarios. Explore these [examples](https://github.com/ballerina-platform/module-ballerina-pdf/tree/main/examples/).

1. [HTML to PDF conversion](https://github.com/ballerina-platform/module-ballerina-pdf/tree/main/examples/html-to-pdf/) — Reads an HTML report file and converts it to PDF.

## Known limitations (HTML-to-PDF conversion)

The `parseHtml` function uses a custom HTML/CSS renderer that supports CSS 2.1 core layout (block, inline, float, table, absolute/relative positioning) but has gaps compared to browser rendering. Key limitations:

- **Layout:** No flexbox, CSS Grid, or multi-column layout. No `position: fixed` or `position: sticky`.
- **Tables:** No `rowspan`, no `<caption>`, no `table-layout: fixed` algorithm.
- **Text:** No `text-align: justify`, no hyphenation, no `text-indent`, no `text-overflow: ellipsis`.
- **CSS features:** No `::before`/`::after` pseudo-elements, no CSS counters, no `calc()`, no custom properties (`var()`), no `@import`, no `@media` queries (print/all media types are supported).
- **Visual:** No CSS gradients, no `text-shadow`, no CSS transforms, no SVG rendering. Only `solid` border style is supported.
- **Fonts:** No `@font-face` (use the `customFonts` option instead). No OpenType features. Bundled fonts: Liberation Sans and Liberation Serif (metrically compatible with Arial and Times New Roman).
- **Page control:** No `page-break-inside: avoid`, no orphans/widows control, no `@page` margin boxes.
