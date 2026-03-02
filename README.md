# Ballerina PDF Module

[![Build](https://github.com/ballerina-platform/module-ballerina-pdf/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-pdf/actions/workflows/build-timestamped-master.yml)
[![codecov](https://codecov.io/gh/ballerina-platform/module-ballerina-pdf/branch/main/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerina-pdf)
[![Trivy](https://github.com/ballerina-platform/module-ballerina-pdf/actions/workflows/trivy-scan.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-pdf/actions/workflows/trivy-scan.yml)
[![GraalVM Check](https://github.com/ballerina-platform/module-ballerina-pdf/actions/workflows/build-with-bal-test-graalvm.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-pdf/actions/workflows/build-with-bal-test-graalvm.yml)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerina-pdf.svg)](https://github.com/ballerina-platform/module-ballerina-pdf/commits/main)
[![GitHub Issues](https://img.shields.io/github/issues/ballerina-platform/ballerina-library/module/pdf.svg?label=Open%20Issues)](https://github.com/ballerina-platform/ballerina-library/labels/module%2Fpdf)

The `ballerina/pdf` module provides functionality to convert HTML content to PDF documents and read data from existing PDFs. It processes HTML strings — including full documents, fragments, and messy real-world markup — and produces PDF byte arrays suitable for writing to files or sending over the network.

All processing is done locally with no external service dependencies.

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

## Known limitations

The HTML/CSS renderer supports CSS 2.1 core layout (block, inline, float, table, absolute/relative positioning) but has gaps compared to browser rendering. Key limitations:

- **Layout:** No flexbox, CSS Grid, or multi-column layout. No `position: fixed` or `position: sticky`.
- **Tables:** No `rowspan`, no `<caption>`, no `table-layout: fixed` algorithm.
- **Text:** No `text-align: justify`, no hyphenation, no `text-indent`, no `text-overflow: ellipsis`.
- **CSS features:** No `::before`/`::after` pseudo-elements, no CSS counters, no `calc()`, no custom properties (`var()`), no `@import`, no `@media` queries (print/all media types are supported).
- **Visual:** No CSS gradients, no `text-shadow`, no CSS transforms, no SVG rendering. Only `solid` border style is supported.
- **Fonts:** No `@font-face` (use the `customFonts` option instead). No OpenType features. Bundled fonts: Liberation Sans and Liberation Serif (metrically compatible with Arial and Times New Roman).
- **Page control:** No `page-break-inside: avoid`, no orphans/widows control, no `@page` margin boxes.

## Issues and projects

Issues and Projects tabs are disabled for this repository as this is part of the Ballerina library. To report bugs, request new features, start new discussions, view project boards, etc., visit the Ballerina library [parent repository](https://github.com/ballerina-platform/ballerina-library).

This repository only contains the source code for the package.

## Build from the source

### Setting up the prerequisites

1. Download and install Java SE Development Kit (JDK) version 21. You can download it from either of the following sources:

    * [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
    * [OpenJDK](https://adoptium.net/)

   > **Note:** After installation, remember to set the `JAVA_HOME` environment variable to the directory where JDK was installed.

2. Download and install [Ballerina Swan Lake](https://ballerina.io/).

3. Download and install [Docker](https://www.docker.com/get-started).

   > **Note**: Ensure that the Docker daemon is running before executing any tests.

4. Export Github Personal access token with read package permissions as follows,

    ```bash
    export packageUser=<Username>
    export packagePAT=<Personal access token>
    ```

### Build options

Execute the commands below to build from the source.

1. To build the package:

   ```bash
   ./gradlew clean build
   ```

2. To run the tests:

   ```bash
   ./gradlew clean test
   ```

3. To build the without the tests:

   ```bash
   ./gradlew clean build -x test
   ```

4. To run tests against different environments:

   ```bash
   ./gradlew clean test -Pgroups=<Comma separated groups/test cases>
   ```

5. To debug the package with a remote debugger:

   ```bash
   ./gradlew clean build -Pdebug=<port>
   ```

6. To debug with the Ballerina language:

   ```bash
   ./gradlew clean build -PbalJavaDebug=<port>
   ```

7. Publish the generated artifacts to the local Ballerina Central repository:

    ```bash
    ./gradlew clean build -PpublishToLocalCentral=true
    ```

8. Publish the generated artifacts to the Ballerina Central repository:

   ```bash
   ./gradlew clean build -PpublishToCentral=true
   ```

## Contribute to Ballerina

As an open-source project, Ballerina welcomes contributions from the community.

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All the contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links

* For more information go to the [`pdf` package](https://central.ballerina.io/ballerina/pdf/latest).
* For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/learn/by-example/).
* Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
