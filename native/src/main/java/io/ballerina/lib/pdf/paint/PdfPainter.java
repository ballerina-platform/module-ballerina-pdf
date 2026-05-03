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

package io.ballerina.lib.pdf.paint;

import io.ballerina.lib.pdf.box.Box;
import io.ballerina.lib.pdf.box.ReplacedBox;
import io.ballerina.lib.pdf.box.TextRun;
import io.ballerina.lib.pdf.css.ComputedStyle;
import io.ballerina.lib.pdf.layout.LayoutContext;
import io.ballerina.lib.pdf.layout.PageBreaker;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.util.Matrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Traverses the laid-out box tree and emits PDFBox drawing commands.
 * Handles coordinate transformation: layout uses top-down Y, PDFBox uses bottom-up Y.
 */
public class PdfPainter {

    private static final float RGB_MAX = 255f;
    private static final float SUPERSCRIPT_OFFSET_RATIO = 0.4f;
    private static final float SUBSCRIPT_OFFSET_RATIO = 0.2f;
    private static final float UNDERLINE_THICKNESS_DIVISOR = 20f;
    private static final float MIN_UNDERLINE_THICKNESS = 0.5f;
    private static final float UNDERLINE_POSITION_RATIO = 0.3f;
    private static final float STRIKETHROUGH_POSITION_RATIO = 0.35f;
    // Kappa constant for approximating a quarter circle with a cubic Bezier curve
    private static final float BEZIER_CIRCLE_KAPPA = 0.552284749831f;

    private record PendingInternalLink(int sourcePageIndex, PDRectangle rect, String targetId) { }

    private final PdfPageManager pageManager;
    private final ImageDecoder imageDecoder;
    private final FontManager fontManager;
    private final LayoutContext layoutContext;
    private final List<PendingInternalLink> pendingInternalLinks = new ArrayList<>();
    private Map<String, float[]> anchorMap = Collections.emptyMap();

    /** Creates a PDF painter with the given dependencies. */
    public PdfPainter(PdfPageManager pageManager, ImageDecoder imageDecoder,
                      FontManager fontManager, LayoutContext layoutContext) {
        this.pageManager = pageManager;
        this.imageDecoder = imageDecoder;
        this.fontManager = fontManager;
        this.layoutContext = layoutContext;
    }

    /** Sets the anchor map for resolving internal links (href="#id"). */
    public void setAnchorMap(Map<String, float[]> anchorMap) {
        this.anchorMap = anchorMap;
    }

    /**
     * Paints the entire box tree across pages (no scaling).
     */
    public void paint(Box root, List<PageBreaker.PageSlice> pages) throws IOException {
        paint(root, pages, 1.0f);
    }

    /**
     * Paints the entire box tree across pages, applying CTM scaling if scale < 1.
     */
    public void paint(Box root, List<PageBreaker.PageSlice> pages, float scale) throws IOException {
        for (int pageIdx = 0; pageIdx < pages.size(); pageIdx++) {
            PageBreaker.PageSlice slice = pages.get(pageIdx);
            PDPageContentStream stream = pageManager.newPage();

            // Apply CTM scale transform to fit content into fewer pages
            if (scale < 1.0f) {
                // Scale around horizontal center to preserve centering, and top edge vertically
                float cx = layoutContext.getMarginLeft() + layoutContext.getContentWidth() / 2f;
                float oy = layoutContext.getPageHeight() - layoutContext.getMarginTop();
                Matrix m = new Matrix(scale, 0, 0, scale, cx * (1 - scale), oy * (1 - scale));
                stream.transform(m);
            }

            // Paint all boxes, offsetting by the page slice's startY
            paintBox(root, stream, 0, 0, slice.startY(), slice.endY());
        }
        pageManager.finish();
    }

