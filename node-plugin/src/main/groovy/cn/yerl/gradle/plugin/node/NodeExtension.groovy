package cn.yerl.gradle.plugin.node

import org.gradle.api.Project

/**
 * Node Extension
 * Created by alan on 2017/4/16.
 */
class NodeExtension {
    final static String NAME = 'node'
    final static NodeExtension get(Project project){
        return project.extensions.getByType(NodeExtension)
    }

    File nodeDir
    File npmDir

    String version = '7.9.0'

    String npmCommand = 'npm'

    boolean embed = false

    NodeExtension(Project project){
        File cacheDir = new File(project.rootProject.projectDir, '.gradle')
        this.nodeDir = new File(cacheDir, 'node')
        this.npmDir = new File(cacheDir, 'npm')
    }
}
