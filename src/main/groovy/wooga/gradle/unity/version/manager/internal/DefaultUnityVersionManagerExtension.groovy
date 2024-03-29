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
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import wooga.gradle.unity.version.manager.UnityVersionManagerExtension

class DefaultUnityVersionManagerExtension implements UnityVersionManagerExtension {

    final Provider<String> uvmVersion
    final Property<String> unityVersion
    final DirectoryProperty unityProjectDir
    final Property<Boolean> autoSwitchUnityEditor
    final Property<Boolean> autoInstallUnityEditor
    final DirectoryProperty unityInstallBaseDir
    final SetProperty<Component> buildRequiredUnityComponents

    DefaultUnityVersionManagerExtension(Project project) {
        uvmVersion = project.provider({ UnityVersionManager.uvmVersion() })
        unityProjectDir = project.objects.directoryProperty()
        unityVersion = project.objects.property(String)
        autoSwitchUnityEditor = project.objects.property(Boolean)
        autoInstallUnityEditor = project.objects.property(Boolean)
        unityInstallBaseDir = project.objects.directoryProperty()
        buildRequiredUnityComponents = project.objects.setProperty(Component)
    }
}
