package cn.yerl.gradle.plugin.pack;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * Pack Example Task
 * Created by Alan Yeh on 2017/1/2.
 */
public class PackExampleTask extends DefaultTask {
    @TaskAction
    def action(){
        description = "Example for pack plugin configuration"
        println """
pack {
    task 'war'
    template 'pack'
    templatePath '/application'
    to '/Users/alan/Desktop/packed'
}
        """
    }
}
