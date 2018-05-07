package cn.yerl.gradle.plugin.nexus

/**
 * Created by Alan Yeh on 2016/12/4.
 */
class ArchiveContainer {
    Boolean sources = Boolean.TRUE
    Boolean doc = Boolean.TRUE

    def invokeMethod(String name, args) {
        if (this.hasProperty(name)){
            setProperty(name, args[0])
        }else {
            super.invokeMethod(name, args)
        }
    }
}
