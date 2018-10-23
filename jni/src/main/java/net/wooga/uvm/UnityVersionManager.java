/*
 * Copyright 2018 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.wooga.uvm;

import cz.adamh.utils.NativeUtils;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * This is a simple native interface for the {@code unity version manager} tool.
 * It loads the needed dynamic library from the resource directory at startup time.
 */
public class UnityVersionManager {

    private final static Logger logger = Logger.getLogger(UnityVersionManager.class.getName());

    static {
        try {
            NativeUtils.loadLibraryFromJar("/native/" + System.mapLibraryName("uvm_jni"));
        } catch (IOException e) {
            logger.warning("unable to load native library: " + System.mapLibraryName("uvm_jni"));
        }
    }

    /**
     * Returns the uvm core library version.
     */
    public static native String uvmVersion();
}
