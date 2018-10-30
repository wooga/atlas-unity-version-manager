/*
 * Copyright 2017 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class Installation {

    private final static Logger logger = Logger.getLogger(Installation.class.getName());

    static {
        try {
            NativeUtils.loadLibraryFromJar("/native/" + System.mapLibraryName("uvm_jni"));
        } catch (IOException e) {
            logger.warning("unable to load native library: " + System.mapLibraryName("uvm_jni"));
        }
    }

    private File location;
    private String version;

    public Installation(File location, String version) {
        this.location = location;
        this.version = version;
    }

    public File getLocation() {
        return location;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Installation) {
            Installation other = (Installation) obj;
            return other.location.equals(this.location)
                    && other.version.equals(this.version);
        }
        return false;
    }

    public native Component[] getComponents();
}
