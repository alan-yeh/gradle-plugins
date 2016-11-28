package cn.yerl.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

import java.text.SimpleDateFormat

/**
 * Created by Alan Yeh on 2016/11/9.
 */
class ProfilePlugin implements Plugin<Project> {
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private Project project;

    @Override
    void apply(Project project) {
        this.project = project;
        project.plugins.apply(JavaPlugin);

        ProfileExtension profileExt = project.extensions.create("profile", ProfileExtension);

        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
        SourceSet profileSrc = javaConvention.sourceSets.create("profile");

        SourceSet main = javaConvention.sourceSets.getByName("main");
        main.resources.source(profileSrc.resources)
        main.java.source(profileSrc.java)

        project.afterEvaluate {
            buildClass(profileSrc, profileExt);
            buildProperties(profileSrc, profileExt);
        }
    }

    private void buildProperties(SourceSet profileSrc, ProfileExtension profileExt) {
        String flavor = profileExt._flavor;

        StringBuilder builder = new StringBuilder()
                .append("""#
# Created by ProfilePlugin on ${format.format(new Date())}
# Automatically generated file. DO NOT MODIFY
#
""");
        builder.append("""PROJECT_GROUP = ${project.group}\n""");
        builder.append("""PROJECT_VERSION = ${project.version}\n""");
        builder.append("""FLAVOR = ${flavor}\n""");

        // 获取默认的config
        HashMap defaultConfig = new HashMap();
        profileExt._defaultPropertyFields.each {
            if (defaultConfig.containsKey(it[0])) {
                throw new GradleException("Profile: defaultProfile中重复propertieField[${it[0]}]");
            }
            defaultConfig.put(it[0], it[1]);
        }


        // 获取当前的config
        HashMap currentConfig = new HashMap();

        def profileMap = profileExt._flavorPropertyFieldMap[flavor];
        profileMap?.each {
            if (currentConfig.containsKey(it[0])) {
                throw new GradleException("Profile: ${flavor}中重复propertieField[${it[0]}]");
            }

            defaultConfig.remove(it[0]);
            currentConfig.put(it[0], it[1]);
        }


        // 写到builder
        if (!defaultConfig.isEmpty()){
            builder.append("""# Fields from default profile.\n""")

            defaultConfig.each {
                builder.append("${it.key} = ${it.value}").append("\n");
            }
        }

        if (!currentConfig.isEmpty()){
            builder.append("""# Field from flavor: $flavor \n""")

            currentConfig.each {
                builder.append("${it.key} = ${it.value}").append("\n");
            }
        }


        // 写到文件
        File resourceDir = profileSrc.resources.srcDirs[0];
        if (!resourceDir.exists()){
            resourceDir.mkdirs();
        }
        clearDir(resourceDir);

        FileOutputStream out = new FileOutputStream(new File(resourceDir.path + File.separator + "${profileExt._profileFileName}.properties"));
        out.write(builder.toString().getBytes("UTF-8"));
        out.close();
    }

    private void buildClass(SourceSet profileSrc, ProfileExtension profileExt) {
        String flavor = profileExt._flavor;

        StringBuilder builder = new StringBuilder()
                .append("""/**
 * Created by ProfilePlugin on ${format.format(new Date())}
 * Automatically generated file. DO NOT MODIFY
 */
package ${profileExt._classPackage};

public class BuildProfile {
""");
        builder.append("""    public static final String PROJECT_GROUP = "${project.group}";\n""");
        builder.append("""    public static final String PROJECT_VERSION = "${project.version}";\n""");
        builder.append("""    public static final String FLAVOR = "$flavor";\n""")

        // 获取默认的config
        HashMap defaultConfig = new HashMap();

        profileExt._defaultClassFields.each {
            if (defaultConfig.containsKey(it[1])) {
                throw new GradleException("Profile: 重复配置buildConfigField[${it[1]}]");
            }
            defaultConfig.put(it[1], it);
        }

        println "$defaultConfig"

        // 获取当前的config
        HashMap currentConfig = new HashMap();

        def profileBuild = profileExt._flavorClassFieldMap[flavor];
        profileBuild?.each {
            defaultConfig.remove(it[1]);
            currentConfig.put(it[1], it);
        }

        // 写到builder
        if (!defaultConfig.isEmpty()) {
            builder.append("    // Fields from default profile.\n")
            defaultConfig.each {
                builder.append("    public static final ${it.value[0]} ${it.value[1]} = ${it.value[2]};\n");
            }
        }

        if (!currentConfig.isEmpty()) {
            builder.append("    // Field from flavor: $flavor \n");
            currentConfig.each {
                builder.append("    public static final ${it.value[0]} ${it.value[1]} = ${it.value[2]};\n");
            }
        }

        builder.append("}");


        // 写到文件
        File javaDir = profileSrc.java.srcDirs[0];
        if (!javaDir.exists()){
            javaDir.mkdirs();
        }
        clearDir(javaDir);

        File classDir = new File("${javaDir.path}.${profileExt._classPackage}".replaceAll("[.]", File.separator));
        classDir.mkdirs();

        FileOutputStream out = new FileOutputStream(new File(classDir.path + File.separator + "BuildProfile.java"));
        out.write(builder.toString().getBytes("UTF-8"));
        out.close();
    }

    private static void clearDir(File dir){
        dir.listFiles().each {
            if (it.isDirectory()){
                clearDir(it);
            }
            it.delete();
        }
    }
}


