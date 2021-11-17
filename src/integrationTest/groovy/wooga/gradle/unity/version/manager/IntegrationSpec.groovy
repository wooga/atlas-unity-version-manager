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

import com.wooga.spock.extensions.uvm.UnityInstallation
import net.wooga.uvm.Installation
import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.contrib.java.lang.system.ProvideSystemProperty
import spock.lang.Shared

import static groovy.json.StringEscapeUtils.escapeJava

class IntegrationSpec extends nebula.test.IntegrationSpec {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables()

    @Rule
    ProvideSystemProperty properties = new ProvideSystemProperty("ignoreDeprecations", "true")

    @Shared
    @UnityInstallation(version = "2019.4.32f1", basePath = "build/unity", cleanup = false)
    Installation preInstalledUnity2019_4_32f1

    @Shared
    @UnityInstallation(version = "2019.4.31f1", basePath = "build/unity", cleanup = false)
    Installation preInstalledUnity2019_4_31f1

    def setup() {
        def gradleVersion = System.getenv("GRADLE_VERSION")
        if (gradleVersion) {
            this.gradleVersion = gradleVersion
            fork = true
        }

        (UnityVersionManagerConventions.unityVersion.environmentKeys +
                UnityVersionManagerConventions.unityInstallBaseDir.environmentKeys +
                UnityVersionManagerConventions.autoInstallUnityEditor.environmentKeys +
                UnityVersionManagerConventions.autoSwitchUnityEditor.environmentKeys).each {
            environmentVariables.clear(it)
        }
    }

    static String escapedPath(String path) {
        String osName = System.getProperty("os.name").toLowerCase()
        if (osName.contains("windows")) {
            return escapeJava(path)
        }
        path
    }

    static String OS = System.getProperty("os.name").toLowerCase()

    static boolean isWindows() {
        return (OS.indexOf("win") >= 0)
    }

    static boolean isMac() {
        return (OS.indexOf("mac") >= 0)
    }

    static boolean isLinux() {
        return (OS.indexOf("linux") >= 0)
    }

    static String testPath(String path) {
        if (isWindows()) {
            if (path.startsWith("/")) {
                path = "C:" + path
            }

            path = path.replace('/', '\\')
        }

        path
    }
}
