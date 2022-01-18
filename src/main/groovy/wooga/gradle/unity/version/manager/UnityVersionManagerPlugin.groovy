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
import org.gradle.api.Transformer
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import wooga.gradle.unity.UnityPlugin
import wooga.gradle.unity.UnityPluginConventions
import wooga.gradle.unity.UnityPluginExtension
import wooga.gradle.unity.UnityTask
import wooga.gradle.unity.models.BuildTarget
import wooga.gradle.unity.version.manager.internal.DefaultUnityVersionManagerExtension
import wooga.gradle.unity.version.manager.tasks.UvmCheckInstallation
import wooga.gradle.unity.version.manager.tasks.UvmInstallUnity
import wooga.gradle.unity.version.manager.tasks.UvmListInstallations
import wooga.gradle.unity.version.manager.tasks.UvmVersion
import org.apache.maven.artifact.versioning.ArtifactVersion
import org.apache.maven.artifact.versioning.DefaultArtifactVersion

class UnityVersionManagerPlugin implements Plugin<Project> {

    static Logger logger = Logging.getLogger(UnityVersionManagerPlugin)

    static String EXTENSION_NAME = "uvm"
    static String GROUP = "unity version manager"
    static String LIST_INSTALLATIONS_TASK_NAME = "listInstallations"
    static String UVM_VERSION_TASK_NAME = "uvmVersion"
    static String CHECK_UNITY_INSTALLATION_TASK_NAME = "checkUnityInstallation"
    static String INSTALL_UNITY_TASK_NAME = "installUnity"

    @Override
    void apply(Project project) {
        def extension = create_and_configure_extension(project)

        String osName = System.getProperty("os.name").toLowerCase()
        if (!osName.contains("mac os") && !osName.contains("windows") && !osName.contains("linux")) {
            logger.warn("This plugin is only supported on macOS and windows and linux.")
            return
        }

        project.tasks.register(UVM_VERSION_TASK_NAME, UvmVersion, {
            group = GROUP
            description = "prints the version of the unity version manager"
            uvmVersion.convention(extension.uvmVersion)
        })

        project.tasks.register(LIST_INSTALLATIONS_TASK_NAME, UvmListInstallations, {
            group = GROUP
            description = "lists all installed unity versions"
        })

        project.tasks.register(INSTALL_UNITY_TASK_NAME, UvmInstallUnity, {
            group = GROUP
        })

        def checkUnityInstallation = project.tasks.register(CHECK_UNITY_INSTALLATION_TASK_NAME, UvmCheckInstallation, {
            buildRequiredUnityComponents.set(extension.buildRequiredUnityComponents)
            unityVersion.convention(extension.unityVersion)
            autoSwitchUnityEditor.convention(extension.autoSwitchUnityEditor)
            autoInstallUnityEditor.convention(extension.autoInstallUnityEditor)
            unityInstallBaseDir.convention(extension.unityInstallBaseDir)
        })

        project.plugins.withType(UnityPlugin).configureEach({

            UnityPluginExtension unity = project.extensions.getByType(UnityPluginExtension)
            extension.unityProjectDir.convention(unity.projectDirectory)
            checkUnityInstallation.configure({
                it.unityExtension.convention(unity)
            })

            project.tasks.withType(UnityTask).configureEach({
                project.tasks[UnityPlugin.Tasks.activateUnity.toString()].mustRunAfter checkUnityInstallation
                dependsOn checkUnityInstallation
            })
        })

        project.tasks.withType(UvmInstallUnity).configureEach({
            unityVersion.convention(extension.unityVersion)
            description = "installs a unity editor version and optional components"
        })
    }