    /**
     * Recursively paints a box and its children.
     *
     * @param box      the box to paint
     * @param stream   the current page's content stream
     * @param parentX  absolute X of parent's content area (in layout coords)
     * @param parentY  absolute Y of parent's content area (in layout coords)
     * @param clipTop  top of visible region (page slice start)
     * @param clipBottom bottom of visible region (page slice end)
     */
    private void paintBox(Box box, PDPageContentStream stream,
                          float parentX, float parentY,
                          float clipTop, float clipBottom) throws IOException {

        // Calculate absolute position in layout coordinates
        float absX = parentX + box.getX() + box.getMarginLeft();
        float absY = parentY + box.getY() + box.getMarginTop();
        float boxBottom = absY + box.getBorderBoxHeight();

        // Skip if entirely outside visible region
        if (boxBottom < clipTop || absY > clipBottom) {
            return;
        }

        // Convert to PDF coordinates (add page margins)
        float pdfX = absX + layoutContext.getMarginLeft();
        float borderBoxW = box.getBorderBoxWidth();
        float borderBoxH = box.getBorderBoxHeight();

        // Paint box-shadow (rendered beneath background)
        paintBoxShadow(box, stream, pdfX, absY - clipTop, borderBoxW, borderBoxH);

        // Paint background
        paintBackground(box, stream, pdfX, absY - clipTop, borderBoxW, borderBoxH);

        // Paint borders
        paintBorders(box, stream, pdfX, absY - clipTop, borderBoxW, borderBoxH);

        // Paint content
        float contentX = absX + box.getBorderLeftWidth() + box.getPaddingLeft();
        float contentY = absY + box.getBorderTopWidth() + box.getPaddingTop();

        if (box instanceof TextRun textRun) {
            paintText(textRun, stream, pdfX + box.getBorderLeftWidth() + box.getPaddingLeft(),
                    contentY - clipTop);
        } else if (box instanceof ReplacedBox replaced) {
            paintImage(replaced, stream, pdfX + box.getBorderLeftWidth() + box.getPaddingLeft(),
                    contentY - clipTop);
        }

        // Create link annotation for boxes with href (from <a> tags)
        if (box.getHref() != null && !box.getHref().isEmpty()) {
            createLinkAnnotation(box, pdfX, absY - clipTop, borderBoxW, borderBoxH);
        }

        // Clip children to border-radius boundary per CSS Overflow Module Level 3 §3.1.2:
        // only when overflow is hidden/scroll/auto/clip (not when overflow: visible, the default)
        boolean hasClip = false;
        ComputedStyle clipStyle = box.getStyle();
        if (clipStyle != null) {
            String overflow = clipStyle.get("overflow");
            boolean overflowClips = overflow != null
                    && (overflow.equals("hidden") || overflow.equals("scroll")
                    || overflow.equals("auto") || overflow.equals("clip"));
            if (overflowClips) {
                float clipFontSize = clipStyle.getFontSize(layoutContext.getFallbackFontSize());
                float tlr = clipStyle.getBorderTopLeftRadius(borderBoxW, clipFontSize);
                float trr = clipStyle.getBorderTopRightRadius(borderBoxW, clipFontSize);
                float brr = clipStyle.getBorderBottomRightRadius(borderBoxW, clipFontSize);
                float blr = clipStyle.getBorderBottomLeftRadius(borderBoxW, clipFontSize);
                hasClip = true;
                boolean hasRadius = tlr > 0 || trr > 0 || brr > 0 || blr > 0;
                float clipPdfY = toPdfY(absY - clipTop, borderBoxH);
                stream.saveGraphicsState();
                if (hasRadius) {
                    addRoundedRectPath(stream, pdfX, clipPdfY, borderBoxW, borderBoxH,
                            tlr, trr, brr, blr);
                } else {
                    stream.addRect(pdfX, clipPdfY, borderBoxW, borderBoxH);
                }
                stream.clip();
            }
        }

        // Recurse into children (use effectiveChildren to pick up inline layout results)
        for (Box child : box.getEffectiveChildren()) {
            paintBox(child, stream, contentX, contentY, clipTop, clipBottom);
        }

        if (hasClip) {
            stream.restoreGraphicsState();
        }
    }

