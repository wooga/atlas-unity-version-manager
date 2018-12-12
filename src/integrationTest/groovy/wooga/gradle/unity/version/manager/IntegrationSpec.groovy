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

package wooga.gradle.unity.version.manager

import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.contrib.java.lang.system.ProvideSystemProperty

import static groovy.json.StringEscapeUtils.escapeJava

class IntegrationSpec extends nebula.test.IntegrationSpec {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables()

    @Rule
    ProvideSystemProperty properties = new ProvideSystemProperty("ignoreDeprecations", "true")

    def setup() {
        def gradleVersion = System.getenv("GRADLE_VERSION")
        if (gradleVersion) {
            this.gradleVersion = gradleVersion
            fork = true
        }

        environmentVariables.clear(
                UnityVersionManagerConsts.UNITY_VERSION_ENV_VAR,
                UnityVersionManagerConsts.UNITY_INSTALL_BASE_DIR_PATH_ENV_VAR,
                UnityVersionManagerConsts.AUTO_INSTALL_UNITY_EDITOR_PATH_ENV_VAR,
                UnityVersionManagerConsts.AUTO_SWITCH_UNITY_EDITOR_PATH_ENV_VAR
        )
    }

    def escapedPath(String path) {
        String osName = System.getProperty("os.name").toLowerCase()
        if (osName.contains("windows")) {
            return escapeJava(path)
        }
        path
    }
}
