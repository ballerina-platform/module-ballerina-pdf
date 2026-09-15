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

import ballerina/io;
import ballerina/test;

// Joins all page text into a single string for content verification.
function joinPages(string[] pages) returns string {
    return string:'join(" ", ...pages);
}

// --- Basic rendering tests ---

@test:Config {}
function testRendersMinimalHtml() returns error? {
    byte[] pdf = check parseHtml("<html><body><p>Hello World</p></body></html>");
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Hello World"), "PDF should contain 'Hello World'");
}

@test:Config {}
function testRendersTableContent() returns error? {
    byte[] pdf = check parseHtml(
        "<html><body><table><tr><td>Cell 1</td><td>Cell 2</td></tr></table></body></html>"
    );
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Cell 1"), "PDF should contain 'Cell 1'");
    test:assertTrue(text.includes("Cell 2"), "PDF should contain 'Cell 2'");
}

@test:Config {}
function testRendersBase64Image() returns error? {
    string html = string `<html><body>
        <img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAIAAAACUFjqAAAAEklEQVR4nGP4z8CAB+GTG8HSALfKY52fTcuYAAAAAElFTkSuQmCC" />
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    assertValidPdf(pdf, "Base64 image");
    string[] pages = check extractText(pdf);
    test:assertEquals(pages.length(), 1, "PDF with small image should be 1 page");
    // Verify the image actually rendered by converting to page image
    string[] pageImages = check toImages(pdf);
    test:assertTrue(pageImages.length() > 0, "Should produce page images");

    // Compare against a blank page — a page with a rendered image produces a larger PNG
    byte[] blankPdf = check parseHtml("<html><body></body></html>");
    string[] blankImages = check toImages(blankPdf);
    test:assertTrue(pageImages[0].length() > blankImages[0].length(),
        "Page with image should produce larger rendered output than blank page");
}

@test:Config {}
function testRendersMultiplePages() returns error? {
    string html = "<html><body>";
    foreach int i in 0 ..< 200 {
        html += "<p>Line " + i.toString() + " of the document to force page breaks.</p>";
    }
    html += "</body></html>";

    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    test:assertTrue(pages.length() > 1, "200 paragraphs should span multiple pages, got: " + pages.length().toString());
    string text = joinPages(pages);
    test:assertTrue(text.includes("Line 0"), "First line should be rendered");
    test:assertTrue(text.includes("Line 199"), "Last line should be rendered");
}

// --- Text styling tests ---

@test:Config {}
function testRendersTextDecoration() returns error? {
    string html = "<html><body>"
        + "<p><u>underlined text</u></p>"
        + "<p><s>struck through</s></p>"
        + "</body></html>";
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("underlined text"), "PDF should contain underlined text");
    test:assertTrue(text.includes("struck through"), "PDF should contain struck-through text");
}

@test:Config {}
function testRendersSubscript() returns error? {
    string html = "<html><body><p>H<sub>2</sub>O is water</p></body></html>";
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("2"), "PDF should contain subscript '2'");
    test:assertTrue(text.includes("water"), "PDF should contain 'water'");
}

@test:Config {}
function testRendersTextTransform() returns error? {
    string html = string `<html><head><style>
        .upper { text-transform: uppercase; }
        .lower { text-transform: lowercase; }
        .cap { text-transform: capitalize; }
        </style></head><body>
        <p class="upper">hello world</p>
        <p class="lower">HELLO WORLD</p>
        <p class="cap">hello world</p>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("HELLO WORLD"), "uppercase transform should produce 'HELLO WORLD'");
    test:assertTrue(text.includes("hello world"), "lowercase transform should produce 'hello world'");
    test:assertTrue(text.includes("Hello World"), "capitalize transform should produce 'Hello World'");
}

// --- Table tests ---