    protected static UnityVersionManagerExtension create_and_configure_extension(Project project) {
        def extension = project.extensions.create(UnityVersionManagerExtension, EXTENSION_NAME, DefaultUnityVersionManagerExtension, project)
        extension.unityProjectDir.convention(project.layout.projectDirectory)
        extension.unityVersion.convention(UnityVersionManagerConventions.unityVersion.getStringValueProvider(project).orElse(project.provider({
            UnityVersionManager.detectProjectVersion(extension.unityProjectDir.get().asFile)
        })))

        extension.unityInstallBaseDir.convention(UnityVersionManagerConventions.unityInstallBaseDir.getDirectoryValueProvider(project).orElse(project.layout.buildDirectory.dir("unity_installations")))
        extension.autoSwitchUnityEditor.convention(UnityVersionManagerConventions.autoSwitchUnityEditor.getBooleanValueProvider(project))
        extension.autoInstallUnityEditor.convention(UnityVersionManagerConventions.autoInstallUnityEditor.getBooleanValueProvider(project))

        extension.buildRequiredUnityComponents.convention(project.provider({
            project.tasks.withType(UnityTask)
                    .findAll({
                        try {
                            return project.gradle.taskGraph.hasTask(it)
                        } catch (IllegalStateException ignored) {
                        }
                        true
                    })
                    .collect({
                        BuildTargetToComponents.buildTargetToComponents(it.buildTarget, extension.unityVersion)
                    })
        }).flatMap(new Transformer<Provider<List<Component>>, List<Provider<List<Component>>>>() {
            @Override
            Provider<List<Component>> transform(List<Provider<List<Component>>> providers) {
                project.provider({
                    providers.collectMany {it.getOrElse([]) }
                })
            }
        }))
        extension
    }

    @Deprecated
    static Component buildTargetToComponent(BuildTarget target) {
        buildTargetToComponent(target.toString())
    }

    @Deprecated
    static Component buildTargetToComponent(String target) {
        buildTargetToComponents(target, "2017.1.1f1").first()
    }

    static List<Component> buildTargetToComponents(String target, String versionString) {
        def version = new DefaultArtifactVersion(versionString.split(/f|p|b|a/).first().toString())
        def osName = System.getProperty("os.name").toLowerCase()
        def isWindows = osName.contains("windows")
        def isLinux = osName.contains("linux")
        def isMac = osName.contains("mac os")

        def components = []
        switch (target.toLowerCase()) {
            case "android":
                components.add(Component.android)
                break
            case "ios":
                components.add(Component.ios)
                break
            case "tvos":
                components.add(Component.tvOs)
                break
            case "webgl":
                components.add(Component.webGl)
                break
            case "lumin":
                components.add(Component.lumin)
                break
        }

        // The Component.<x>Mono doesn't exist on platform <x> as individual component, but is part of Editor component.
        // Component.<x>IL2CPP usually only exists on platform <x> itself, Linux is an exception.
        if (version.majorVersion >= 2019) {
            switch (target.toLowerCase()) {
                case "linux":
                case "linux64":
                case "linuxuniversal":
                    if (!isLinux) components.add(Component.linuxMono)
                    // all three platforms support linux I2CPP (cross-)compilation
                    if (isLinux || isWindows || isMac) components.(Component.linuxIL2CPP)
                    break
                case 'osxuniversal':
                    if (!isMac) components.add(Component.macMono)
                    if (isMac) components.add(Component.macIL2CPP)
                    break
                case "win32":
                case "win64":
                    if (!isWindows) components.add(Component.windowsMono)
                    if (isWindows) components.add(Component.windowsIL2CCP)
                    break
            }
        } else if (version.majorVersion == 2018) {
            switch (target.toLowerCase()) {
                case "linux":
                case "linux64":
                case "linuxuniversal":
                    if (!isLinux) components.add(Component.linux)
                    break
                case 'osxuniversal':
                    if (!isMac) components.add(Component.macMono)
                    if (isMac) components.add(Component.macIL2CPP)
                    break
                case "win32":
                case "win64":
                    if (!isWindows) components.add(Component.windowsMono)
                    if (isWindows) components.add(Component.windowsIL2CCP)
                    break
            }
        } else {
            switch (target.toLowerCase()) {
                case "linux":
                case "linux64":
                case "linuxuniversal":
                    if (!isLinux) components.add(Component.linux)
                    break
                case 'osxuniversal':
                    if (!isMac) components.add(Component.mac)
                    break
                case "win32":
                case "win64":
                    if (!isWindows) components.add(Component.windows)
                    break
            }
        }

        components
    }

    private static class BuildTargetToComponents implements Transformer<List<Component>, String> {
        private Provider<String> version

        static Provider<List<Component>> buildTargetToComponents(Provider<String> buildTarget, Provider<String> version) {
             buildTarget.map(new BuildTargetToComponents(version))
        }

        BuildTargetToComponents(Provider<String> version) {
            this.version = version
        }

        @Override
        List<Component> transform(String target) {
            buildTargetToComponents(target, version.get())
        }
    }
}
