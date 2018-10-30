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

package net.wooga.uvm

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files

class InstallationSpec extends Specification {

    @Shared
    def buildDir

    def setup() {
        buildDir = new File('build/unityVersionManagerSpec')
        buildDir.mkdirs()
    }


    @Unroll("can call :#method on Installation")
    def "installation interface doesn't crash"() {
        given: "An Installation object"
        def installation = new Installation(null, null)

        when:
        installation.invokeMethod(method, *arguments)
        then:
        noExceptionThrown()

        where:
        method          | arguments
        "getComponents" | null
    }

    @Unroll("method :getComponents returns #valueMessage #reason")
    def "get components returns list of installed components"() {
        given: "install unity with specific components"
        def basedir = Files.createTempDirectory(buildDir.toPath(), "installationSpec_getComponents").toFile()
        basedir.deleteOnExit()
        def destination = new File(basedir, version)
        assert !destination.exists()
        def installation = UnityVersionManager.installUnityEditor(version, destination, components.toArray() as Component[])
        assert destination.exists()

        expect:
        installation.components == expectedComponents.toArray() as Component[]

        cleanup:
        destination.deleteDir()

        where:
        components                         | expectedComponents                 | reason
        [Component.android, Component.ios] | [Component.android, Component.ios] | "when components are installed"
        []                                 | []                                 | "when no components are installed"
        version = "2017.1.0f1"
        valueMessage = components.size() > 0 ? "list of installed components" : "empty list"
    }

}