@test:Config {}
function testRendersTableVerticalAlign() returns error? {
    string html = string `<html><body>
        <table><tr style="height: 100px;">
        <td style="vertical-align: top;">Top</td>
        <td style="vertical-align: middle;">Middle</td>
        <td style="vertical-align: bottom;">Bottom</td>
        </tr></table></body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Top"), "Should contain 'Top'");
    test:assertTrue(text.includes("Middle"), "Should contain 'Middle'");
    test:assertTrue(text.includes("Bottom"), "Should contain 'Bottom'");
}

@test:Config {}
function testRendersTableWithValignAttr() returns error? {
    string html = string `<html><body>
        <table><tr style="height: 80px;">
        <td valign="top">Top aligned</td>
        <td valign="middle">Middle aligned</td>
        </tr></table></body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Top aligned"), "Should render valign=top text");
    test:assertTrue(text.includes("Middle aligned"), "Should render valign=middle text");
}

@test:Config {}
function testRendersNestedTable() returns error? {
    string html = "<html><body>"
        + "<table><tr><td>"
        + "<table><tr><td>Inner Cell 1</td><td>Inner Cell 2</td></tr></table>"
        + "</td><td>Outer Cell</td></tr></table>"
        + "</body></html>";
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Inner Cell 1"), "Should contain 'Inner Cell 1'");
    test:assertTrue(text.includes("Inner Cell 2"), "Should contain 'Inner Cell 2'");
    test:assertTrue(text.includes("Outer Cell"), "Should contain 'Outer Cell'");
}

// --- Inline-block tests ---

@test:Config {}
function testRendersInlineBlock() returns error? {
    string html = string `<html><body>
        <p>Before <span style="display: inline-block; border: 1px solid black; padding: 5px;">inline block content</span> after</p>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Before"), "Should contain 'Before'");
    test:assertTrue(text.includes("inline block content"), "Should contain inline-block content");
    test:assertTrue(text.includes("after"), "Should contain 'after'");
}

@test:Config {}
function testRendersMultipleInlineBlocks() returns error? {
    string html = string `<html><head><style>
        .box { display: inline-block; padding: 10px; border: 1px solid #999; }
        </style></head><body>
        <div><span class="box">Box A</span> <span class="box">Box B</span> <span class="box">Box C</span></div>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Box A"), "Should contain 'Box A'");
    test:assertTrue(text.includes("Box B"), "Should contain 'Box B'");
    test:assertTrue(text.includes("Box C"), "Should contain 'Box C'");
}

// --- CSS selector tests ---

@test:Config {}
function testRendersChildCombinatorCss() returns error? {
    string html = string `<html><head><style>
        div > p { font-size: 20px; }
        </style></head>
        <body><div><p>Direct child</p><span><p>Nested child</p></span></div></body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Direct child"), "PDF should contain styled direct child text");
    test:assertTrue(text.includes("Nested child"), "PDF should contain nested text");
}

@test:Config {}
function testRendersCssSpecificity() returns error? {
    string html = string `<html><head><style>
        p { color: green; }
        .highlight { color: blue; }
        #unique { color: red; }
        </style></head><body>
        <p id="unique" class="highlight">Specificity test</p>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Specificity test"), "Should render text regardless of specificity resolution");
}

// --- Custom font test ---

@test:Config {}
function testRendersWithCustomFont() returns error? {
    byte[] fontBytes = check io:fileReadBytes("tests/resources/LiberationSans-Regular.ttf");
    Font[] fonts = [{family: "TestFont", content: fontBytes}];
    string html = string `<html><head><style>body { font-family: 'TestFont'; }</style></head>
        <body><p>Custom font text</p></body></html>`;
    byte[] pdf = check parseHtml(html, customFonts = fonts);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Custom font text"), "PDF should contain text rendered with custom font");
}

// --- Positioning tests ---

@test:Config {}
function testRendersPositionRelative() returns error? {
    string html = "<html><body>"
        + "<p>First paragraph</p>"
        + "<p style=\"position: relative; top: 20px; left: 10px;\">Offset paragraph</p>"
        + "<p>Third paragraph</p>"
        + "</body></html>";
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("First paragraph"), "Should contain 'First paragraph'");
    test:assertTrue(text.includes("Offset paragraph"), "Should contain 'Offset paragraph'");
    test:assertTrue(text.includes("Third paragraph"), "Should contain 'Third paragraph'");
}

