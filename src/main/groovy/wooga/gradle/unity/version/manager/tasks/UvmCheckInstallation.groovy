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

import com.wooga.gradle.BaseSpec
import net.wooga.uvm.Component
import net.wooga.uvm.Installation
import net.wooga.uvm.UnityVersionManager
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import wooga.gradle.unity.UnityPluginExtension
import wooga.gradle.unity.version.manager.error.UvmInstallException

class UvmCheckInstallation extends DefaultTask implements BaseSpec {

    private final Property<String> unityVersion = objects.property(String)

    @Input
    @Optional
    Property<String> getUnityVersion() {
        unityVersion
    }

    void setUnityVersion(Provider<String> value) {
        unityVersion.set(value)
    }

    void setUnityVersion(String value) {
        unityVersion.set(value)
    }

    private final Provider<String> unityVersionWithoutRevision = unityVersion.map({ it.split(/( |\/)/).first() })

    @Internal
    Provider<String> getUnityVersionWithoutRevision() {
        unityVersionWithoutRevision
    }

    private final Property<UnityPluginExtension> unityExtension = objects.property(UnityPluginExtension)

    @Input
    @Optional
    Property<UnityPluginExtension> getUnityExtension() {
        unityExtension
    }

    void setUnityExtension(Provider<UnityPluginExtension> value) {
        unityExtension.set(value)
    }

    void setUnityExtension(UnityPluginExtension value) {
        unityExtension.set(value)
    }

    private final Property<Boolean> autoSwitchUnityEditor = objects.property(Boolean)

    @Input
    Property<Boolean> getAutoSwitchUnityEditor() {
        autoSwitchUnityEditor
    }

    void setAutoSwitchUnityEditor(Provider<Boolean> value) {
        autoSwitchUnityEditor.set(value)
    }

    void setAutoSwitchUnityEditor(Boolean value) {
        autoSwitchUnityEditor.set(value)
    }

    private final Property<Boolean> autoInstallUnityEditor = objects.property(Boolean)

    @Input
    Property<Boolean> getAutoInstallUnityEditor() {
        autoInstallUnityEditor
    }

    void setAutoInstallUnityEditor(Provider<Boolean> value) {
        autoInstallUnityEditor.set(value)
    }

    void setAutoInstallUnityEditor(Boolean value) {
        autoInstallUnityEditor.set(value)
    }


    private final DirectoryProperty unityInstallBaseDir = objects.directoryProperty()

    @Input
    DirectoryProperty getUnityInstallBaseDir() {
        unityInstallBaseDir
    }

    void setUnityInstallBaseDir(Provider<Directory> value) {
        unityInstallBaseDir.set(value)
    }

    void setUnityInstallBaseDir(File value) {
        unityInstallBaseDir.set(value)
    }

    private final SetProperty<Component> buildRequiredUnityComponents = objects.setProperty(Component)

    @Input
    @Optional
    SetProperty<Component> getBuildRequiredUnityComponents() {
        buildRequiredUnityComponents
    }

    void setBuildRequiredUnityComponents(Provider<Iterable<Component>> value) {
        buildRequiredUnityComponents.set(value)
    }

    void setBuildRequiredUnityComponents(Iterable<Component> value) {
        buildRequiredUnityComponents.set(value)
    }

    void buildRequiredUnityComponents(Component value) {
        buildRequiredUnityComponents.add(value)
    }

    void buildRequiredUnityComponents(Component... value) {
        buildRequiredUnityComponents.addAll(value)
    }

    void buildRequiredUnityComponents(Iterable<Component> value) {
        buildRequiredUnityComponents.addAll(value)
    }

    @TaskAction
    void checkInstalltion() {
        if (!unityVersion.present) {
            logger.warn("no unity editor version found")
            return
        }

        def version = unityVersion.get()
        // The version string may contain a version revision. Lets clean this out.
        def version_without_revision = unityVersionWithoutRevision.get()
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
            def destination = unityInstallBaseDir.file(version_without_revision).get().asFile
            def components = buildRequiredUnityComponents.getOrElse(new HashSet<Component>()).toArray() as Component[]
            logger.info("install unity ${version_without_revision}")
            if (components.size() > 0) {
                logger.info("with components: ")
                logger.info("${components.join("\n")}")
            }
            logger.info("to destination: ${destination}")

            installation = UnityVersionManager.installUnityEditor(version, destination, components)
            if (!installation) {
                logger.error("Unable to install requested unity version ${version_without_revision}")
                throw new UvmInstallException("Unable to install requested unity version ${version_without_revision}")
            }
        }

        if (!installation) {
            return
        }

        if (!autoSwitchUnityEditor.get()) {
            logger.info("auto switch editor is turned off")
            return
        }

        if (unityExtension.present) {
            logger.info("update path to unity installtion ${installation.location}")
            def extension = unityExtension.get()
            extension.unityPath.set(installation.executable)
        }
    }
}

