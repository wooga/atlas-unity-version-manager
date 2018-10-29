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

import nebula.test.ProjectSpec
import org.gradle.api.DefaultTask
import org.junit.Rule
import org.junit.contrib.java.lang.system.RestoreSystemProperties
import spock.lang.Unroll
import wooga.gradle.unity.version.manager.tasks.UvmListInstallations

class UnityVersionManagerPluginSpec extends ProjectSpec {
    public static final String PLUGIN_NAME = 'net.wooga.unity-version-manager'

    @Unroll("creates the task #taskName")
    def 'Creates needed tasks'(String taskName, Class taskType) {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !project.tasks.findByName(taskName)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def task = project.tasks.findByName(taskName)
        taskType.isInstance(task)

        where:
        taskName            | taskType
        "uvmVersion"        | DefaultTask
        "listInstallations" | UvmListInstallations
    }

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    def "doesn't apply anything on windows"() {
        given:
        System.setProperty("os.name", "windows")

        assert !project.plugins.hasPlugin(PLUGIN_NAME)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.tasks.size() == 0
    }
}
