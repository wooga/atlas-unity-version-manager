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

import spock.lang.Unroll
import wooga.gradle.unity.UnityPlugin
import wooga.gradle.unity.tasks.Unity

class UnityVersionManagerPluginIntegrationSpec extends IntegrationSpec {

    def setup() {
        buildFile << """
            ${applyPlugin(UnityVersionManagerPlugin)}
        """.stripIndent()
    }

    @Unroll
    def "task :#taskName prints uvm version"() {
        given: "default plugin setup"

        expect:
        runTasksSuccessfully(taskName).standardOutput.contains("uvm core version: ${expectedVersion}")

        where:
        taskName     | expectedVersion
        "uvmVersion" | "0.2.0"
    }


    def "creates hooks into atlas-unity when plugin is applied"() {
        given: "a project with atlas-unity applied"
        buildFile << """
            ${applyPlugin(UnityPlugin)}

            task(customUnity, type: ${Unity.name})

        """.stripIndent()

        when:
        def result = runTasks("customUnity")

        then:
        result.wasExecuted("customUnity")
        result.wasExecuted("checkUnityInstallation")
    }
}
