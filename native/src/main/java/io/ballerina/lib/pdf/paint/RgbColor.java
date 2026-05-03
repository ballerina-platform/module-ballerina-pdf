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

/**
 * Lightweight RGBA color representation that avoids java.awt.Color's AWT native library dependency.
 * All channel values are 0–255.
 *
 * @param red   red channel (0–255)
 * @param green green channel (0–255)
 * @param blue  blue channel (0–255)
 * @param alpha alpha channel (0–255, where 255 is fully opaque)
 */
public record RgbColor(int red, int green, int blue, int alpha) {

    public RgbColor(int red, int green, int blue) {
        this(red, green, blue, 255);
    }
}
