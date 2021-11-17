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

import net.wooga.uvm.Component
import net.wooga.uvm.UnityVersionManager
import spock.lang.Unroll

class UvmInstallUnityIntegrationSpec extends IntegrationSpec {
    def setup() {
        buildFile << """
            ${applyPlugin(UnityVersionManagerPlugin)}
        """.stripIndent()
    }

    static String unityTestVersion() {
        "2019.1.0b"
    }

    @Unroll("installs unity #components_message #destination_message")
    def "task :#taskToRun installs unity version #version #components_message #destination_message with options: #options"() {
        given: "the destination exists"
        def destinationDir
        if (destination) {
            destinationDir = new File(projectDir, destination)
            destinationDir.mkdirs()
            destinationDir.deleteOnExit()
        }

        when:
        runTasksSuccessfully(taskToRun, *options)

        then:
        def installation = UnityVersionManager.locateUnityInstallation(version)
        if (destinationDir) {
            installation.location.absolutePath == destinationDir.absolutePath
        }
        installation.components.toList().containsAll(components.toList())

        cleanup:
        if (destinationDir) {
            destinationDir.deleteDir()
        } else if (installation) {
            installation.location.deleteDir()
        }

        where:
        taskToRun      | version                  | components                         | destination
        "installUnity" | "${unityTestVersion()}1" | []                                 | "build/unity"
        "installUnity" | "${unityTestVersion()}2" | [Component.android, Component.ios] | "build/unity"
        "installUnity" | "${unityTestVersion()}3" | [Component.webGl]                  | "build/unity"
//      "installUnity" | "2017.1.0f1" | []                                 | null
//      "installUnity" | "2017.1.0f2" | [Component.android, Component.ios] | null
//      "installUnity" | "2017.1.0f3" | [Component.webGl]                  | null

        components_message = components.size() > 0 ? "with components" : ""
        destination_message = destination ? "" : "to default destination"
        options = (components.collect({
            "--component=${it}"
        }) << ((destination) ? "--destination=${destination}" : "") << ((version) ? "--version=${version}" : "")).findAll({
            it != ""
        })
    }

    @Unroll("installs unity version set in project #components_message")
    def "task :#taskToRun installs unity version '#version' set in project #components_message #destination_message with options: #options"() {
        given: "the destination exists"
        def destinationDir
        if (destination) {
            destinationDir = new File(projectDir, destination)
            destinationDir.mkdirs()
            destinationDir.deleteOnExit()
        }

        and: "version set in project"
        buildFile << """
            uvm.unityVersion = "${version}"
        """.stripIndent()

        when:
        runTasksSuccessfully(taskToRun, *options)

        then:
        def installation = UnityVersionManager.locateUnityInstallation(version)
        if (destinationDir) {
            installation.location.absolutePath == destinationDir.absolutePath
        }
        installation.components.toList().containsAll(components.toList())

        cleanup:
        if (destinationDir) {
            destinationDir.deleteDir()
        } else if (installation) {
            installation.location.deleteDir()
        }

        where:
        taskToRun      | version                  | components                         | destination
        "installUnity" | "${unityTestVersion()}1" | []                                 | "build/unity"
        "installUnity" | "${unityTestVersion()}2" | [Component.android, Component.ios] | "build/unity"
        "installUnity" | "${unityTestVersion()}3" | [Component.webGl]                  | "build/unity"
//      "installUnity" | "2017.1.0f1" | []                                 | null
//      "installUnity" | "2017.1.0f2" | [Component.android, Component.ios] | null
//      "installUnity" | "2017.1.0f3" | [Component.webGl]                  | null

        components_message = components.size() > 0 ? "with components" : ""
        destination_message = destination ? "" : "to default destination"
        options = (components.collect({
            "--component=${it}"
        }) << ((destination) ? "--destination=${destination}" : "")).findAll({ it != "" })
    }

    def "help prints commandline description for installUnity"() {
        when:
        def result = runTasksSuccessfully("help", "--task", "installUnity")

        then:
        result.standardOutput.contains("Path")
        result.standardOutput.contains("Type")
        result.standardOutput.contains("Options")
        result.standardOutput.contains("--component")
        result.standardOutput.contains("--destination")
        result.standardOutput.contains("--version")
        result.standardOutput.contains("Description")
        result.standardOutput.contains("Group")
    }
}
