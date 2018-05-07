package cn.yerl.gradle.plugin.nexus

/**
 * RepositoryContainer
 * Created by Alan Yeh on 2016/12/4.
 */
class RepositoryContainer {
    def invokeMethod(String name, args) {
        if (this.hasProperty(name)){
            setProperty(name, args[0])
        }else {
            super.invokeMethod(name, args)
        }
    }

    def username = ''
    def password = ''
    def release = 'https://oss.sonatype.org/service/local/staging/deploy/maven2'
    def snapshot = 'https://oss.sonatype.org/content/repositories/snapshots'
}