    private void paintBackground(Box box, PDPageContentStream stream,
                                  float pdfX, float layoutY, float width, float height) throws IOException {
        ComputedStyle style = box.getStyle();
        if (style == null) {
            return;
        }

        float fontSize = style.getFontSize(layoutContext.getFallbackFontSize());
        float tlr = style.getBorderTopLeftRadius(width, fontSize);
        float trr = style.getBorderTopRightRadius(width, fontSize);
        float brr = style.getBorderBottomRightRadius(width, fontSize);
        float blr = style.getBorderBottomLeftRadius(width, fontSize);
        boolean hasRadius = tlr > 0 || trr > 0 || brr > 0 || blr > 0;

        String bgColor = style.getBackgroundColor();
        RgbColor color = ColorParser.parse(bgColor);
        if (color != null && color.alpha() > 0) {
            float alpha = (color.alpha() / RGB_MAX) * style.getOpacity();
            float pdfY = toPdfY(layoutY, height);
            if (alpha < 1.0f) {
                stream.saveGraphicsState();
                applyAlpha(stream, alpha, false);
            }
            stream.setNonStrokingColor(
                    color.red() / RGB_MAX, color.green() / RGB_MAX,
                    color.blue() / RGB_MAX);
            if (hasRadius) {
                addRoundedRectPath(stream, pdfX, pdfY, width, height, tlr, trr, brr, blr);
            } else {
                stream.addRect(pdfX, pdfY, width, height);
            }
            stream.fill();
            if (alpha < 1.0f) {
                stream.restoreGraphicsState();
            }
        }

        // Background image (base64 data URL via CSS)
        String bgImage = style.getBackgroundImage();
        if (bgImage != null && bgImage.contains("data:image")) {
            PDImageXObject image = imageDecoder.decode(bgImage);
            if (image != null) {
                float pdfY = toPdfY(layoutY, height);
                stream.drawImage(image, pdfX, pdfY, width, height);
            }
        }
    }

    /**
     * Paints a box-shadow approximation using concentric filled shapes with decreasing opacity.
     * True Gaussian blur is not available in PDF; this uses layered rects as an approximation.
     */
    private void paintBoxShadow(Box box, PDPageContentStream stream,
                                 float pdfX, float layoutY, float width, float height) throws IOException {
        ComputedStyle style = box.getStyle();
        if (style == null) {
            return;
        }

        float fontSize = style.getFontSize(layoutContext.getFallbackFontSize());
        java.util.List<ComputedStyle.BoxShadow> shadows = style.getBoxShadows(width, fontSize);
        if (shadows.isEmpty()) {
            return;
        }

        float pdfY = toPdfY(layoutY, height);

        // Resolve border-radius for shadow shape
        float tlr = style.getBorderTopLeftRadius(width, fontSize);
        float trr = style.getBorderTopRightRadius(width, fontSize);
        float brr = style.getBorderBottomRightRadius(width, fontSize);
        float blr = style.getBorderBottomLeftRadius(width, fontSize);
        boolean hasRadius = tlr > 0 || trr > 0 || brr > 0 || blr > 0;

        // CSS spec: first shadow in the list is painted on top, so paint in reverse order
        for (int si = shadows.size() - 1; si >= 0; si--) {
            ComputedStyle.BoxShadow shadow = shadows.get(si);
            RgbColor shadowColor = ColorParser.parse(shadow.color());
            if (shadowColor == null) {
                shadowColor = new RgbColor(0, 0, 0, 128);
            }

            float baseAlpha = (shadowColor.alpha() / RGB_MAX) * style.getOpacity();

            if (shadow.blur() <= 0) {
                // No blur — single crisp layer at full opacity
                float expand = shadow.spread();
                float sx = pdfX + shadow.offsetX() - expand;
                float sy = pdfY - shadow.offsetY() - expand;
                float sw = width + expand * 2;
                float sh = height + expand * 2;

                stream.saveGraphicsState();
                applyAlpha(stream, baseAlpha, false);
                stream.setNonStrokingColor(shadowColor.red() / RGB_MAX,
                        shadowColor.green() / RGB_MAX, shadowColor.blue() / RGB_MAX);

                if (hasRadius) {
                    addRoundedRectPath(stream, sx, sy, sw, sh,
                            tlr + expand, trr + expand, brr + expand, blr + expand);
                } else {
                    stream.addRect(sx, sy, sw, sh);
                }
                stream.fill();
                stream.restoreGraphicsState();
            } else {
                // Approximate blur with concentric layers
                int layers = 4;
                float blurStep = shadow.blur() / layers;

                for (int i = layers; i >= 1; i--) {
                    float expand = shadow.spread() + (blurStep * i);
                    float layerAlpha = baseAlpha * ((float) (layers - i + 1) / (layers + 1));

                    float sx = pdfX + shadow.offsetX() - expand;
                    float sy = pdfY - shadow.offsetY() - expand;
                    float sw = width + expand * 2;
                    float sh = height + expand * 2;

                    stream.saveGraphicsState();
                    applyAlpha(stream, layerAlpha, false);
                    stream.setNonStrokingColor(shadowColor.red() / RGB_MAX,
                            shadowColor.green() / RGB_MAX, shadowColor.blue() / RGB_MAX);

                    if (hasRadius) {
                        addRoundedRectPath(stream, sx, sy, sw, sh,
                                tlr + expand, trr + expand, brr + expand, blr + expand);
                    } else {
                        stream.addRect(sx, sy, sw, sh);
                    }
                    stream.fill();
                    stream.restoreGraphicsState();
                }
            }
        }
    }

