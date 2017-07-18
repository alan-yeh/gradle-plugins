package cn.yerl.gradle.plugin.pack;

/**
 * Pack Extension
 * Created by Alan Yeh on 2016/12/18.
 */
class PackExtension {
    static String NAME = 'pack'

    def methodMissing(String name, args) {
        if (name == "targets"){
            destDirs.addAll((Collection)args)
        }else if (name == 'archives') {
            sources.addAll((Collection)args)
        }else{
                setProperty(name, args[0])
        }

    }

    String task;
    def sources = []
    String template
    String templatePath = "/";
    def destDirs = [];
}
