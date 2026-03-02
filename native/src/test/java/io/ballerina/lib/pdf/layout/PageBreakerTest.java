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

package io.ballerina.lib.pdf.layout;

import io.ballerina.lib.pdf.box.BlockBox;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PageBreaker}.
 * Tests page-breaking logic using programmatically constructed box trees.
 */
class PageBreakerTest {

    private final PageBreaker breaker = new PageBreaker();

    /**
     * Creates a root box containing the given number of child block boxes,
     * each with the specified height, stacked vertically.
     */
    private BlockBox makeRoot(int childCount, float childHeight) {
        BlockBox root = new BlockBox(null);
        float y = 0;
        for (int i = 0; i < childCount; i++) {
            BlockBox child = new BlockBox(null);
            child.setY(y);
            child.setHeight(childHeight);
            root.addChild(child);
            y += childHeight;
        }
        root.setHeight(y);
        return root;
    }

    @Test
    void singlePageContentReturnsSingleSlice() {
        // 3 children × 100pt = 300pt, fits in 800pt page
        BlockBox root = makeRoot(3, 100);
        List<PageBreaker.PageSlice> pages = breaker.computePages(root, 800);
        assertEquals(1, pages.size());
        assertEquals(0f, pages.get(0).startY(), 0.01f);
        assertEquals(300f, pages.get(0).endY(), 0.01f);
    }

    @Test
    void multiPageContentReturnsMultipleSlices() {
        // 10 children × 100pt = 1000pt, page height = 300pt → needs at least 4 pages
        BlockBox root = makeRoot(10, 100);
        List<PageBreaker.PageSlice> pages = breaker.computePages(root, 300);
        assertTrue(pages.size() > 1, "Should produce multiple pages");
        // First page starts at 0
        assertEquals(0f, pages.get(0).startY(), 0.01f);
        // Last page ends at or near total height
        PageBreaker.PageSlice last = pages.get(pages.size() - 1);
        assertEquals(1000f, last.endY(), 0.01f);
    }

    @Test
    void breakPointsPreferBlockBoundaries() {
        // 5 children × 200pt = 1000pt, page height = 350pt
        // Should break at child boundaries (200, 400, 600, 800) rather than at 350
        BlockBox root = makeRoot(5, 200);
        List<PageBreaker.PageSlice> pages = breaker.computePages(root, 350);

        // First page should break at a child boundary (200 or 400), not at 350
        float firstEnd = pages.get(0).endY();
        // The largest breakpoint ≤ 350 that is > 0 should be 200
        assertEquals(200f, firstEnd, 0.01f);
    }

    @Test
    void computeVisualHeightReflectsChildren() {
        BlockBox root = makeRoot(3, 100);
        float height = PageBreaker.computeVisualHeight(root);
        assertEquals(300f, height, 0.01f);
    }

    @Test
    void computeVisualHeightIncludesRootPadding() {
        BlockBox root = makeRoot(2, 100);
        root.setPaddingTop(10);
        root.setPaddingBottom(15);
        float height = PageBreaker.computeVisualHeight(root);
        // paddingTop(10) + children(200) + paddingBottom(15)
        assertEquals(225f, height, 0.01f);
    }

    @Test
    void zeroPageHeightThrowsException() {
        BlockBox root = makeRoot(1, 100);
        assertThrows(IllegalArgumentException.class, () ->
                breaker.computePages(root, 0));
    }

    @Test
    void negativePageHeightThrowsException() {
        BlockBox root = makeRoot(1, 100);
        assertThrows(IllegalArgumentException.class, () ->
                breaker.computePages(root, -10));
    }

    @Test
    void forcedBreakWhenNoBreakPointFits() {
        // Single child taller than page → forced break at page boundary
        BlockBox root = new BlockBox(null);
        BlockBox tallChild = new BlockBox(null);
        tallChild.setHeight(500);
        root.addChild(tallChild);
        root.setHeight(500);

        List<PageBreaker.PageSlice> pages = breaker.computePages(root, 200);
        assertTrue(pages.size() > 1, "Should force-break content taller than a page");
        // First page should break at exactly the page height since no breakpoint fits
        assertEquals(200f, pages.get(0).endY(), 0.01f);
    }

    @Test
    void pageSliceHeightCalculation() {
        PageBreaker.PageSlice slice = new PageBreaker.PageSlice(100, 350);
        assertEquals(250f, slice.height(), 0.01f);
    }

    @Test
    void emptyRootProducesSingleEmptyPage() {
        BlockBox root = new BlockBox(null);
        root.setHeight(0);
        List<PageBreaker.PageSlice> pages = breaker.computePages(root, 800);
        assertEquals(1, pages.size());
        assertEquals(0f, pages.get(0).height(), 0.01f);
    }
}
