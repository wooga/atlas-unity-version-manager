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

import net.wooga.uvm.UnityVersionManager
import spock.lang.Unroll

class UvmListInstallatonsIntegrationSpec extends IntegrationSpec {
    def setup() {
        buildFile << """
            ${applyPlugin(UnityVersionManagerPlugin)}
        """.stripIndent()
    }

    @Unroll("task :#taskToRun #message #additionalOptions to #destination with options: #options")
    def "list installed versions"() {
        given: "some installed versions"
        def v = UnityVersionManager.listInstallations().toList()

        when:
        def result = runTasksSuccessfully(taskToRun, *options)

        then:
        String out = (destination == "stdout") ? result.standardOutput : new File(projectDir, "build/installations.txt").text

        out.contains("installations:")
        v.every {
            def format = expectedOutputFormat.replace("VERSION", it.version).replace("PATH", it.location.path)
            out.contains(format)
        }

        where:
        taskToRun           | message                     | options                                                   | destination | additionalOptions | expectedOutputFormat
        "listInstallations" | "prints installed versions" | []                                                        | "stdout"    | ""                | "VERSION"
        "listInstallations" | "prints installed versions" | ["--print-path"]                                          | "stdout"    | "with path"       | "VERSION - PATH"
        "listInstallations" | "prints installed versions" | ["--output-path=build/installations.txt"]                 | "to file"   | ""                | "VERSION"
        "listInstallations" | "prints installed versions" | ["--print-path", "--output-path=build/installations.txt"] | "to file"   | "with path"       | "VERSION - PATH"
    }

    def "help prints commandline description for listInstallations"() {
        when:
        def result = runTasksSuccessfully("help", "--task", "listInstallations")

        then:
        result.standardOutput.contains("Path")
        result.standardOutput.contains("Type")
        result.standardOutput.contains("Options")
        result.standardOutput.contains("--print-path")
        result.standardOutput.contains("--output-path")
        result.standardOutput.contains("Description")
        result.standardOutput.contains("Group")
    }
}
