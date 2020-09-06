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

package wooga.gradle.unity.version.manager.tasks

import net.wooga.uvm.Component
import net.wooga.uvm.Installation
import net.wooga.uvm.UnityVersionManager
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import wooga.gradle.unity.UnityPluginExtension
import wooga.gradle.unity.version.manager.error.UvmInstallException

class UvmCheckInstallation extends DefaultTask {

    @Input
    @Optional
    final Property<String> unityVersion

    @Input
    @Optional
    final Property<UnityPluginExtension> unityExtension

    @Input
    final Property<Boolean> autoSwitchUnityEditor

    @Input
    final Property<Boolean> autoInstallUnityEditor

    @Input
    final DirectoryProperty unityInstallBaseDir

    @Input
    @Optional
    final SetProperty<Component> buildRequiredUnityComponents

    UvmCheckInstallation() {
        unityVersion = project.objects.property(String)
        unityExtension = project.objects.property(UnityPluginExtension)
        autoSwitchUnityEditor = project.objects.property(Boolean)
        autoInstallUnityEditor = project.objects.property(Boolean)
        unityInstallBaseDir = project.objects.directoryProperty()
        buildRequiredUnityComponents = project.objects.setProperty(Component)
    }

    @TaskAction
    void checkInstalltion() {
        if (!unityVersion.present) {
            logger.warn("no unity editor version found")
            return
        }

        def version = unityVersion.get()
        Installation installation = UnityVersionManager.locateUnityInstallation(version)

        boolean needInstall = false
        if (!installation) {
            logger.warn("unity version ${version} not installed")
            needInstall = true
        } else {
            def installedComponents = installation.components.toList().toSet()
            def requiredComponents = buildRequiredUnityComponents.get()
            if (!installedComponents.containsAll(requiredComponents)) {
                def missing = requiredComponents - installedComponents
                logger.warn("required components ${missing} not installed")
                needInstall = true
            }
        }

        if (autoSwitchUnityEditor.get() && autoInstallUnityEditor.get() && needInstall) {
            def destination = unityInstallBaseDir.file(version).get().asFile
            def components = buildRequiredUnityComponents.getOrElse(new HashSet<Component>()).toArray() as Component[]
            logger.info("install unity ${version}")
            if(components.size() > 0) {
                logger.info("with components: ")
                logger.info("${components.join("\n")}")
            }
            logger.info("to destination: ${destination}")

            installation = UnityVersionManager.installUnityEditor(version, destination, components)
            if(!installation) {
                logger.error("Unable to install requested unity version ${version}")
                throw new UvmInstallException("Unable to install requested unity version ${version}")
            }
        }

        if(!installation) {
            return
        }

        installation.location.setLastModified(new Date().getTime())

        if (!autoSwitchUnityEditor.get()) {
            logger.info("auto switch editor is turned off")
            return
        }

        if(unityExtension.present) {
            logger.info("update path to unity installtion ${installation.location}")
            def extension = unityExtension.get()
            extension.unityPath.set(installation.executable)
        }
    }
}