    private void paintBorders(Box box, PDPageContentStream stream,
                               float pdfX, float layoutY, float width, float height) throws IOException {
        ComputedStyle style = box.getStyle();
        if (style == null) {
            return;
        }

        float pdfY = toPdfY(layoutY, height);
        float opacity = style.getOpacity();

        // Check for border-radius — if present, stroke the rounded rect as a whole
        float fontSize = style.getFontSize(layoutContext.getFallbackFontSize());
        float tlr = style.getBorderTopLeftRadius(width, fontSize);
        float trr = style.getBorderTopRightRadius(width, fontSize);
        float brr = style.getBorderBottomRightRadius(width, fontSize);
        float blr = style.getBorderBottomLeftRadius(width, fontSize);
        boolean hasRadius = tlr > 0 || trr > 0 || brr > 0 || blr > 0;

        if (hasRadius) {
            // Use average border width for the stroked rounded rect path
            float avgBorderWidth = (box.getBorderTopWidth() + box.getBorderRightWidth()
                    + box.getBorderBottomWidth() + box.getBorderLeftWidth()) / 4f;
            if (avgBorderWidth <= 0) {
                return;
            }

            // Determine border color (use top border color as primary)
            boolean hasBorder = !isNoneBorderStyle(style.getBorderTopStyle())
                    || !isNoneBorderStyle(style.getBorderRightStyle())
                    || !isNoneBorderStyle(style.getBorderBottomStyle())
                    || !isNoneBorderStyle(style.getBorderLeftStyle());
            if (!hasBorder) {
                return;
            }

            RgbColor color = ColorParser.parse(style.getBorderTopColor());
            if (color == null) {
                color = ColorParser.parse(style.getBorderRightColor());
            }
            if (color == null) {
                color = ColorParser.parse(style.getBorderBottomColor());
            }
            if (color == null) {
                color = ColorParser.parse(style.getBorderLeftColor());
            }
            if (color == null) {
                color = new RgbColor(0, 0, 0);
            }

            float alpha = (color.alpha() / RGB_MAX) * opacity;
            if (alpha < 1.0f) {
                stream.saveGraphicsState();
                applyAlpha(stream, alpha, true);
            }
            stream.setStrokingColor(
                    color.red() / RGB_MAX, color.green() / RGB_MAX, color.blue() / RGB_MAX);
            stream.setLineWidth(avgBorderWidth);
            addRoundedRectPath(stream, pdfX, pdfY, width, height, tlr, trr, brr, blr);
            stream.stroke();
            if (alpha < 1.0f) {
                stream.restoreGraphicsState();
            }
            return;
        }

        // Standard per-side border drawing (no radius)

        // Top border — inset by half width so stroke stays within border box
        if (box.getBorderTopWidth() > 0 && !isNoneBorderStyle(style.getBorderTopStyle())) {
            RgbColor color = ColorParser.parse(style.getBorderTopColor());
            if (color == null) {
                color = new RgbColor(0, 0, 0);
            }
            float alpha = (color.alpha() / RGB_MAX) * opacity;
            if (alpha < 1.0f) {
                stream.saveGraphicsState();
                applyAlpha(stream, alpha, true);
            }
            float topY = pdfY + height - (box.getBorderTopWidth() / 2f);
            drawLine(stream, pdfX, topY, pdfX + width, topY,
                    box.getBorderTopWidth(), color);
            if (alpha < 1.0f) {
                stream.restoreGraphicsState();
            }
        }

        // Bottom border — inset by half width
        if (box.getBorderBottomWidth() > 0 && !isNoneBorderStyle(style.getBorderBottomStyle())) {
            RgbColor color = ColorParser.parse(style.getBorderBottomColor());
            if (color == null) {
                color = new RgbColor(0, 0, 0);
            }
            float alpha = (color.alpha() / RGB_MAX) * opacity;
            if (alpha < 1.0f) {
                stream.saveGraphicsState();
                applyAlpha(stream, alpha, true);
            }
            float bottomY = pdfY + (box.getBorderBottomWidth() / 2f);
            drawLine(stream, pdfX, bottomY, pdfX + width, bottomY,
                    box.getBorderBottomWidth(), color);
            if (alpha < 1.0f) {
                stream.restoreGraphicsState();
            }
        }

        // Left border — inset by half width
        if (box.getBorderLeftWidth() > 0 && !isNoneBorderStyle(style.getBorderLeftStyle())) {
            RgbColor color = ColorParser.parse(style.getBorderLeftColor());
            if (color == null) {
                color = new RgbColor(0, 0, 0);
            }
            float alpha = (color.alpha() / RGB_MAX) * opacity;
            if (alpha < 1.0f) {
                stream.saveGraphicsState();
                applyAlpha(stream, alpha, true);
            }
            float leftX = pdfX + (box.getBorderLeftWidth() / 2f);
            drawLine(stream, leftX, pdfY, leftX, pdfY + height,
                    box.getBorderLeftWidth(), color);
            if (alpha < 1.0f) {
                stream.restoreGraphicsState();
            }
        }

        // Right border — inset by half width
        if (box.getBorderRightWidth() > 0 && !isNoneBorderStyle(style.getBorderRightStyle())) {
            RgbColor color = ColorParser.parse(style.getBorderRightColor());
            if (color == null) {
                color = new RgbColor(0, 0, 0);
            }
            float alpha = (color.alpha() / RGB_MAX) * opacity;
            if (alpha < 1.0f) {
                stream.saveGraphicsState();
                applyAlpha(stream, alpha, true);
            }
            float rightX = pdfX + width - (box.getBorderRightWidth() / 2f);
            drawLine(stream, rightX, pdfY, rightX, pdfY + height,
                    box.getBorderRightWidth(), color);
            if (alpha < 1.0f) {
                stream.restoreGraphicsState();
            }
        }
    }

