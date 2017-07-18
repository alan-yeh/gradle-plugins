package cn.yerl.gradle.plugin.vue

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

/**
 * Vue Plugin
 * Created by alan on 2017/4/14.
 */
class VuePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        VueExtension profileExt = project.extensions.create(VueExtension.NAME, VueExtension)

        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);

        SourceSet mainSrc = javaConvention.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

        System.out.println("==== SourceSet: ${mainSrc.getClass()}")


        File file = new File("src/main/node");


        System.out.println("==== path: ${file.exists()}");
    }
}