@test:Config {}
function testRendersPositionAbsolute() returns error? {
    string html = string `<html><body>
        <div style="position: relative; height: 100px;">
        <p>Container text</p>
        <div style="position: absolute; top: 10px; right: 10px;">Badge</div>
        </div></body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Container text"), "Should contain 'Container text'");
    test:assertTrue(text.includes("Badge"), "Should contain absolute-positioned 'Badge'");
}

@test:Config {}
function testAbsolutePositionDoesNotAffectFlow() returns error? {
    string html = string `<html><body>
        <div style="position: relative;">
        <p>Before absolute</p>
        <div style="position: absolute; top: 0; left: 0;">Floating overlay</div>
        <p>After absolute</p>
        </div></body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Before absolute"), "Should contain 'Before absolute'");
    test:assertTrue(text.includes("Floating overlay"), "Should contain 'Floating overlay'");
    test:assertTrue(text.includes("After absolute"), "Should contain 'After absolute'");
}

// --- List tests ---

@test:Config {}
function testRendersOrderedList() returns error? {
    string html = "<html><body>"
        + "<ol><li>First item</li><li>Second item</li><li>Third item</li></ol>"
        + "</body></html>";
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("1."), "Should contain '1.' numbering");
    test:assertTrue(text.includes("First item"), "Should contain 'First item'");
    test:assertTrue(text.includes("2."), "Should contain '2.' numbering");
    test:assertTrue(text.includes("Second item"), "Should contain 'Second item'");
    test:assertTrue(text.includes("3."), "Should contain '3.' numbering");
    test:assertTrue(text.includes("Third item"), "Should contain 'Third item'");
}

@test:Config {}
function testRendersUnorderedList() returns error? {
    string html = "<html><body>"
        + "<ul><li>Bullet A</li><li>Bullet B</li></ul>"
        + "</body></html>";
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Bullet A"), "Should contain 'Bullet A'");
    test:assertTrue(text.includes("Bullet B"), "Should contain 'Bullet B'");
}

// --- Text alignment and box model tests ---

@test:Config {}
function testRendersCenterAlignedText() returns error? {
    string html = string `<html><head><style>
        .center { text-align: center; }
        </style></head><body>
        <p class="center">Centered paragraph</p>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Centered paragraph"), "Should contain centered text");
}

@test:Config {}
function testRendersMinWidth() returns error? {
    string html = string `<html><head><style>
        .minbox { min-width: 200px; background-color: #eee; }
        </style></head><body>
        <div class="minbox">Min width box content</div>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Min width box content"), "Should render min-width constrained element");
}

@test:Config {}
function testRendersFontShorthand() returns error? {
    string html = string `<html><head><style>
        #styled { font: bold 14px 'Liberation Sans'; }
        </style></head><body>
        <p id="styled">Bold fourteen pixel text</p>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Bold fourteen pixel text"), "Should render font shorthand styled text");
}

// --- Background image test ---

@test:Config {}
function testRendersBackgroundImage() returns error? {
    string html = string `<html><head><style>
        #bgimg { background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==); width: 300px; height: 100px; }
        </style></head><body>
        <div id="bgimg">Over image</div>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    assertValidPdf(pdf, "Background image");
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Over image"), "Should render text over background image");
    // Verify the background image rendered by comparing against same layout without image
    string[] pageImages = check toImages(pdf);
    byte[] noImgPdf = check parseHtml(string `<html><head><style>
        #bgimg { width: 300px; height: 100px; }
        </style></head><body>
        <div id="bgimg">Over image</div>
        </body></html>`);
    string[] noImgImages = check toImages(noImgPdf);
    test:assertTrue(pageImages[0].length() > noImgImages[0].length(),
        "Page with background image should produce larger rendered output than without");
}

// --- Empty body test ---

@test:Config {}
function testHandlesEmptyBody() returns error? {
    byte[] pdf = check parseHtml("<html><body></body></html>");
    assertValidPdf(pdf, "Empty body");
    string[] pages = check extractText(pdf);
    test:assertEquals(pages.length(), 1, "Empty body should produce exactly 1 page");
}

// --- Smoke test with file ---

@test:Config {}
function testSmokeTestBasicHtml() returns error? {
    string html = check io:fileReadString("tests/resources/smoke-test.html");
    byte[] pdf = check parseHtml(html);
    assertValidPdf(pdf, "Smoke test");
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("SMOKE TEST"), "Should contain header text");
    test:assertTrue(text.includes("John Doe"), "Should contain table data 'John Doe'");
    test:assertTrue(text.includes("Credit Card"), "Should contain 'Credit Card'");
    test:assertTrue(text.includes("750"), "Should contain score '750'");
}

