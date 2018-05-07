package com.minstone.mobile

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolveDetails


class DependenciesPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.configurations.all {
            resolutionStrategy.eachDependency{ DependencyResolveDetails details ->
                if (details.requested.group == "com.minstone.mobile") {
                    details.useVersion "1.0.1-SNAPSHOT"
                    details.because "统一管理移动研发部版本"
                }

                if (details.requested.group == "net.lingala.zip4j"){
                    details.useVersion "1.3.1"
                }

                if (details.requested.group == "dom4j"){
                    details.useVersion "1.6.1"
                }

                if (details.requested.group == "com.jfinal" && details.requested.version == null){
                    details.useVersion "3.2"
                }


//                if (details.requested.group == "cglib"){
//                    details.useVersion "3.1"
//                }
            }
        }
    }
}
