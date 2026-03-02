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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageDecoderTest {

    // 1x1 red PNG
    private static final String VALID_DATA_URL =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4"
                    + "nGP4z8BQDwAEgAF/pooBPQAAAABJRU5ErkJggg==";

    private static PDDocument document;
    private static ImageDecoder decoder;

    @BeforeAll
    static void setUp() {
        document = new PDDocument();
        decoder = new ImageDecoder(document);
    }

    @AfterAll
    static void tearDown() throws IOException {
        document.close();
    }

    @Test
    void decodeValidBase64Png() {
        PDImageXObject image = decoder.decode(VALID_DATA_URL);
        assertNotNull(image, "Valid base64 PNG should decode successfully");
    }

    @Test
    void decodeReturnsNullForNonBase64() {
        assertNull(decoder.decode("https://example.com/image.png"));
    }

    @Test
    void decodeReturnsNullForNull() {
        assertNull(decoder.decode(null));
    }

    @Test
    void decodeCachesResult() {
        PDImageXObject first = decoder.decode(VALID_DATA_URL);
        PDImageXObject second = decoder.decode(VALID_DATA_URL);
        assertNotNull(first);
        assertSame(first, second, "Second decode should return cached object");
    }

    @Test
    void decodeReturnsNullForMalformedBase64() {
        assertNull(decoder.decode("data:image/png;base64,!!!invalid!!!"));
    }

    @Test
    void getImageDimensionsReturnsActualForValidImage() {
        float[] dims = decoder.getImageDimensions(VALID_DATA_URL);
        assertNotNull(dims);
        assertTrue(dims[0] > 0 && dims[1] > 0, "Valid image should have positive dimensions");
    }

    @Test
    void getImageDimensionsReturnsDefaultForInvalid() {
        float[] dims = decoder.getImageDimensions("https://example.com/nope.png");
        assertArrayEquals(new float[]{50, 50}, dims, "Invalid src should return default 50x50");
    }

    @Test
    void decodeReturnsNullForEmptyString() {
        assertNull(decoder.decode(""));
    }
}
