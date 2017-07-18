package cn.yerl.gradle.plugin.pack

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy;

/**
 * Pack Plugin
 * Created by Alan Yeh on 2016/12/18.
 */
class PackPlugin implements Plugin<Project> {
    static final String TASK_GROUP = "pack";

    @Override
    void apply(Project project) {
        PackExtension extension = project.extensions.create(PackExtension.NAME, PackExtension);

        project.afterEvaluate {
        }

        project.afterEvaluate{
            File file = project.file(extension.template);
            if (!file.exists()){
                throw new GradleException("Pack Plugin: Template[$file] is not exists.");
            }

            Task archivesTask = project.tasks.findByName(extension.task);
            if (archivesTask == null){
                throw new GradleException("Pack Plugin: Can't find task named $extension.task");
            }

            Task packTask = project.task("pack", type: DefaultTask)
            packTask.group = TASK_GROUP


            int i = 1;
            if (extension.destDirs.size() < 1){
                extension.destDirs.add("build/packed");
            }
            extension.destDirs.each {
                int index = i ++
                String to = "$it"
                // 将template复制到目标目录
                Copy copyTemplateTask = project.task("copyTemplate_${index}", type: Copy) {
                    from extension.template
                    into to
                }
                copyTemplateTask.description = "Copy template to path [$copyTemplateTask.destinationDir]"
                copyTemplateTask.group = TASK_GROUP
                copyTemplateTask.dependsOn(archivesTask)


                // 复制打包文件
                Copy copyArchivesTask = project.task("copyArchives_${index}", type: Copy) {
                    if (extension.sources.size() > 0){
                        extension.sources.each {
                            from it;
                        }
                    }else {
                        from archivesTask
                    }
                    into to + File.separator + extension.templatePath
                }
                copyArchivesTask.description = "Copy archives to path [${copyArchivesTask.destinationDir}]"

                copyArchivesTask.doLast{
                    println "Pack Plugin: Packed to path[${copyTemplateTask.destinationDir}]."

                }

                copyArchivesTask.group = TASK_GROUP
                copyArchivesTask.dependsOn(copyTemplateTask)
                copyArchivesTask.dependsOn(archivesTask)

                packTask.dependsOn(copyArchivesTask)
            }


            // Example
            Task exampleTask = project.task("packExample", type: PackExampleTask)
            exampleTask.group = TASK_GROUP
        }
    }
}
