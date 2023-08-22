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
import net.wooga.uvm.Component
import org.junit.Rule
import spock.lang.Shared
import spock.lang.Unroll
import wooga.gradle.unity.UnityPlugin
import wooga.gradle.unity.UnityTask
import wooga.gradle.unity.tasks.Unity

class UnityVersionManagerExtensionIntegrationSpec extends IntegrationSpec {

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
                    return "value is provided in script"
                case property:
                    return "value is provided in props"
                case env:
                    return "value is set in env"
                default:
                    return "no value was configured"
            }
        }
    }

    String envNameFromProperty(String property) {
        "UVM_${property.replaceAll(/([A-Z])/, "_\$1").toUpperCase()}"
    }

    @Unroll(":#property returns '#testValue' if #reason")
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

        def escapedValue = (value instanceof String) ? escapedPath(value) : value

        switch (location) {
            case PropertyLocation.script:
                buildFile << "uvm.${property} = ${escapedValue}"
                break
            case PropertyLocation.property:
                propertiesFile << "uvm.${property} = ${escapedValue}"
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
            testValue = testValue.replaceAll("#projectDir#", escapedPath(projectDir.path))
        }

        when: ""
        def result = runTasksSuccessfully("custom")

        then:
        result.standardOutput.contains("uvm.${property}: ${testValue}")

        where:
        property                 | value                                 | expectedValue                                      | providedValue | location
        "autoSwitchUnityEditor"  | true                                  | _                                                  | true          | PropertyLocation.env
        "autoSwitchUnityEditor"  | true                                  | _                                                  | 1             | PropertyLocation.env
        "autoSwitchUnityEditor"  | true                                  | _                                                  | 'TRUE'        | PropertyLocation.env
        "autoSwitchUnityEditor"  | true                                  | _                                                  | 'y'           | PropertyLocation.env
        "autoSwitchUnityEditor"  | true                                  | _                                                  | 'yes'         | PropertyLocation.env
        "autoSwitchUnityEditor"  | true                                  | _                                                  | 'YES'         | PropertyLocation.env
        "autoSwitchUnityEditor"  | true                                  | _                                                  | true          | PropertyLocation.property
        "autoSwitchUnityEditor"  | true                                  | _                                                  | 1             | PropertyLocation.property
        "autoSwitchUnityEditor"  | true                                  | _                                                  | 'TRUE'        | PropertyLocation.property
        "autoSwitchUnityEditor"  | true                                  | _                                                  | 'y'           | PropertyLocation.property
        "autoSwitchUnityEditor"  | true                                  | _                                                  | 'yes'         | PropertyLocation.property
        "autoSwitchUnityEditor"  | true                                  | _                                                  | 'YES'         | PropertyLocation.property
        "autoSwitchUnityEditor"  | true                                  | _                                                  | true          | PropertyLocation.script
        "autoSwitchUnityEditor"  | false                                 | _                                                  | null          | PropertyLocation.none

        "autoInstallUnityEditor" | true                                  | _                                                  | true          | PropertyLocation.env
        "autoInstallUnityEditor" | true                                  | _                                                  | 1             | PropertyLocation.env
        "autoInstallUnityEditor" | true                                  | _                                                  | 'TRUE'        | PropertyLocation.env
        "autoInstallUnityEditor" | true                                  | _                                                  | 'y'           | PropertyLocation.env
        "autoInstallUnityEditor" | true                                  | _                                                  | 'yes'         | PropertyLocation.env
        "autoInstallUnityEditor" | true                                  | _                                                  | 'YES'         | PropertyLocation.env
        "autoInstallUnityEditor" | true                                  | _                                                  | true          | PropertyLocation.property
        "autoInstallUnityEditor" | true                                  | _                                                  | 1             | PropertyLocation.property
        "autoInstallUnityEditor" | true                                  | _                                                  | 'TRUE'        | PropertyLocation.property
        "autoInstallUnityEditor" | true                                  | _                                                  | 'y'           | PropertyLocation.property
        "autoInstallUnityEditor" | true                                  | _                                                  | 'yes'         | PropertyLocation.property
        "autoInstallUnityEditor" | true                                  | _                                                  | 'YES'         | PropertyLocation.property
        "autoInstallUnityEditor" | true                                  | _                                                  | true          | PropertyLocation.script
        "autoInstallUnityEditor" | false                                 | _                                                  | null          | PropertyLocation.none
        "unityInstallBaseDir"    | testPath("/custom/path")              | _                                                  | "path string" | PropertyLocation.env
        "unityInstallBaseDir"    | testPath("/custom/path")              | _                                                  | "path string" | PropertyLocation.property
        "unityInstallBaseDir"    | "file('${escapedPath(testPath("/custom/path"))}')" | testPath("/custom/path")                           | "path string" | PropertyLocation.script
        "unityInstallBaseDir"    | null                                  | testPath("#projectDir#/build/unity_installations") | "null"        | PropertyLocation.none
        "unityVersion"           | "2017.2.4f5"                          | _                                                  | "2017.2.4f5"  | PropertyLocation.env
        "unityVersion"           | "2018.3.1b4"                          | _                                                  | "2018.3.1b4"  | PropertyLocation.property
        "unityVersion"           | "'2019.2.1f1'"                        | "2019.2.1f1"                                       | "2019.2.1f1"  | PropertyLocation.script
        "unityVersion"           | null                                  | defaultProjectVersion                              | "null"        | PropertyLocation.none

        testValue = (expectedValue == _) ? value : expectedValue
        reason = location.reason() + ((location == PropertyLocation.none) ? "" : " with '$providedValue'")
    }

    @Unroll("buildRequiredUnityComponentsProvider returns required components based on build state")
    def "has provider object resolve unity components"() {
        given: "a project with atlas-unity applied"
        buildFile << """
            
            uvm {
                unityVersion = "${defaultProjectVersion}"
            }

            tasks.register('customUnityIos', ${Unity.name}) {
                buildTarget = 'ios'
            }
            
            tasks.register('customUnityAndroid', ${Unity.name}) {
                buildTarget = 'android'
            }
            
            tasks.register('customUnityWebGl', ${Unity.name}) {
                buildTarget = 'webGl'
            }
            
            [customUnityIos, customUnityAndroid, customUnityWebGl].each {it.actions = []}
            
            import static wooga.gradle.unity.version.manager.UnityVersionManagerPlugin.BuildTargetToComponents.buildTargetToComponents
            
            tasks.register('printComponents') {
                def allComponents = project.tasks.withType(wooga.gradle.unity.UnityTask).collect({
                    buildTargetToComponents(it.buildTarget, uvm.unityVersion).getOrNull()
                }).findAll{it != null}.flatten()
                println("print during task configuration: " + allComponents.sort())    
                doLast {
                    println("print during task execution: " + uvm.buildRequiredUnityComponents.get().sort())
                }
            }
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("printComponents", *tasksToExecute)

        then:

        result.standardOutput.contains("print during task configuration: ${unfilteredComponentes.sort()}")


        result.standardOutput.contains("print during task execution: ${expectedFilteredComponentes.sort()}")

        where:
        unfilteredComponentes                               | filteredComponentes                | tasksToExecute
        [Component.ios, Component.android, Component.webGl] | _                                  | ["customUnityIos", "customUnityAndroid", "customUnityWebGl"]
        [Component.ios, Component.android, Component.webGl] | [Component.ios, Component.android] | ["customUnityIos", "customUnityAndroid"]
        [Component.ios, Component.android, Component.webGl] | [Component.ios]                    | ["customUnityIos"]

        expectedFilteredComponentes = (filteredComponentes == _) ? unfilteredComponentes : filteredComponentes
    }
}
