package cn.yerl.gradle.plugin.pack

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy;

/**
 * Pack Plugin
 * Created by Alan Yeh on 2016/12/18.
 */
public class PackPlugin implements Plugin<Project> {
    static final String TASK_GROUP = "pack";

    @Override
    void apply(Project project) {
        PackExtension extension = project.extensions.create('pack', PackExtension);

        project.afterEvaluate{

            File file = project.file(extension.template);
            if (!file.exists()){
                throw new GradleException("Pack Plugin: Template[$file] is not exists.");
            }

            Task archivesTask = project.tasks.findByName(extension.task);
            if (archivesTask == null){
                throw new GradleException("Pack Plugin: Can't find task named $extension.task");
            } 


            // 将template复制到目标目录
            Copy copyTemplateTask = project.task("copyTemplate", type: Copy) {
                description = "Copy template to target path"
                from extension.template
                into extension.to
            }
            copyTemplateTask.group = TASK_GROUP


            // 复制打包文件
            Copy packTask = project.task("pack", type: Copy) {
                description = "Copy archives and template to target path"
                from archivesTask
                into extension.to + File.separator + extension.templatePath
            }
            packTask.doLast{
                println "Pack Plugin: Packed to path[${copyTemplateTask.destinationDir}]."
            }

            packTask.group = TASK_GROUP
            packTask.dependsOn(archivesTask)
            packTask.dependsOn(copyTemplateTask)


            // Example
            Task exampleTask = project.task("packExample", type: PackExampleTask)
            exampleTask.group = TASK_GROUP
        }
    }
}
