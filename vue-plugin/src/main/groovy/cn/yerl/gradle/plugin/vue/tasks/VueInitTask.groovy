package cn.yerl.gradle.plugin.vue.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 *
 * Created by alan on 2017/4/15.
 */
class VueInitTask extends DefaultTask {

    @TaskAction
    def action(){
        description = "Init Vue "
    }
}
