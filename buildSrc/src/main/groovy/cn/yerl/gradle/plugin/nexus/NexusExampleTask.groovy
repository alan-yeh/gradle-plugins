package cn.yerl.gradle.plugin.nexus;

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction;

/**
 * Created by alan on 2016/12/5.
 */
public class NexusExampleTask extends DefaultTask {
    @TaskAction
    def action(){
        description = "Example for nexus plugin configuration"
        println """
nexus {
    repository {
        username 'your_nexus_username'
        password 'your_nexus_password'
//        release 'your_nexus_release_repo_url'
//        snapshot 'your_nexus_snapshot_repo_url'
    }

    signatory {
        keyId 'your_GnuPG_key_id'
        password 'your_GnuPG_password'
//        secretKeyRingFile 'your_GnuPG_key_ring_file'
    }

    // upload sources and doc
//    archive {
//        sources true
//        doc true
//    }

    pom {
        name 'your_project_name'
        description 'your_project_description'
        url 'your_project_website'

        scm {
            url 'https://github.com/example-user/example-project'
            connection 'scm:https://github.com/example-user/example-project.git'
            developerConnection 'scm:git@github.com:example-user/example-project.git'
        }

        licenses {
            license {
                name 'The Apache Software License, Version 2.0'
                url 'http://www.apache.org/licenses/LICENSE-2.0.txt'
            }
        }

        developers {
            developer {
                name 'Your Name'
                email 'your@email.com'
            }
        }
    }
}
"""
    }
}
