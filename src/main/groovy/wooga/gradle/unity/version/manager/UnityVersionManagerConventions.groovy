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

package wooga.gradle.unity.version.manager

import com.wooga.gradle.PropertyLookup

class UnityVersionManagerConventions {

    /**
     * Gradle property/env convention to set the default value for {@code autoSwitchUnityEditor}.
     * @see UnityVersionManagerExtension#getAutoSwitchUnityEditor()
     */
    static final PropertyLookup autoSwitchUnityEditor = new PropertyLookup("UVM_AUTO_SWITCH_UNITY_EDITOR", "uvm.autoSwitchUnityEditor", false)

    /**
     * Gradle property/env convention set the default value for {@code autoInstallUnityEditor}.
     * @see UnityVersionManagerExtension#getAutoInstallUnityEditor()
     */
    static final PropertyLookup autoInstallUnityEditor = new PropertyLookup("UVM_AUTO_INSTALL_UNITY_EDITOR", "uvm.autoInstallUnityEditor", false)

    /**
     * Gradle property/env convention to set the default value for {@code unityInstallBaseDir}.
     * @see UnityVersionManagerExtension#getUnityInstallBaseDir()
     */
    static final PropertyLookup unityInstallBaseDir = new PropertyLookup("UVM_UNITY_INSTALL_BASE_DIR", "uvm.unityInstallBaseDir", null)

    /**
     * Gradle property/env convention to set the default value for {@code unityVersion}.
     * @see UnityVersionManagerExtension#getUnityVersion()
     */
    static final PropertyLookup unityVersion = new PropertyLookup("UVM_UNITY_VERSION", "uvm.unityVersion", null)
}