    /**
     * A contiguous run of text that can be rendered with a single font.
     *
     * @param text the text content
     * @param font the PDF font to render with
     */
    private record TextSegment(String text, PDFont font) { }

    private void paintText(TextRun textRun, PDPageContentStream stream,
                            float pdfX, float layoutY) throws IOException {
        String text = textRun.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        PDFont font = textRun.getFont();
        float fontSize = textRun.getFontSize();
        if (font == null) {
            font = fontManager.getDefaultFont();
        }
        if (fontSize <= 0) {
            fontSize = layoutContext.getFallbackFontSize();
        }

        // Resolve text into segments, each using a font that can encode its characters
        List<TextSegment> segments = resolveTextSegments(text, font);
        if (segments.isEmpty()) {
            return;
        }

        // Text color
        ComputedStyle style = textRun.getStyle();
        RgbColor textColor = new RgbColor(0, 0, 0);
        if (style != null && style.getColor() != null) {
            RgbColor parsed = ColorParser.parse(style.getColor());
            if (parsed != null) {
                textColor = parsed;
            }
        }

        // Calculate baseline position
        float ascent = fontManager.getAscent(font, fontSize);
        float baselineY = layoutY + ascent;

        // Superscript: shift baseline up
        if (textRun.isSuperscript()) {
            baselineY -= fontSize * SUPERSCRIPT_OFFSET_RATIO;
        }
        // Subscript: shift baseline down
        if (textRun.isSubscript()) {
            baselineY += fontSize * SUBSCRIPT_OFFSET_RATIO;
        }

        float pdfY = toPdfYBaseline(baselineY);

        // Resolve letter-spacing and word-spacing
        float letterSpacing = 0;
        float wordSpacing = 0;
        if (style != null) {
            letterSpacing = style.getLetterSpacing(fontSize);
            wordSpacing = style.getWordSpacing(fontSize);
        }

        // Apply opacity
        float textAlpha = (textColor.alpha() / RGB_MAX) * (style != null ? style.getOpacity() : 1.0f);
        if (textAlpha < 1.0f) {
            stream.saveGraphicsState();
            applyAlpha(stream, textAlpha, false);
        }

        // Paint each text segment, advancing X cursor between font switches
        float cursorX = pdfX;
        for (TextSegment segment : segments) {
            stream.beginText();
            stream.setFont(segment.font(), fontSize);
            stream.setNonStrokingColor(
                    textColor.red() / RGB_MAX, textColor.green() / RGB_MAX,
                    textColor.blue() / RGB_MAX);
            if (letterSpacing != 0) {
                stream.setCharacterSpacing(letterSpacing);
            }
            if (wordSpacing != 0) {
                stream.setWordSpacing(wordSpacing);
            }
            stream.newLineAtOffset(cursorX, pdfY);
            stream.showText(segment.text());
            if (letterSpacing != 0) {
                stream.setCharacterSpacing(0);
            }
            if (wordSpacing != 0) {
                stream.setWordSpacing(0);
            }
            stream.endText();
            float segmentWidth = fontManager.measureText(segment.text(), segment.font(), fontSize);
            if (letterSpacing != 0) {
                segmentWidth += letterSpacing * segment.text().length();
            }
            if (wordSpacing != 0) {
                for (int i = 0; i < segment.text().length(); i++) {
                    if (segment.text().charAt(i) == ' ') {
                        segmentWidth += wordSpacing;
                    }
                }
            }
            cursorX += segmentWidth;
        }

        if (textAlpha < 1.0f) {
            stream.restoreGraphicsState();
        }

        // Text decoration (underline, line-through)
        if (style != null) {
            String decoration = style.get("text-decoration");
            if (decoration != null && !"none".equals(decoration)) {
                float totalWidth = cursorX - pdfX;
                float thickness = Math.max(fontSize / UNDERLINE_THICKNESS_DIVISOR, MIN_UNDERLINE_THICKNESS);
                float descent = fontManager.getDescent(font, fontSize);

                if (decoration.contains("underline")) {
                    float lineY = toPdfYBaseline(baselineY + descent * UNDERLINE_POSITION_RATIO);
                    drawLine(stream, pdfX, lineY, pdfX + totalWidth, lineY, thickness, textColor);
                }
                if (decoration.contains("line-through")) {
                    float lineY = toPdfYBaseline(baselineY - ascent * STRIKETHROUGH_POSITION_RATIO);
                    drawLine(stream, pdfX, lineY, pdfX + totalWidth, lineY, thickness, textColor);
                }
            }
        }
    }

