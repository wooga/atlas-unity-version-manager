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
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import wooga.gradle.unity.UnityPlugin
import wooga.gradle.unity.UnityPluginExtension
import wooga.gradle.unity.batchMode.BuildTarget
import wooga.gradle.unity.tasks.internal.AbstractUnityProjectTask
import wooga.gradle.unity.tasks.internal.AbstractUnityTask
import wooga.gradle.unity.version.manager.internal.DefaultUnityVersionManagerExtension
import wooga.gradle.unity.version.manager.tasks.UvmCheckInstallation
import wooga.gradle.unity.version.manager.tasks.UvmListInstallations
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

        def extension = create_and_configure_extension(project)

        project.tasks.create("uvmVersion", UvmVersion) {
            uvmVersion.set(extension.uvmVersion)
        }

        project.tasks.create("listInstallations", UvmListInstallations)

        project.plugins.withType(UnityPlugin, new Action<UnityPlugin>() {
            @Override
            void execute(UnityPlugin plugin) {
                setupUnityHooks(project, extension)
            }
        })

        project.tasks.withType(UvmCheckInstallation, new Action<UvmCheckInstallation>() {
            @Override
            void execute(UvmCheckInstallation checkInstallation) {
                checkInstallation.unityVersion.set(extension.unityVersion)
                checkInstallation.autoSwitchUnityEditor.set(extension.autoSwitchUnityEditor)
                checkInstallation.autoInstallUnityEditor.set(extension.autoInstallUnityEditor)
                checkInstallation.unityInstallBaseDir.set(extension.unityInstallBaseDir)
            }
        })
    }

    protected static UnityVersionManagerExtension create_and_configure_extension(Project project) {
        def extension = project.extensions.create(UnityVersionManagerExtension, EXTENSION_NAME, DefaultUnityVersionManagerExtension, project)
        extension.unityProjectDir.set(project.layout.projectDirectory)
        extension.unityVersion.set(project.provider({
            String version = (project.properties[UnityVersionManagerConsts.UNITY_VERSION_OPTION]
                    ?: System.getenv()[UnityVersionManagerConsts.UNITY_VERSION_ENV_VAR]) as String

            if(!version) {
                version = UnityVersionManager.detectProjectVersion(extension.unityProjectDir.get().asFile)
            }

            version
        }))

        extension.unityInstallBaseDir.set(project.provider({
            String path = (project.properties[UnityVersionManagerConsts.UNITY_INSTALL_BASE_DIR_OPTION]
                    ?: System.getenv()[UnityVersionManagerConsts.UNITY_INSTALL_BASE_DIR_PATH_ENV_VAR]) as String
            if (!path) {
                path = new File(project.buildDir, "unity_installations").path
            }
            return project.layout.projectDirectory.dir(path)
        }))

        extension.autoSwitchUnityEditor.set(project.provider({
            String rawValue = (project.properties[UnityVersionManagerConsts.AUTO_SWITCH_UNITY_EDITOR_OPTION]
                    ?: System.getenv()[UnityVersionManagerConsts.AUTO_SWITCH_UNITY_EDITOR_PATH_ENV_VAR]) as String

            if (rawValue) {
                return (rawValue == "1" || rawValue.toLowerCase() == "yes" || rawValue.toLowerCase() == "y" || rawValue.toLowerCase() == "true")
            }

            false
        }))

        extension.autoInstallUnityEditor.set(project.provider({
            String rawValue = (project.properties[UnityVersionManagerConsts.AUTO_INSTALL_UNITY_EDITOR_OPTION]
                    ?: System.getenv()[UnityVersionManagerConsts.AUTO_INSTALL_UNITY_EDITOR_PATH_ENV_VAR]) as String

            if (rawValue) {
                return (rawValue == "1" || rawValue.toLowerCase() == "yes" || rawValue.toLowerCase() == "y" || rawValue.toLowerCase() == "true")
            }

            false
        }))



        extension
    }

    static Component buildTargetToComponent(BuildTarget buildTarget) {
        Component component
        switch (buildTarget) {
            case BuildTarget.ios:
                component = Component.ios
                break
            case BuildTarget.android:
                component = Component.android
                break
            case BuildTarget.webgl:
                component = Component.webGl
                break
            case BuildTarget.linux:
            case BuildTarget.linux64:
                component = Component.linux
                break
            case BuildTarget.win32:
            case BuildTarget.win64:
                component = Component.windows
                break
            default:
                component = null
        }
        component
    }

    static void setupUnityHooks(Project project, UnityVersionManagerExtension extension) {

        //set the unity project dir to the configured value in UnityPluginExtension
        UnityPluginExtension unity = project.extensions.getByType(UnityPluginExtension)
        extension.unityProjectDir.set(project.provider({
            def projectDir = project.layout.directoryProperty()
            projectDir.set(project.file(unity.projectPath))
            projectDir.get()
        }))

        project.tasks.withType(AbstractUnityProjectTask, new Action<AbstractUnityProjectTask>() {
            @Override
            void execute(AbstractUnityProjectTask unityTask) {
                def checkInstallation = project.tasks.maybeCreate("checkUnityInstallation", UvmCheckInstallation)
                checkInstallation.unityExtension.set(unity)
                checkInstallation.buildRequiredUnityComponents.set(project.provider {
                    def tasks = project.tasks.withType(AbstractUnityProjectTask)
                    tasks.collect { buildTargetToComponent(it.buildTarget) }.findAll {it != null}
                })
                project.tasks[UnityPlugin.ACTIVATE_TASK_NAME].mustRunAfter checkInstallation
                unityTask.dependsOn checkInstallation
            }
        })
    }
}
