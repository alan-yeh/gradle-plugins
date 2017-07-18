package cn.yerl.gradle.plugin.node;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Created by alan on 2017/4/16.
 */
class NodePlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        target.extensions.create(NodeExtension.NAME, NodeExtension)

    }
}
