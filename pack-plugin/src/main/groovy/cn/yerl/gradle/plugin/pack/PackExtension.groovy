package cn.yerl.gradle.plugin.pack;

/**
 * Pack Extension
 * Created by Alan Yeh on 2016/12/18.
 */
public class PackExtension {
    def methodMissing(String name, args) {
        if (name == "to"){
            destDirs.addAll((Collection)args)
        }else {
            setProperty(name, args[0])
        }
    }

    String task;
    String template
    String templatePath = "/";
    def destDirs = ["build/packed"];
}
