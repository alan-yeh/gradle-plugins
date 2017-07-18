package cn.yerl.gradle.plugin.node.task

import org.gradle.api.DefaultTask;

/**
 * Node Setup
 * Created by alan on 2017/4/16.
 */
class NodeSetupTask extends DefaultTask {
    final static String NAME = 'nodeSetup'

    NodeSetupTask(){
        this.group = 'node'
        this.description = 'Download and install a local node'

    }
}
