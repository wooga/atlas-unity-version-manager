atlas-unity-version-manager
===========================

[![Gradle Plugin ID](https://img.shields.io/badge/gradle-net.wooga.unity--version--manager-brightgreen.svg?style=flat-square)](https://plugins.gradle.org/plugin/net.wooga.build-unity)
[![Build Status](https://wooga-shields.herokuapp.com/jenkins/s/https/atlas-jenkins.wooga.com/job/atlas-plugins/job/atlas-unity-version-manager/job/master.svg?style=flat-square)]()
[![Coveralls Status](https://img.shields.io/coveralls/wooga/atlas-unity-version-manager/master.svg?style=flat-square)](https://coveralls.io/github/wooga/atlas-unity-version-manager?branch=master)
[![Apache 2.0](https://img.shields.io/badge/license-Apache%202-blue.svg?style=flat-square)](https://raw.githubusercontent.com/wooga/atlas-unity-version-manager/master/LICENSE)
[![GitHub tag](https://img.shields.io/github/tag/wooga/atlas-unity-version-manager.svg?style=flat-square)]()
[![GitHub release](https://img.shields.io/github/release/wooga/atlas-unity-version-manager.svg?style=flat-square)]()

A gradle plugin which provides tasks and bindings to manage and download [unity] editor versions.
MacOS is the only supported platform at the moment.

This plugin is work in progress.

# Applying the plugin

**build.gradle**
```groovy
plugins {
    id 'net.wooga.unity-version-manager' version '0.1.0'
}
```


Development
===========

[Code of Conduct](docs/Code-of-conduct.md)

Gradle and Java Compatibility
=============================

Built with OpenJDK8

| Gradle Version  | Works  |
| :-------------: | :----: |
| < 5.1           | ![no]  |
| 5.1             | ![yes] |
| 5.2             | ![yes] |
| 5.3             | ![yes] |
| 5.4             | ![yes] |
| 5.5             | ![yes] |
| 5.6             | ![yes] |
| 5.6             | ![yes] |
| 6.0             | ![yes] |
| 6.1             | ![yes] |
| 6.2             | ![yes] |
| 6.3             | ![yes] |
| 6.4             | ![yes] |
| 6.5             | ![yes] |
| 6.6             | ![yes] |
| 6.6             | ![yes] |
| 6.7             | ![yes] |
| 6.8             | ![yes] |
| 6.9             | ![yes] |
| 7.0             | ![yes] |

LICENSE
=======

Copyright 2018 Wooga GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

<!-- Links -->
[unity]:                https://unity3d.com/ "Unity 3D"
[unity_cmd]:            https://docs.unity3d.com/Manual/CommandLineArguments.html
[gradle]:               https://gradle.org/ "Gradle"

[yes]:                  https://atlas-resources.wooga.com/icons/icon_check.svg "yes"
[no]:                   https://atlas-resources.wooga.com/icons/icon_uncheck.svg "no"

