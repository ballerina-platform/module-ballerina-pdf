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

package io.ballerina.lib.pdf;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Module;

/**
 * Stores the Ballerina module reference, set during module initialization.
 * Used to create module-scoped distinct error types from Java.
 */
public final class ModuleUtils {

    private static Module module;

    private ModuleUtils() {
    }

    /**
     * Called from Ballerina during module init to capture the module reference.
     *
     * @param env Ballerina environment
     */
    public static void setModule(Environment env) {
        // Required by PDFBox's PDF-to-image rendering (PDFRenderer) in headless environments
        System.setProperty("java.awt.headless", "true");
        module = env.getCurrentModule();
    }

    /**
     * Returns the current module reference.
     *
     * @return Module instance
     */
    public static Module getModule() {
        return module;
    }
}
