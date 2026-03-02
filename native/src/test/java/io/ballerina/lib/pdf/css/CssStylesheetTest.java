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

package io.ballerina.lib.pdf.css;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CssStylesheetTest {

    @Test
    void getPageSizeReturnsDefaultA4() {
        CssStylesheet sheet = new CssStylesheet();
        assertEquals("a4", sheet.getPageSize());
    }

    @Test
    void getPageSizeReturnsCustom() {
        CssStylesheet sheet = new CssStylesheet();
        sheet.addPageDeclaration(new CssDeclaration("size", "letter"));
        assertEquals("letter", sheet.getPageSize());
    }

    @Test
    void getPageMarginReturnsDefault() {
        CssStylesheet sheet = new CssStylesheet();
        assertEquals("15mm 10mm", sheet.getPageMargin());
    }

    @Test
    void getPageMarginReturnsCustom() {
        CssStylesheet sheet = new CssStylesheet();
        sheet.addPageDeclaration(new CssDeclaration("margin", "20mm"));
        assertEquals("20mm", sheet.getPageMargin());
    }

    @Test
    void addRuleAndGetRules() {
        CssStylesheet sheet = new CssStylesheet();
        CssRule rule = new CssRule(
                new CssSelector("p"),
                java.util.List.of(new CssDeclaration("color", "red")),
                0);
        sheet.addRule(rule);
        assertEquals(1, sheet.getRules().size());
    }

    @Test
    void getPageDeclarationsReturnsAll() {
        CssStylesheet sheet = new CssStylesheet();
        sheet.addPageDeclaration(new CssDeclaration("size", "a4"));
        sheet.addPageDeclaration(new CssDeclaration("margin", "10mm"));
        assertEquals(2, sheet.getPageDeclarations().size());
    }

    @Test
    void getPageSizeReturnFirstDeclaration() {
        CssStylesheet sheet = new CssStylesheet();
        sheet.addPageDeclaration(new CssDeclaration("size", "letter"));
        sheet.addPageDeclaration(new CssDeclaration("size", "legal"));
        // First matching declaration wins
        assertEquals("letter", sheet.getPageSize());
    }

    @Test
    void emptyStylesheetHasNoRules() {
        CssStylesheet sheet = new CssStylesheet();
        assertTrue(sheet.getRules().isEmpty());
        assertTrue(sheet.getPageDeclarations().isEmpty());
    }
}