    private void paintImage(ReplacedBox replaced, PDPageContentStream stream,
                             float pdfX, float layoutY) throws IOException {
        String src = replaced.getSrc();
        if (src == null) {
            return;
        }

        PDImageXObject image = imageDecoder.decode(src);
        if (image == null) {
            return;
        }

        float width = replaced.getIntrinsicWidth();
        float height = replaced.getIntrinsicHeight();
        float pdfY = toPdfY(layoutY, height);

        stream.drawImage(image, pdfX, pdfY, width, height);
    }

    /**
     * Creates a PDF link annotation for a box with an href.
     * External URLs use PDActionURI; internal anchors (#id) use PDActionGoTo
     * with a PDPageXYZDestination resolved from the anchor map.
     */
    private void createLinkAnnotation(Box box, float pdfX, float layoutY,
                                       float width, float height) throws IOException {
        String href = box.getHref();
        if ("#".equals(href)) {
            return;
        }

        if (href.startsWith("#")) {
            // Internal anchor — defer until all pages exist so we can use PDPage references
            String targetId = href.substring(1);
            float[] target = anchorMap.get(targetId);
            if (target == null) {
                return;
            }
            int sourcePageIdx = pageManager.getCurrentPageIndex();
            float pdfY = toPdfY(layoutY, height);
            pendingInternalLinks.add(new PendingInternalLink(
                    sourcePageIdx, new PDRectangle(pdfX, pdfY, width, height), targetId));
            return;
        }

        // External URL → PDActionURI
        PDPage page = pageManager.getCurrentPage();
        if (page == null) {
            return;
        }

        float pdfY = toPdfY(layoutY, height);

        PDAnnotationLink link = new PDAnnotationLink();
        link.setRectangle(new PDRectangle(pdfX, pdfY, width, height));

        PDActionURI action = new PDActionURI();
        action.setURI(href);
        link.setAction(action);

        // Hide default blue border around link annotation
        PDBorderStyleDictionary borderStyle = new PDBorderStyleDictionary();
        borderStyle.setWidth(0);
        link.setBorderStyle(borderStyle);

        page.getAnnotations().add(link);
    }

