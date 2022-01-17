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
import net.wooga.uvm.UnityVersionManager
import org.junit.Rule
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Unroll
import wooga.gradle.unity.UnityPlugin
import wooga.gradle.unity.models.BuildTarget
import wooga.gradle.unity.tasks.Unity

class UvmCheckInstalltionIntegrationSpec extends IntegrationSpec {

    @Rule
    ProjectGeneratorRule unityProject = new ProjectGeneratorRule()

    def setup() {
        buildFile << """
            ${applyPlugin(UnityVersionManagerPlugin)}
            ${applyPlugin(UnityPlugin)}

            task(customUnity, type: ${Unity.name})

        """.stripIndent()
        unityProject.projectDir = projectDir
    }

    static String unityTestVersion() {
        "2019.4.30f1"
    }

    @Unroll("#message and autoSwitchUnityEditor is true")
    def "task :checkUnityInstallation #message and autoSwitchUnityEditor is true"() {
        given: "A project with a mocked unity version"
        unityProject.setProjectVersion(editorVersion)

        and: "version switch enabled"
        buildFile << """
        uvm.autoSwitchUnityEditor = true
        """.stripIndent()

        and: "and a custom set unity path not matching the project version"
        buildFile << """
        unity.unityPath = file("${baseVersion.executable.absolutePath}")
        """.stripIndent()

        when:
        def result = runTasks("customUnity")

        then:
        println(result.standardError)
        println(result.standardOutput)
        result.success
        result.standardOutput.contains(expectedUnityPath.path)

        where:
        editorVersion | expectedUnityPath
        "2030.1.2f3"  | preInstalledUnity2019_4_31f1.executable
        baseVersion = preInstalledUnity2019_4_31f1
        message = "keeps configured path when unity is not found"
    }

    @Unroll("#message if autoSwitchUnityEditor is #autoSwitchEnabled")
    def "task :checkUnityInstallation #message if autoSwitchUnityEditor is #autoSwitchEnabled"() {
        given: "A project with a mocked unity version"
        unityProject.setProjectVersion(editorVersion.version)

        and: "version switch enabled"
        buildFile << """
        uvm.autoSwitchUnityEditor = ${autoSwitchEnabled}
        """.stripIndent()

        and: "and a custom set unity path non matching project version"
        buildFile << """
        unity.unityPath = file("${baseVersion.executable}")
        """.stripIndent()

        when:
        def result = runTasks("customUnity")

        then:
        result.standardOutput.contains(expectedUnityPath.absolutePath)

        where:
        editorVersion                | expectedUnityPath                     | autoSwitchEnabled
        preInstalledUnity2019_4_31f1 | editorVersion.location                | true
        preInstalledUnity2019_4_31f1 | preInstalledUnity2019_4_32f1.location | false
        baseVersion = preInstalledUnity2019_4_32f1
        message = autoSwitchEnabled ? "switches path to unity" : "keeps configured path to unity"
    }


