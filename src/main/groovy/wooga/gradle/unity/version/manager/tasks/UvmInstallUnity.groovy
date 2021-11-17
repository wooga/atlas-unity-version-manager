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

package wooga.gradle.unity.version.manager.tasks

import net.wooga.uvm.Component
import net.wooga.uvm.UnityVersionManager
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.options.OptionValues
import wooga.gradle.unity.version.manager.error.UvmInstallException

class UvmInstallUnity extends DefaultTask {

    @Option(option = "version", description = "The version to install")
    void setUnityVersion(String version) {
        unityVersion.set(version)
    }

    @Input
    final Property<String> unityVersion

    @Option(option = "component", description = "A unity component to install")
    void setComponent(List<Component> components) {
        this.components.addAll(components)
    }

    @OptionValues("component")
    List<Component> getAvailableComponents() {
        new ArrayList<Component>(Arrays.asList(Component.values()))
    }

    @Optional
    @Input
    final SetProperty<Component> components

    @Option(option = "destination", description = "The location to install Unity to")
    void setInstallDestination(String path) {
        File dir = new File(path)
        dir.mkdirs()
        installDestination.set(dir)
    }

    @Optional
    @InputDirectory
    final DirectoryProperty installDestination

    UvmInstallUnity() {
        unityVersion = project.objects.property(String)
        components = project.objects.setProperty(Component)
        installDestination = project.objects.directoryProperty()

        outputs.upToDateWhen(new Spec<UvmInstallUnity>() {
            @Override
            boolean isSatisfiedBy(UvmInstallUnity task) {
                def installation = UnityVersionManager.locateUnityInstallation(task.unityVersion.get())
                return installation != null && installation.components.toList().containsAll(task.components.get())
            }
        })
    }

    @TaskAction
    protected void install() {
        def version = unityVersion.get()
        File destination = installDestination.asFile.getOrNull()
        Component[] components = components.get().toArray() as Component[]

        logger.info("install unity ${version}")
        if (components.size() > 0) {
            logger.info("with components: ")
            logger.info("${components.join("\n")}")
        }

        logger.info("to destination: ${destination}")
        if (!UnityVersionManager.installUnityEditor(version, destination, components)) {
            logger.error("Unable to install requested unity version ${version}")
            throw new UvmInstallException("Unable to install requested unity version ${version}")
        }
    }
}
