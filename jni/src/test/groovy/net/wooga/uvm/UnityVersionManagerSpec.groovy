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

package net.wooga.uvm

import spock.lang.Specification
import spock.lang.Unroll

class UnityVersionManagerSpec extends Specification {

    @Unroll("can call :#method on UnityVersionManager")
    def "unity version manager interface doesn't crash"() {
        when:
        (UnityVersionManager.class).invokeMethod(method, null)

        then:
        noExceptionThrown()

        where:
        method       | _
        "uvmVersion" | _
    }

    @Unroll
    def "uvmVersion is #expectedVersion"() {
        expect:
        UnityVersionManager.uvmVersion() == expectedVersion

        where:
        expectedVersion = "0.0.1"
    }
}
