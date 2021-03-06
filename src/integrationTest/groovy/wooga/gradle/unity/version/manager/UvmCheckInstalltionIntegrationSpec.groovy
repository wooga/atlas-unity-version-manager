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
import spock.lang.IgnoreIf
import spock.lang.Unroll
import wooga.gradle.unity.UnityPlugin
import wooga.gradle.unity.batchMode.BuildTarget
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
        if (isLinux()) {
            return "2019.1.0b1"
        }

        "2017.1.0f1"
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
        unity.unityPath = file("${pathToUnityVersion(baseVersion)}")
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("customUnity")

        then:
        result.standardOutput.contains(expectedUnityPath.path)

        where:
        editorVersion | expectedUnityPath
        "2030.1.2f3"  | unityVersion(installedUnityVersions().last())
        baseVersion = installedUnityVersions().last()
        message = "keeps configured path when unity is not found"
    }

    @Unroll("#message if autoSwitchUnityEditor is #autoSwitchEnabled")
    def "task :checkUnityInstallation #message if autoSwitchUnityEditor is #autoSwitchEnabled"() {
        given: "A project with a mocked unity version"
        unityProject.setProjectVersion(editorVersion)

        and: "version switch enabled"
        buildFile << """
        uvm.autoSwitchUnityEditor = ${autoSwitchEnabled}
        """.stripIndent()

        and: "and a custom set unity path non matching project version"
        buildFile << """
        unity.unityPath = file("${pathToUnityVersion(baseVersion)}")
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("customUnity")

        then:
        result.standardOutput.contains(expectedUnityPath.path)

        where:
        editorVersion                    | expectedUnityPath                                                      | autoSwitchEnabled
        installedUnityVersions().first() | new File(baseUnityPath(), "Unity-${installedUnityVersions().first()}") | true
        installedUnityVersions().first() | new File(baseUnityPath(), "Unity-${installedUnityVersions().last()}")  | false
        baseVersion = installedUnityVersions().last()
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
        unity.unityPath = file("${pathToUnityVersion(baseVersion)}")
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("customUnity")

        then:
        if (autoSwitchEnabled && autoInstallEnabled) {
            def expectedUnityPath = new File(projectDir, installPath)
            result.standardOutput.contains(expectedUnityPath.path)
        } else {
            result.standardOutput.contains("${pathToUnityVersion(baseVersion)}")
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
        baseVersion = installedUnityVersions().last()
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
        unity.unityPath = file("${pathToUnityVersion(baseVersion)}")
        """.stripIndent()

        expect:
        def result = runTasksWithFailure("customUnity")
        result.standardOutput.contains("Unable to install requested unity version ${editorVersion}")

        where:
        editorVersion = "2030.1.0f1"
        baseVersion = installedUnityVersions().last()
    }

    @IgnoreIf({os.linux})
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
        unity.unityPath = file("${pathToUnityVersion(baseVersion)}")
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
            installation.components.contains(expectedComponent)
        } else {
            installation.components.size() == 0
        }


        cleanup:
        new File(projectDir, installPath).deleteDir()

        where:
        editorVersion = unityTestVersion()
        installPath = "build/unity_installations/${editorVersion}"
        baseVersion = installedUnityVersions().last()
        buildTarget << BuildTarget.values().toList()
        expectedComponent << BuildTarget.values().collect { UnityVersionManagerPlugin.buildTargetToComponent(it) }
        message = expectedComponent ? "installs: ${expectedComponent}" : "installs no component"
    }

    @Unroll("installs missing components")
    def "task :checkUnityInstallation installs multiple missing components"() {
        given: "A project with a mocked unity version"
        unityProject.setProjectVersion(editorVersion)

        and: "version switch and install enabled"
        buildFile << """
        uvm.autoSwitchUnityEditor = true
        uvm.autoInstallUnityEditor = true
        """.stripIndent()

        and: "and a custom set unity path no matching project version"
        buildFile << """
        unity.unityPath = file("${pathToUnityVersion(baseVersion)}")
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
        baseVersion = installedUnityVersions().last()
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
        unity.unityPath = file("${pathToUnityVersion(baseVersion)}")
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
        runTasksSuccessfully("customUnity")

        then:
        installation.components.contains(expectedComponent)

        cleanup:
        new File(projectDir, installPath).deleteDir()

        where:
        editorVersion = unityTestVersion()
        installPath = "build/unity_installations/${editorVersion}"
        baseVersion = installedUnityVersions().last()
        buildTarget1 = BuildTarget.ios
        buildTarget2 = BuildTarget.android
        expectedComponent = Component.ios
    }
}
