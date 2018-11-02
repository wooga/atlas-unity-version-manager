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

import groovy.io.GroovyPrintStream
import net.wooga.uvm.UnityVersionManager
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * A utility task to list all installed Unity installations.
 */
class UvmListInstallations extends DefaultTask {

    private Boolean printPath

    @Option(option = "print-path", description = "Print the path to the installation")
    void setPrintPath(Boolean printPath) {
        this.printPath = printPath
    }

    @Console
    Boolean getPrintPath() {
        this.printPath
    }

    @Option(option = "output-path", description = "The output path. [default: stdout]")
    void setOutputFilePath(String path) {
        outputFile.set(new File(path))
    }

    @Optional
    @OutputFile
    final RegularFileProperty outputFile

    @Internal
    final Provider<PrintStream> outputPrintStream

    UvmListInstallations() {
        super()
        this.outputs.upToDateWhen({ false })
        this.outputFile = project.layout.fileProperty()
        this.outputPrintStream = project.provider({
            this.outputFile.map({
                new GroovyPrintStream(it.asFile) as PrintStream
            }).getOrElse(System.out)
        })
    }

    @TaskAction
    protected void list() {
        def installations = UnityVersionManager.listInstallations().toList()
        def out = outputPrintStream.get()
        if (installations.isEmpty()) {
            out.println("No versions installed")
        } else {
            out.println("installations:")
            installations.each {
                if (getPrintPath()) {
                    out.println("${it.version} - ${it.location.path}")
                } else {
                    out.println("${it.version}")
                }
            }
        }
    }
}
