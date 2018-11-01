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

package wooga.gradle.unity.version.manager.internal

import net.wooga.uvm.Component
import net.wooga.uvm.UnityVersionManager
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import wooga.gradle.unity.tasks.internal.AbstractUnityProjectTask
import wooga.gradle.unity.version.manager.UnityVersionManagerExtension
import wooga.gradle.unity.version.manager.UnityVersionManagerPlugin

class DefaultUnityVersionManagerExtension implements UnityVersionManagerExtension {

    static Logger logger = Logging.getLogger(DefaultUnityVersionManagerExtension)

    final Provider<String> uvmVersion
    final Property<String> unityVersion
    final DirectoryProperty unityProjectDir
    final Property<Boolean> autoSwitchUnityEditor
    final Property<Boolean> autoInstallUnityEditor
    final DirectoryProperty unityInstallBaseDir
    final Provider<Set<Component>> buildRequiredUnityComponentsProvider

    DefaultUnityVersionManagerExtension(Project project) {
        uvmVersion = project.provider({ UnityVersionManager.uvmVersion() })
        unityProjectDir = project.layout.directoryProperty()
        unityVersion = project.objects.property(String)
        autoSwitchUnityEditor = project.objects.property(Boolean)
        autoInstallUnityEditor = project.objects.property(Boolean)
        unityInstallBaseDir = project.layout.directoryProperty()
        buildRequiredUnityComponentsProvider = project.provider {
            def tasks = project.tasks.withType(AbstractUnityProjectTask)

            tasks.findAll {
                try {
                    return project.gradle.taskGraph.hasTask(it)
                } catch (IllegalStateException ignored) {
                    logger.warn("try to filter required build components to early. Filter not applied")
                }
                true
            }.collect {
                UnityVersionManagerPlugin.buildTargetToComponent(it.buildTarget)
            }.findAll {
                it != null
            }.toSet()
        }

    }
}
