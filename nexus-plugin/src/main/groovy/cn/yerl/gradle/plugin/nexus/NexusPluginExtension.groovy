package cn.yerl.gradle.plugin.nexus

import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * NexusPluginExtension
 * Created by Alan Yeh on 2016/12/4.
 */
class NexusPluginExtension {
    static String NAME = 'nexus'

    final Project proj
    NexusPluginExtension(Project project){
        this.proj = project
    }

    def pom(Closure closure){
        proj.afterEvaluate {
            proj.poms.each {
                it.whenConfigured {
                    proj.configure(it){
                        project closure
                    }
                }
            }
        }
    }

    def currentContainer
    def methodMissing(String name, args) {

        if (currentContainer){
            currentContainer.invokeMethod(name, args)
            return
        }

        if (this.hasProperty(name + "Container") && args.length == 1 && (args[0] instanceof Closure)){
            Object container = this.getProperty(name + "Container")

            try {
                Closure configuration = args[0]
                configuration.delegate = container
                currentContainer = container
                configuration()
            } finally {
                currentContainer = null
            }
            return
        }

        throw new GradleException("Nexus: Not supported method $name")
    }

    // Nexus Repository Manager配置
    def repositoryContainer = new RepositoryContainer()

    // 签名配置
    def signatoryContainer = new SignatoryContainer()

    // 上传包配置
    def archiveContainer = new ArchiveContainer()
}