    @Unroll("#message")
    def "task :checkUnityInstallation #message if autoInstallUnityEditor is #autoInstallEnabled and autoSwitchUnityEditor is #autoSwitchEnabled"() {
        given: "A project with a mocked unity version"
        unityProject.setProjectVersion(editorVersion)

        and: "version switch and install enabled"
        buildFile << """
        uvm.autoSwitchUnityEditor = ${autoSwitchEnabled}
        uvm.autoInstallUnityEditor = ${autoInstallEnabled}
        """.stripIndent()

        and: "and a custom set unity path no matching project version"
        buildFile << """
        unity.unityPath = file("${baseVersion.executable.absolutePath}")
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("customUnity")

        then:
        if (autoSwitchEnabled && autoInstallEnabled) {
            def expectedUnityPath = new File(projectDir, installPath)
            result.standardOutput.contains(expectedUnityPath.path)
        } else {
            result.standardOutput.contains("${baseVersion.executable.absolutePath}")
        }

        cleanup:
        new File(projectDir, installPath).deleteDir()

        where:
        editorVersion      | autoInstallEnabled | autoSwitchEnabled
        unityTestVersion() | false              | false
        unityTestVersion() | true               | false
        unityTestVersion() | false              | true
        unityTestVersion() | true               | true

        installPath = "build/unity_installations/${editorVersion}"
        baseVersion = preInstalledUnity2019_4_31f1
        message = (autoInstallEnabled && autoSwitchEnabled) ? "installs & uses version" : "uses default version"
    }

    @Unroll("fails if version can't be installed")
    def "task :checkUnityInstallation fails if version can't be installed"() {
        given: "A project with a mocked unity version"
        unityProject.setProjectVersion(editorVersion)

        and: "version switch and install enabled"
        buildFile << """
        uvm.autoSwitchUnityEditor = true
        uvm.autoInstallUnityEditor = true
        """.stripIndent()

        and: "and a custom set unity path no matching project version"
        buildFile << """
        unity.unityPath = file("${baseVersion.executable.absolutePath}")
        """.stripIndent()

        expect:
        def result = runTasksWithFailure("customUnity")
        result.standardError.contains("Unable to install requested unity version ${editorVersion}")

        where:
        editorVersion = "2030.1.0f1"
        baseVersion = preInstalledUnity2019_4_31f1
    }

    @Unroll("when: #buildTarget")
    def "task :checkUnityInstallation #message when task contains buildTarget: #buildTarget"() {
        given: "A project with a mocked unity version"
        unityProject.setProjectVersion(editorVersion)

        and: "version switch and install enabled"
        buildFile << """
        uvm.autoSwitchUnityEditor = true
        uvm.autoInstallUnityEditor = true
        """.stripIndent()

        and: "and a custom set unity path no matching project version"
        buildFile << """
        unity.unityPath = file("${baseVersion.executable.absolutePath}")
        """.stripIndent()

        and: "and a unity installation without components"
        def installation = UnityVersionManager.installUnityEditor(editorVersion, new File(projectDir, installPath))
        assert installation

        and: "a configured build target in unity task"
        buildFile << "customUnity.buildTarget = '${buildTarget}'"

        when:
        runTasksSuccessfully("customUnity")

        then:
        if (expectedComponent) {
            installation.components.equals(expectedComponent)
        } else {
            installation.components.size() == 0
        }


        cleanup:
        new File(projectDir, installPath).deleteDir()

        where:
        editorVersion       | buildTarget           | expectedComponent
        unityTestVersion()  | BuildTarget.android   | [Component.android]
        unityTestVersion()  | BuildTarget.ios       | [Component.ios]
        unityTestVersion()  | BuildTarget.linux64   | [Component.linuxMono, Component.linuxIL2CPP]
        unityTestVersion()  | BuildTarget.win64     | [Component.windowsMono]

        installPath = "build/unity_installations/${editorVersion}"
        baseVersion = preInstalledUnity2019_4_31f1
        message = expectedComponent ? "installs: ${expectedComponent}" : "installs no component"
    }

    @Unroll("installs missing components")
    def "task :checkUnityInstallation installs multiple missing components"() {
        given: "A project with a mocked unity version"
        unityProject.setProjectVersion(editorVersion)

        and: "make sure version is not installed"
        def i = UnityVersionManager.locateUnityInstallation(editorVersion)
        if(i) {
            i.location.deleteDir()
        }

        and: "version switch and install enabled"
        buildFile << """
        uvm.autoSwitchUnityEditor = true
        uvm.autoInstallUnityEditor = true
        """.stripIndent()

        and: "and a custom set unity path no matching project version"
        buildFile << """
        unity.unityPath = file("${baseVersion.location.absolutePath}")
        """.stripIndent()

        and: "and a unity installation without components"
        def installation = UnityVersionManager.installUnityEditor(editorVersion, new File(projectDir, installPath))
        assert installation

        and: "multiple configured build targets in unity tasks"
        buildFile << """
            customUnity.buildTarget = '${buildTarget1}'

            task(customUnity2, type: ${Unity.name}) {
                buildTarget = '${buildTarget2}'
            }
        """

        when:
        runTasksSuccessfully("customUnity", "customUnity2")

        then:
        installation.components.contains(expectedComponent1)
        installation.components.contains(expectedComponent2)

        cleanup:
        new File(projectDir, installPath).deleteDir()

        where:
        editorVersion = unityTestVersion()
        installPath = "build/unity_installations/${editorVersion}"
        baseVersion = preInstalledUnity2019_4_31f1

        buildTarget1 = BuildTarget.ios
        buildTarget2 = BuildTarget.android
        expectedComponent1 = Component.ios
        expectedComponent2 = Component.android

    }

    @Unroll("installs required components")
    def "task :checkUnityInstallation installs only required components in current build"() {
        given: "A project with a mocked unity version"
        unityProject.setProjectVersion(editorVersion)

        and: "version switch and install enabled"
        buildFile << """
        uvm.autoSwitchUnityEditor = true
        uvm.autoInstallUnityEditor = true
        """.stripIndent()

        and: "and a custom set unity path no matching project version"
        buildFile << """
        unity.unityPath = file("${baseVersion.executable.absolutePath}")
        """.stripIndent()

        and: "make sure version is not installed"
        def i = UnityVersionManager.locateUnityInstallation(editorVersion)
        if(i) {
            i.location.deleteDir()
        }

        and: "and a unity installation without components"
        def installation = UnityVersionManager.installUnityEditor(editorVersion, new File(projectDir, installPath))
        assert installation

        and: "multiple configured build targets in unity tasks"
        buildFile << """
            customUnity.buildTarget = '${buildTarget1}'

            task(customUnity2, type: ${Unity.name}) {
                buildTarget = '${buildTarget2}'
            }
        """

        when:
        runTasksSuccessfully("customUnity")

        then:
        installation.components.contains(expectedComponent)

        cleanup:
        new File(projectDir, installPath).deleteDir()

        where:
        editorVersion = unityTestVersion()
        installPath = "build/unity_installations/${editorVersion}"
        baseVersion = preInstalledUnity2019_4_31f1
        buildTarget1 = BuildTarget.ios
        buildTarget2 = BuildTarget.android
        expectedComponent = Component.ios
    }
}
