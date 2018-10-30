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

package wooga.gradle.unity.version.manager

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

interface UnityVersionManagerExtension {

    /**
     * Returns the uvm core library version.
     */
    Provider<String> getUvmVersion()

    /**
     * The unity editor version used for this project.
     *
     * Sets the unity version used for this project. The default value will be fetched from the unity project settings.
     *
     * @see #getUnityProjectDir()
     */
    Property<String> getUnityVersion()

    /**
     * A {@code DirectoryProperty} pointing to a unity project.
     */
    DirectoryProperty getUnityProjectDir()

    /**
     * Enables autoswitch of unity installations.
     *
     * Only effects workspaces with {@code net.wooga.unity} plugins applied.
     * If set to {@code true}, the plugin will switch the Unity path for
     * {@code net.wooga.unity} to the located unity installation with the matching project version.
     *
     * @return {@code true} if autoswitching is enabled
     */
    Property<Boolean> getAutoSwitchUnityEditor()
}