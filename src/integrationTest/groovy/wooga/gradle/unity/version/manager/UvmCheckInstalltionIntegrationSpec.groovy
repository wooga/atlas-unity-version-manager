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
import spock.lang.Requires
import spock.lang.Unroll
import wooga.gradle.unity.UnityPlugin
import wooga.gradle.unity.tasks.Unity

@Requires({ os.macOs })
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

    static List<String> _installedUnityVersions

    List<String> installedUnityVersions() {
        if (_installedUnityVersions) {
            return _installedUnityVersions
        }
        def applications = new File("/Applications")
        _installedUnityVersions = applications.listFiles(new FilenameFilter() {
            @Override
            boolean accept(File dir, String name) {
                return name.startsWith("Unity-")
            }
        }).collect {
            it.name.replace("Unity-", "")
        }
    }

    @Unroll
    def "task :checkUnityInstallation #message and autoSwitchUnityEditor is true"() {
        given: "A project with a mocked unity version"
        unityProject.setProjectVersion(editorVersion)

        and: "version switch enabled"
        buildFile << """
        uvm.autoSwitchUnityEditor = true
        """.stripIndent()

        and: "and a custom set unity path no matching project version"
        buildFile << """
        unity.unityPath = file("/Applications/Unity-${baseVersion}/Unity.app/Contents/MacOS/Unity")
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("customUnity")

        then:
        result.standardOutput.contains(expectedUnityPath.path)

        where:
        editorVersion | expectedUnityPath
        "2030.1.2f3"  | new File("/Applications/Unity-${installedUnityVersions().last()}")
        baseVersion = installedUnityVersions().last()
        message = "keeps configured path to unity when unity version can't be found"
    }

    @Unroll
    def "task :checkUnityInstallation #message if autoSwitchUnityEditor is #autoSwitchEnabled"() {
        given: "A project with a mocked unity version"
        unityProject.setProjectVersion(editorVersion)

        and: "version switch enabled"
        buildFile << """
        uvm.autoSwitchUnityEditor = ${autoSwitchEnabled}
        """.stripIndent()

        and: "and a custom set unity path no matching project version"
        buildFile << """
        unity.unityPath = file("/Applications/Unity-${baseVersion}/Unity.app/Contents/MacOS/Unity")
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("customUnity")

        then:
        result.standardOutput.contains(expectedUnityPath.path)

        where:
        editorVersion                    | expectedUnityPath                                                   | autoSwitchEnabled
        installedUnityVersions().first() | new File("/Applications/Unity-${installedUnityVersions().first()}") | true
        installedUnityVersions().first() | new File("/Applications/Unity-${installedUnityVersions().last()}")  | false
        baseVersion = installedUnityVersions().last()
        message = autoSwitchEnabled ? "switches path to unity" : "keeps configured path to unity"
    }
}