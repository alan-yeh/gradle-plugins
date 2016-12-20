package cn.yerl.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by alan on 2016/11/1.
 */
class AndroidConfigPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.ext["compileSdkVersion"] = 23;
        project.ext["buildToolsVersion"] = "23.0.3"

        project.ext["minSdkVersion"] = 17;
        project.ext["targetSdkVersion"] = 23;

        project.ext["supportVersion"] = "23.0.3";
    }

}