// --- Float layout tests ---

@test:Config {}
function testRendersFloatLeftWithText() returns error? {
    string html = string `<html><body>
        <div style="float: left; width: 100px; height: 50px; background-color: #ccc;">Sidebar</div>
        <p>Main content that flows beside the floated element</p>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Sidebar"), "Should contain float content 'Sidebar'");
    test:assertTrue(text.includes("Main content"), "Should contain flow text 'Main content'");
}

@test:Config {}
function testRendersFloatLeftAndRight() returns error? {
    string html = string `<html><body>
        <div style="float: left; width: 100px;">Left float</div>
        <div style="float: right; width: 100px;">Right float</div>
        <p>Content between floats</p>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Left float"), "Should contain 'Left float'");
    test:assertTrue(text.includes("Right float"), "Should contain 'Right float'");
    test:assertTrue(text.includes("Content between"), "Should contain middle text");
}

@test:Config {}
function testRendersClearBoth() returns error? {
    string html = string `<html><body>
        <div style="float: left; width: 150px; height: 80px;">Float A</div>
        <div style="float: right; width: 150px; height: 100px;">Float B</div>
        <div style="clear: both;">Cleared footer content</div>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Float A"), "Should contain 'Float A'");
    test:assertTrue(text.includes("Float B"), "Should contain 'Float B'");
    test:assertTrue(text.includes("Cleared footer"), "Should contain cleared content");
}

// --- CSS visual feature tests ---

@test:Config {}
function testRendersWithMediaQueries() returns error? {
    string html = string `<html><head><style>
        @media screen { .screen-only { color: green; } }
        @media print { p { font-weight: bold; } }
        body { font-family: 'Liberation Sans'; }
        p { color: red; }
        </style></head><body>
        <p>Content with media queries</p>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    assertValidPdf(pdf, "Media queries");
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Content with media queries"), "Should render text from HTML with @media blocks");
}

@test:Config {}
function testRendersWithBorderRadius() returns error? {
    string html = string `<html><head><style>
        .card { border-radius: 10px; background-color: #eef; border: 1px solid #999; padding: 10px; }
        </style></head><body>
        <div class="card">Rounded card content</div>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Rounded card content"), "Should render text inside border-radius element");
}

@test:Config {}
function testRendersWithBoxShadow() returns error? {
    string html = string `<html><head><style>
        .shadow { box-shadow: 2px 2px 5px rgba(0,0,0,0.3); padding: 10px; }
        </style></head><body>
        <div class="shadow">Shadow box content</div>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Shadow box content"), "Should render text inside box-shadow element");
}

@test:Config {}
function testRendersWithOpacity() returns error? {
    string html = string `<html><head><style>
        .faded { opacity: 0.5; background-color: rgba(255,0,0,0.3); padding: 10px; }
        </style></head><body>
        <div class="faded">Semi-transparent content</div>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Semi-transparent content"), "Should render text with opacity");
}

@test:Config {}
function testRendersWithLetterSpacing() returns error? {
    string html = string `<html><head><style>
        .spaced { letter-spacing: 2px; word-spacing: 5px; }
        </style></head><body>
        <p class="spaced">Spaced out text here</p>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("Spaced"), "Should render text with letter/word spacing");
}

@test:Config {}
function testRendersWithMarginCollapsing() returns error? {
    string html = string `<html><head><style>
        p { margin-bottom: 20px; margin-top: 10px; }
        </style></head><body>
        <p>First paragraph</p>
        <p>Second paragraph</p>
        <p>Third paragraph</p>
        </body></html>`;
    byte[] pdf = check parseHtml(html);
    string[] pages = check extractText(pdf);
    string text = joinPages(pages);
    test:assertTrue(text.includes("First paragraph"), "Should contain first paragraph");
    test:assertTrue(text.includes("Second paragraph"), "Should contain second paragraph");
    test:assertTrue(text.includes("Third paragraph"), "Should contain third paragraph");
}
