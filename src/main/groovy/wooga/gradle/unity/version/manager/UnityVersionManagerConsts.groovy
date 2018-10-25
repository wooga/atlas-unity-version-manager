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

class UnityVersionManagerConsts {

    /**
     * Gradle property name to set the default value for {@code autoSwitchUnityEditor}.
     * @value "uvm.autoSwitchUnityEditor"
     * @see UnityVersionManagerExtension#getAutoSwitchUnityEditor()
     */
    static String AUTO_SWITCH_UNITY_EDITOR_OPTION = "uvm.autoSwitchUnityEditor"

    /**
     * Environment variable name to set the default value for {@code autoSwitchUnityEditor}.
     * @value "UVM_AUTO_SWITCH_UNITY_EDITOR"
     * @see UnityVersionManagerExtension#getAutoSwitchUnityEditor()
     */
    static String AUTO_SWITCH_UNITY_EDITOR_PATH_ENV_VAR = "UVM_AUTO_SWITCH_UNITY_EDITOR"
}
