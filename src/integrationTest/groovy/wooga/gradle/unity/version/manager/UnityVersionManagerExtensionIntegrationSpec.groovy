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

import net.wooga.test.unity.ProjectGeneratorRule
import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import spock.lang.Shared
import spock.lang.Unroll
import wooga.gradle.unity.UnityPlugin

class UnityVersionManagerExtensionIntegrationSpec extends IntegrationSpec {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    ProjectGeneratorRule unityProject = new ProjectGeneratorRule()

    @Shared
    String defaultProjectVersion = "2030.1.4f1"

    def setup() {
        buildFile << """
            ${applyPlugin(UnityVersionManagerPlugin)}
            ${applyPlugin(UnityPlugin)}
        """.stripIndent()

        unityProject.setProjectVersion(defaultProjectVersion)
        unityProject.projectDir = projectDir
    }

    enum PropertyLocation {
        none, script, property, env

        String reason() {
            switch (this) {
                case script:
                    return "value is provided in build script"
                case property:
                    return "value is provided in properties"
                case env:
                    return "value is provided in environment"
                default:
                    return "no value was configured"
            }
        }
    }

    String envNameFromProperty(String property) {
        "UVM_${property.replaceAll(/([A-Z])/, "_\$1").toUpperCase()}"
    }

    @Unroll
    def "extension property :#property returns '#testValue' if #reason"() {
        given:
        buildFile << """
            task(custom) {
                doLast {
                    def value = uvm.${property}.get()
                    println("uvm.${property}: " + value)
                }
            }
        """

        and: "a gradle.properties"
        def propertiesFile = createFile("gradle.properties")

        switch (location) {
            case PropertyLocation.script:
                buildFile << "uvm.${property} = ${value}"
                break
            case PropertyLocation.property:
                propertiesFile << "uvm.${property} = ${value}"
                break
            case PropertyLocation.env:
                environmentVariables.set(envNameFromProperty(property), "${value}")
                break
            default:
                break
        }

        and: "a mocked unity project"
        unityProject

        and: "the test value with replace placeholders"
        if (testValue instanceof String) {
            testValue = testValue.replaceAll("#projectDir#", projectDir.path)
        }

        when: ""
        def result = runTasksSuccessfully("custom")

        then:
        result.standardOutput.contains("uvm.${property}: ${testValue}")

        where:
        property                 | value                  | expectedValue                            | providedValue | location
        "autoSwitchUnityEditor"  | true                   | _                                        | true          | PropertyLocation.env
        "autoSwitchUnityEditor"  | true                   | _                                        | 1             | PropertyLocation.env
        "autoSwitchUnityEditor"  | true                   | _                                        | 'TRUE'        | PropertyLocation.env
        "autoSwitchUnityEditor"  | true                   | _                                        | 'y'           | PropertyLocation.env
        "autoSwitchUnityEditor"  | true                   | _                                        | 'yes'         | PropertyLocation.env
        "autoSwitchUnityEditor"  | true                   | _                                        | 'YES'         | PropertyLocation.env
        "autoSwitchUnityEditor"  | true                   | _                                        | true          | PropertyLocation.property
        "autoSwitchUnityEditor"  | true                   | _                                        | 1             | PropertyLocation.property
        "autoSwitchUnityEditor"  | true                   | _                                        | 'TRUE'        | PropertyLocation.property
        "autoSwitchUnityEditor"  | true                   | _                                        | 'y'           | PropertyLocation.property
        "autoSwitchUnityEditor"  | true                   | _                                        | 'yes'         | PropertyLocation.property
        "autoSwitchUnityEditor"  | true                   | _                                        | 'YES'         | PropertyLocation.property
        "autoSwitchUnityEditor"  | true                   | _                                        | true          | PropertyLocation.script
        "autoSwitchUnityEditor"  | false                  | _                                        | null          | PropertyLocation.none

        "autoInstallUnityEditor" | true                   | _                                        | true          | PropertyLocation.env
        "autoInstallUnityEditor" | true                   | _                                        | 1             | PropertyLocation.env
        "autoInstallUnityEditor" | true                   | _                                        | 'TRUE'        | PropertyLocation.env
        "autoInstallUnityEditor" | true                   | _                                        | 'y'           | PropertyLocation.env
        "autoInstallUnityEditor" | true                   | _                                        | 'yes'         | PropertyLocation.env
        "autoInstallUnityEditor" | true                   | _                                        | 'YES'         | PropertyLocation.env
        "autoInstallUnityEditor" | true                   | _                                        | true          | PropertyLocation.property
        "autoInstallUnityEditor" | true                   | _                                        | 1             | PropertyLocation.property
        "autoInstallUnityEditor" | true                   | _                                        | 'TRUE'        | PropertyLocation.property
        "autoInstallUnityEditor" | true                   | _                                        | 'y'           | PropertyLocation.property
        "autoInstallUnityEditor" | true                   | _                                        | 'yes'         | PropertyLocation.property
        "autoInstallUnityEditor" | true                   | _                                        | 'YES'         | PropertyLocation.property
        "autoInstallUnityEditor" | true                   | _                                        | true          | PropertyLocation.script
        "autoInstallUnityEditor" | false                  | _                                        | null          | PropertyLocation.none

        "unityInstallBaseDir"    | "/custom/path"         | _                                        | "path string" | PropertyLocation.env
        "unityInstallBaseDir"    | "/custom/path"         | _                                        | "path string" | PropertyLocation.property
        "unityInstallBaseDir"    | "file('/custom/path')" | "/custom/path"                           | "path string" | PropertyLocation.script
        "unityInstallBaseDir"    | null                   | "#projectDir#/build/unity_installations" | "null"        | PropertyLocation.none

        "unityVersion"           | "2017.2.4f5"           | _                                        | "2017.2.4f5"  | PropertyLocation.env
        "unityVersion"           | "2018.3.1b4"           | _                                        | "2018.3.1b4"  | PropertyLocation.property
        "unityVersion"           | "'2019.2.1f1'"         | "2019.2.1f1"                             | "2019.2.1f1"  | PropertyLocation.script
        "unityVersion"           | null                   | defaultProjectVersion                    | "null"        | PropertyLocation.none

        testValue = (expectedValue == _) ? value : expectedValue
        reason = location.reason() + ((location == PropertyLocation.none) ? "" : " with value '$providedValue'")
    }


}