    /**
     * Resolves deferred internal anchor links after all pages have been created.
     * PDFBox 3.x requires PDActionGoTo destinations to reference actual PDPage objects,
     * not page numbers. Since forward-referencing pages (e.g., a link on page 1 targeting
     * page 3) isn't possible during painting, links are collected and resolved here.
     */
    public void resolveInternalLinks(PdfPageManager pageManager) throws IOException {
        for (PendingInternalLink link : pendingInternalLinks) {
            float[] target = anchorMap.get(link.targetId());
            if (target == null) {
                continue;
            }

            PDPage targetPage = pageManager.getPage((int) target[0]);
            PDPage sourcePage = pageManager.getPage(link.sourcePageIndex());
            if (targetPage == null || sourcePage == null) {
                continue;
            }

            PDPageXYZDestination dest = new PDPageXYZDestination();
            dest.setPage(targetPage);
            dest.setTop((int) target[1]);

            PDActionGoTo goTo = new PDActionGoTo();
            goTo.setDestination(dest);

            PDAnnotationLink annotation = new PDAnnotationLink();
            annotation.setRectangle(link.rect());
            annotation.setAction(goTo);

            PDBorderStyleDictionary borderStyle = new PDBorderStyleDictionary();
            borderStyle.setWidth(0);
            annotation.setBorderStyle(borderStyle);

            sourcePage.getAnnotations().add(annotation);
        }
    }

    /**
     * Converts a layout Y coordinate (top-down) to PDFBox Y (bottom-up).
     * For rectangles: pdfY is the bottom-left corner.
     */
    private float toPdfY(float layoutY, float height) {
        return layoutContext.getPageHeight() - layoutContext.getMarginTop() - layoutY - height;
    }

    /**
     * Converts a layout baseline Y coordinate (top-down) to PDFBox Y (bottom-up).
     */
    private float toPdfYBaseline(float layoutBaselineY) {
        return layoutContext.getPageHeight() - layoutContext.getMarginTop() - layoutBaselineY;
    }

    private void drawLine(PDPageContentStream stream, float x1, float y1, float x2, float y2,
                           float lineWidth, RgbColor color) throws IOException {
        stream.setStrokingColor(
                color.red() / RGB_MAX, color.green() / RGB_MAX,
                color.blue() / RGB_MAX);
        stream.setLineWidth(lineWidth);
        stream.moveTo(x1, y1);
        stream.lineTo(x2, y2);
        stream.stroke();
    }

