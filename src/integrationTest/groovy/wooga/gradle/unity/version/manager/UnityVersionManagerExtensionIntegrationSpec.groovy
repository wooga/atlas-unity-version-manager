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

import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import spock.lang.Unroll
import wooga.gradle.unity.UnityPlugin

class UnityVersionManagerExtensionIntegrationSpec extends IntegrationSpec {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();


    def setup() {
        buildFile << """
            ${applyPlugin(UnityVersionManagerPlugin)}
            ${applyPlugin(UnityPlugin)}
        """.stripIndent()
    }

    enum PropertyLocation {
        none, script, property, env
    }

    @Unroll
    def "extension property :#property returns '#value' if #reason with value '#providedValue'"() {
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
                environmentVariables.set("UVM_AUTO_SWITCH_UNITY_EDITOR", "${value}")
                break
            default:
                break
        }

        when:
        def result = runTasksSuccessfully("custom")

        then:
        result.standardOutput.contains("uvm.${property}: ${value}")

        where:
        property                | value | providedValue | reason                           | location
        "autoSwitchUnityEditor" | true  | true          | "value provided in env"          | PropertyLocation.env
        "autoSwitchUnityEditor" | true  | 1             | "value provided in env"          | PropertyLocation.env
        "autoSwitchUnityEditor" | true  | 'TRUE'        | "value provided in env"          | PropertyLocation.env
        "autoSwitchUnityEditor" | true  | 'y'           | "value provided in env"          | PropertyLocation.env
        "autoSwitchUnityEditor" | true  | 'yes'         | "value provided in env"          | PropertyLocation.env
        "autoSwitchUnityEditor" | true  | 'YES'         | "value provided in env"          | PropertyLocation.env
        "autoSwitchUnityEditor" | true  | true          | "value provided in properties"   | PropertyLocation.property
        "autoSwitchUnityEditor" | true  | 1             | "value provided in properties"   | PropertyLocation.property
        "autoSwitchUnityEditor" | true  | 'TRUE'        | "value provided in properties"   | PropertyLocation.property
        "autoSwitchUnityEditor" | true  | 'y'           | "value provided in properties"   | PropertyLocation.property
        "autoSwitchUnityEditor" | true  | 'yes'         | "value provided in properties"   | PropertyLocation.property
        "autoSwitchUnityEditor" | true  | 'YES'         | "value provided in properties"   | PropertyLocation.property
        "autoSwitchUnityEditor" | true  | true          | "value provided in build script" | PropertyLocation.script
        "autoSwitchUnityEditor" | false | null          | "no value is set"                | PropertyLocation.none
    }


}
