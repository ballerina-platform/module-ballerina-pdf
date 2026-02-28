## Overview

The `ballerina/pdf` module provides functionality to convert HTML content to PDF documents. It processes HTML strings — including full documents, fragments, and messy real-world markup — and produces PDF byte arrays suitable for writing to files or sending over the network.

All processing is done locally with no external service dependencies. The module handles HTML cleanup, CSS injection, and PDF rendering in a single pipeline.

## Quickstart

### Convert HTML to PDF

```ballerina
import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    byte[] pdfBytes = check pdf:parseHtml("<h1>Hello World</h1><p>Generated with Ballerina.</p>");
    check io:fileWriteBytes("output.pdf", pdfBytes);
}
```

### Convert with custom options

```ballerina
import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    string html = check io:fileReadString("report.html");

    byte[] pdfBytes = check pdf:parseHtml(html,
        fallbackFontSize = 10.0,
        pageSize = pdf:LETTER,
        margins = {top: 72, right: 54, bottom: 72, left: 54},
        additionalCss = "body { font-family: sans-serif; } .container { width: 100% !important; }"
    );

    check io:fileWriteBytes("report.pdf", pdfBytes);
}
```

### Extract text from a PDF

```ballerina
import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    byte[] pdfBytes = check io:fileReadBytes("document.pdf");
    string[] pages = check pdf:extractText(pdfBytes);
    foreach int i in 0 ..< pages.length() {
        io:println("Page ", i + 1, ": ", pages[i]);
    }
}
```

You can also extract text directly from a file path or URL:

```ballerina
// From a local file
string[] pagesFromFile = check pdf:fileExtractText("document.pdf");
// From a URL
string[] pagesFromUrl = check pdf:urlExtractText("https://example.com/document.pdf");
```

### Convert PDF pages to images

```ballerina
import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    byte[] pdfBytes = check io:fileReadBytes("document.pdf");
    string[] base64Images = check pdf:toImages(pdfBytes);
    // Each element is a Base64-encoded PNG string (one per page)
}
```

File and URL variants are also available: `fileToImages()` and `urlToImages()`.

## Examples

The `pdf` module provides practical examples illustrating usage in various scenarios. Explore these [examples](https://github.com/ballerina-platform/module-ballerina-pdf/tree/main/examples/).

1. [HTML to PDF conversion](https://github.com/ballerina-platform/module-ballerina-pdf/tree/main/examples/html-to-pdf/) — Reads an HTML report file and converts it to PDF.