    private boolean isNoneBorderStyle(String style) {
        return style == null || "none".equals(style) || "hidden".equals(style);
    }

    /**
     * Adds a rounded rectangle path using Bezier curves.
     * Kappa constant approximates a quarter circle with a cubic Bezier.
     * PDFBox Y is bottom-up: (x, y) is the bottom-left corner.
     */
    private void addRoundedRectPath(PDPageContentStream stream, float x, float y,
                                     float w, float h, float tlr, float trr,
                                     float brr, float blr) throws IOException {
        float k = BEZIER_CIRCLE_KAPPA;

        // Clamp radii to half the box dimension
        float maxR = Math.min(w, h) / 2f;
        tlr = Math.min(tlr, maxR);
        trr = Math.min(trr, maxR);
        brr = Math.min(brr, maxR);
        blr = Math.min(blr, maxR);

        // Start at bottom-left, just after the BL radius (moving clockwise in PDF coords)
        stream.moveTo(x + blr, y);

        // Bottom edge → bottom-right corner
        stream.lineTo(x + w - brr, y);
        if (brr > 0) {
            stream.curveTo(x + w - brr + brr * k, y,
                           x + w, y + brr - brr * k,
                           x + w, y + brr);
        }

        // Right edge → top-right corner
        stream.lineTo(x + w, y + h - trr);
        if (trr > 0) {
            stream.curveTo(x + w, y + h - trr + trr * k,
                           x + w - trr + trr * k, y + h,
                           x + w - trr, y + h);
        }

        // Top edge → top-left corner
        stream.lineTo(x + tlr, y + h);
        if (tlr > 0) {
            stream.curveTo(x + tlr - tlr * k, y + h,
                           x, y + h - tlr + tlr * k,
                           x, y + h - tlr);
        }

        // Left edge → bottom-left corner
        stream.lineTo(x, y + blr);
        if (blr > 0) {
            stream.curveTo(x, y + blr - blr * k,
                           x + blr - blr * k, y,
                           x + blr, y);
        }

        stream.closePath();
    }

    private void applyAlpha(PDPageContentStream stream, float alpha, boolean stroking) throws IOException {
        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        if (stroking) {
            gs.setStrokingAlphaConstant(alpha);
        } else {
            gs.setNonStrokingAlphaConstant(alpha);
        }
        stream.setGraphicsStateParameters(gs);
    }

    /**
     * Splits text into segments where each segment uses a font that can encode all its characters.
     * Tries the primary font first, then falls back to symbol/other fonts for missing glyphs.
     * Characters that no font can encode are replaced with spaces (or dropped for control chars).
     */
    private List<TextSegment> resolveTextSegments(String text, PDFont primaryFont) {
        List<TextSegment> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        PDFont currentFont = primaryFont;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (FontManager.canEncode(primaryFont, c)) {
                // Primary font can handle this character
                if (currentFont != primaryFont && current.length() > 0) {
                    segments.add(new TextSegment(current.toString(), currentFont));
                    current = new StringBuilder();
                }
                currentFont = primaryFont;
                current.append(c);
            } else {
                // Primary font can't encode — try fallback fonts
                PDFont fallback = fontManager.findFallbackFont(c);
                if (fallback != null) {
                    if (currentFont != fallback && current.length() > 0) {
                        segments.add(new TextSegment(current.toString(), currentFont));
                        current = new StringBuilder();
                    }
                    currentFont = fallback;
                    current.append(c);
                } else {
                    // No font can encode this character — replace with space for readability
                    if (!Character.isISOControl(c)) {
                        if (currentFont != primaryFont && current.length() > 0) {
                            segments.add(new TextSegment(current.toString(), currentFont));
                            current = new StringBuilder();
                        }
                        currentFont = primaryFont;
                        current.append(' ');
                    }
                }
            }
        }
        if (current.length() > 0) {
            segments.add(new TextSegment(current.toString(), currentFont));
        }
        return segments;
    }
}
