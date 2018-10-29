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

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import wooga.gradle.unity.UnityPlugin
import wooga.gradle.unity.UnityPluginExtension
import wooga.gradle.unity.tasks.internal.AbstractUnityTask
import wooga.gradle.unity.version.manager.internal.DefaultUnityVersionManagerExtension
import wooga.gradle.unity.version.manager.tasks.UvmListInstallations
import wooga.gradle.unity.version.manager.tasks.UvmCheckInstallation
import wooga.gradle.unity.version.manager.tasks.UvmVersion

class UnityVersionManagerPlugin implements Plugin<Project> {

    static Logger logger = Logging.getLogger(UnityVersionManagerPlugin)

    static String EXTENSION_NAME = "uvm"

    @Override
    void apply(Project project) {
        String osName = System.getProperty("os.name").toLowerCase()
        if (!osName.contains("mac os")) {
            logger.warn("This plugin is only supported on macOS.")
            return
        }

        def extension = project.extensions.create(UnityVersionManagerExtension,EXTENSION_NAME, DefaultUnityVersionManagerExtension, project)

        project.tasks.create("uvmVersion", UvmVersion) {
            uvmVersion.set(extension.version)
        }

        project.tasks.create("listInstallations", UvmListInstallations)

        project.plugins.withType(UnityPlugin, new Action<UnityPlugin>() {
            @Override
            void execute(UnityPlugin plugin) {
                setupUnityHooks(project, extension)
            }
        })

    }

    static void setupUnityHooks(Project project, UnityVersionManagerExtension extension) {

        //set the unity project dir to the configured value in UnityPluginExtension
        UnityPluginExtension unity = project.extensions.getByType(UnityPluginExtension)
        extension.unityProjectDir.set(project.provider({
            def projectDir = project.layout.directoryProperty()
            projectDir.set(project.file(unity.projectPath))
            projectDir.get()
        }))

        extension.autoSwitchUnityEditor.set(project.provider({
            String rawValue = (project.properties[UnityVersionManagerConsts.AUTO_SWITCH_UNITY_EDITOR_OPTION]
                    ?: System.getenv()[UnityVersionManagerConsts.AUTO_SWITCH_UNITY_EDITOR_PATH_ENV_VAR]) as String

            if(rawValue) {
                return (rawValue == "1" || rawValue .toLowerCase()== "yes" || rawValue .toLowerCase()== "y" || rawValue .toLowerCase()== "true")
            }

            false
        }))

        project.tasks.withType(AbstractUnityTask, new Action<AbstractUnityTask>() {
            @Override
            void execute(AbstractUnityTask unityTask) {
                def checkInstallation = project.tasks.maybeCreate("checkUnityInstallation", UvmCheckInstallation)
                checkInstallation.projectUnityVersion.set(extension.projectVersion)
                checkInstallation.autoSwitchUnityEditor.set(extension.autoSwitchUnityEditor)
                checkInstallation.unityExtension.set(unity)
                project.tasks[UnityPlugin.ACTIVATE_TASK_NAME].mustRunAfter checkInstallation
                unityTask.dependsOn checkInstallation
            }
        })
    }
}
