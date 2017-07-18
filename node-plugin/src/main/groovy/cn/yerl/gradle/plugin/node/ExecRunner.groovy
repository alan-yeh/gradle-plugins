package cn.yerl.gradle.plugin.node

import org.gradle.api.Project
import org.gradle.process.ExecResult

/**
 * Created by alan on 2017/4/16.
 */
class ExecRunner {
    final Project project

    Map<String, Object> environment = [:]

    ExecRunner(Project project){
        this.project = project
        this.environment << System.getenv()
    }

    final ExecResult run(String command, List<String> args){
        return this.project.exec {
            it.executable = command
            it.args = args
            it.environment = environment
        }
    }
}
