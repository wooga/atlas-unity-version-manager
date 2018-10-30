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

package net.wooga.uvm

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files

class UnityVersionManagerSpec extends Specification {

    @Shared
    def buildDir

    def setup() {
        buildDir = new File('build/unityVersionManagerSpec')
        buildDir.mkdirs()
    }

    @Unroll("can call :#method on UnityVersionManager")
    def "unity version manager interface doesn't crash"() {
        when:
        (UnityVersionManager.class).invokeMethod(method, *arguments)
        then:
        noExceptionThrown()

        where:
        method                    | arguments
        "uvmVersion"              | null
        "listInstallations"       | null
        "detectProjectVersion"    | [new File("")]
        "locateUnityInstallation" | null
    }

    def "can call :installUnityEditor on UnityVersionManager"() {
        when:
        UnityVersionManager.installUnityEditor(null, null)
        then:
        noExceptionThrown()
        when:
        UnityVersionManager.installUnityEditor(null, null, null)
        then:
        noExceptionThrown()
    }

    @Unroll
    def "uvmVersion is #expectedVersion"() {
        expect:
        UnityVersionManager.uvmVersion() == expectedVersion

        where:
        expectedVersion = "0.0.1"
    }

    File mockUnityProject(String editorVersion) {
        def outerDir = File.createTempDir("uvm_jni_projects_", "_base_path")
        def projectDir = new File(outerDir, "unity_testproject")
        def projectSettings = new File(projectDir, "ProjectSettings")
        projectSettings.mkdirs()

        def projectVersion = new File(projectSettings, "ProjectVersion.txt")
        projectVersion << "m_EditorVersion: ${editorVersion}"
        projectDir
    }

    List<String> installedUnityVersions() {
        def applications = new File("/Applications")
        applications.listFiles(new FilenameFilter() {
            @Override
            boolean accept(File dir, String name) {
                return name.startsWith("Unity-")
            }
        }).collect {
            it.name.replace("Unity-", "")
        }
    }

    @Unroll
    def "detectProjectVersion returns #resultMessage when #reason"() {
        expect:
        UnityVersionManager.detectProjectVersion(path) == expectedResult

        where:
        path                                      | reason                                                  | expectedResult
        null                                      | "path is null"                                          | null
        File.createTempDir()                      | "path points to an invalid location"                    | null
        mockUnityProject("2018.2b4")              | "editor version is invalid"                             | null
        mockUnityProject("2018.2.1b4")            | "path points to a unity project location"               | "2018.2.1b4"
        mockUnityProject("2017.1.2f3").parentFile | "path points to a directory containing a unity project" | "2017.1.2f3"
        resultMessage = expectedResult ? "the editor version" : "null"
    }

    @Unroll
    def "locateUnityInstallation returns #resultMessage when #reason"() {
        expect:
        UnityVersionManager.locateUnityInstallation(version) == expectedResult

        where:
        version                          | reason                          | expectedResult
        null                             | "version is null"               | null
        installedUnityVersions().first() | "when version is installed"     | new Installation(new File("/Applications/Unity-${installedUnityVersions().first()}"), installedUnityVersions().first())
        "1.1.1f1"                        | "when version is not installed" | null
        "2018.0.1"                       | "when version is invalid"       | null

        resultMessage = expectedResult ? "the unity location" : "null"
    }

    @Unroll
    def "listInstallations returns list of installed versions"() {
        given: "some installed versions"
        def v = installedUnityVersions()

        when: "fetch installations"
        def installations = UnityVersionManager.listInstallations()

        then:
        installations != null
        def versions = installations.collect { it.version }
        v.each { version ->
            versions.contains(version)
        }
    }

    def "installUnityEditor installs unity to location"() {
        given: "a version to install"
        def version = "2017.1.0f1"
        assert !UnityVersionManager.listInstallations().collect({ it.version }).contains(version)

        and: "a temp install location"
        def basedir = Files.createTempDirectory(buildDir.toPath(), "installUnityEditor_without_components").toFile()
        def destination = new File(basedir, version)
        assert !destination.exists()

        when:
        def result = UnityVersionManager.installUnityEditor(version, destination)

        then:
        result != null
        result.location.exists()
        result.location == destination
        result.version == version
        UnityVersionManager.listInstallations().collect({ it.version }).contains(version)

        cleanup:
        destination.deleteDir()
    }

    def "installUnityEditor installs unity and components to location"() {
        given: "a version to install"
        def version = "2017.1.0f1"
        assert !UnityVersionManager.listInstallations().collect({ it.version }).contains(version)

        and: "a temp install location"
        def basedir = Files.createTempDirectory(buildDir.toPath(), "installUnityEditor_with_components").toFile()
        def destination = new File(basedir, version)
        assert !destination.exists()

        and: "no engines"
        def playbackEngines = new File(destination, "PlaybackEngines")
        assert !playbackEngines.exists()

        when:
        def result = UnityVersionManager.installUnityEditor(version, destination, [Component.android, Component.ios].toArray() as Component[])

        then:
        result != null
        result.location.exists()
        result.location == destination
        result.version == version
        UnityVersionManager.listInstallations().collect({ it.version }).contains(version)
        destination.exists()
        playbackEngines.exists()
        new File(playbackEngines, "iOSSupport").exists()
        new File(playbackEngines, "AndroidPlayer").exists()

        cleanup:
        destination.deleteDir()
    }
}
