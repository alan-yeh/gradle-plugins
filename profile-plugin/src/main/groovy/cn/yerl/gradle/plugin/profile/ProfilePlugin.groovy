package cn.yerl.gradle.plugin.profile

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.tasks.DefaultSourceSet
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

import java.lang.reflect.Field
import java.text.SimpleDateFormat

/**
 * Profile Plugin
 * Created by Alan Yeh on 2016/11/9.
 */
class ProfilePlugin implements Plugin<Project> {
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private Project project;

    @Override
    void apply(Project project) {
        this.project = project;
        project.plugins.apply(JavaPlugin);

        ProfileExtension profileExt = project.extensions.create(ProfileExtension.NAME, ProfileExtension);

        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
        SourceSet profileSrc = javaConvention.sourceSets.create("profile");

        SourceSet mainSrc = javaConvention.sourceSets.getByName("main");
        mainSrc.resources.source(profileSrc.resources)
        mainSrc.java.source(profileSrc.java)

        // 生成文件
        project.afterEvaluate {
            buildClass(profileSrc, profileExt);
            buildProperties(profileSrc, profileExt);
        }
        configureExampleTask()
    }

    private void buildClass(SourceSet profileSrc, ProfileExtension profileExt) {
        String flavor = profileExt.flavor ?: "defaultProfile";

        if (profileExt.classPackage.isEmpty()){
            throw new GradleException("Profile: profile.classPackage is required.");
        }

        StringBuilder builder = new StringBuilder()
                .append("""/**
 * Created by ProfilePlugin on ${format.format(new Date())}
 * Automatically generated file. DO NOT MODIFY
 */
package ${profileExt.classPackage};

import java.io.InputStream;
import java.util.Properties;

public final class BuildProfile {
    private static Properties properties = new Properties();

    public static String get(String key){
        if (properties.isEmpty()){
            synchronized (BuildProfile.class){
                if (properties.isEmpty()){
                    try {
                        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("${profileExt.profileFileName}.properties");
                        properties.load(inputStream);
                    }catch (Exception ex){
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
        String result = properties.getProperty(key);
        if (result == null){
            throw new RuntimeException("BuildProfile: 获取不存在的属性[" + key + "]");
        }
        return result;
    }
""");
        builder.append("""    public static final String PROJECT_GROUP = "${project.group}";\r\n""");
        builder.append("""    public static final String PROJECT_NAME = "${project.name}";\r\n""");
        builder.append("""    public static final String PROJECT_VERSION = "${project.version}";\r\n""");
        builder.append("""    public static final String FLAVOR = "$flavor";\r\n""")

        // 获取默认的config
        Map<String, Object> defaultFields = new HashMap<String, Object>();

        profileExt.defaultProfile.classFields.each {
            if (defaultFields.containsKey(it[1])) {
                throw new GradleException("Profile: defaultProfile中重复classField[${it[1]}]");
            }
            defaultFields.put(it[1], it);
        }

        // 获取当前的config
        Map<String, Object> flavorFields = new HashMap<String, Object>();

        profileExt.flavorMap[flavor]?.classFields.each {
            if (flavorFields.containsKey(it[1])) {
                throw new GradleException("Profile: ${flavor}中重复classField[${it[0]}]");
            }

            defaultFields.remove(it[1]);
            flavorFields.put(it[1], it);
        }

        // 写到builder
        if (!defaultFields.isEmpty()) {
            builder.append("    // Fields from default profile.\r\n")
            defaultFields.sort{a, b -> a.key.compareTo(b.key) }.each {
                builder.append("    public static final ${it.value[0]} ${it.value[1]} = ${it.value[2]};\r\n");
            }
        }

        if (!flavorFields.isEmpty()) {
            builder.append("    // Fields from flavor: $flavor \r\n");
            flavorFields.sort{a, b -> a.key.compareTo(b.key)}.each {
                builder.append("    public static final ${it.value[0]} ${it.value[1]} = ${it.value[2]};\r\n");
            }
        }

        builder.append("}");


        // 写到文件
        File javaDir = profileSrc.java.srcDirs[0];
        if (!javaDir.exists()){
            javaDir.mkdirs();
        }
        clearDir(javaDir);

        File classDir = new File("${javaDir.path}.${profileExt.classPackage}".replaceAll("[.]", File.separator));
        classDir.mkdirs();

        FileOutputStream out = new FileOutputStream(new File(classDir.path + File.separator + "BuildProfile.java"));
        out.write(builder.toString().getBytes(profileExt.encoding));
        out.close();
    }

    private void buildProperties(SourceSet profileSrc, ProfileExtension profileExt) {
        String flavor = profileExt.flavor ?: "defaultProfile";

        StringBuilder builder = new StringBuilder()
                .append("""#
# Created by ProfilePlugin on ${format.format(new Date())}
# Automatically generated file. DO NOT MODIFY
#
""");
        builder.append("""PROJECT_GROUP = ${project.group}\r\n""");
        builder.append("""PROJECT_NAME = ${project.name}\r\n""");
        builder.append("""PROJECT_VERSION = ${project.version}\r\n""");
        builder.append("""VERSION_DATE = ${format.format(new Date())}\r\n""")
        builder.append("""FLAVOR = ${flavor}\r\n""");

        // 获取默认的config
        Map<String, Object> defaultFields = new HashMap<String, Object>();
        profileExt.defaultProfile.propertyFields.each {
            if (defaultFields.containsKey(it[0])) {
                throw new GradleException("Profile: defaultProfile中重复propertieField[${it[0]}]");
            }
            defaultFields.put(it[0], it[1]);
        }


        // 获取当前的config
        Map<String, Object> flavorFields = new HashMap<String, Object>();

        profileExt.flavorMap[flavor]?.propertyFields.each {
            if (flavorFields.containsKey(it[0])) {
                throw new GradleException("Profile: ${flavor}中重复propertieField[${it[0]}]");
            }

            defaultFields.remove(it[0]);
            flavorFields.put(it[0], it[1]);
        }


        // 写到builder
        if (!defaultFields.isEmpty()){
            builder.append("""# Fields from default profile.\r\n""")

            defaultFields.sort{a, b -> a.key.compareTo(b.key)}.each {
                builder.append("${it.key} = ${it.value}").append("\r\n");
            }
        }

        if (!flavorFields.isEmpty()){
            builder.append("""# Fields from flavor: $flavor \r\n""")

            flavorFields.sort{a, b -> a.key.compareTo(b.key)}.each {
                builder.append("${it.key} = ${it.value}").append("\r\n");
            }
        }

        // 写到文件
        File resourceDir = profileSrc.resources.srcDirs[0];
        if (!resourceDir.exists()){
            resourceDir.mkdirs();
        }
        clearDir(resourceDir);

        FileOutputStream out = new FileOutputStream(new File(resourceDir.path + File.separator + "${profileExt.profileFileName}.properties"));
        out.write(chinaToUnicode(builder.toString()).getBytes(profileExt.encoding));
        out.close()
    }

    private static void clearDir(File dir){
        dir.listFiles().each {
            if (it.isDirectory()){
                clearDir(it);
            }
            it.delete();
        }
    }

    static final String PROFILE_TASK_GROUP = "profile"

    private void configureExampleTask(){
        project.afterEvaluate{
            project.task("profileExample", type: ProfileExampleTask){
                group = PROFILE_TASK_GROUP
            }
        }
    }

    String chinaToUnicode(String str){
        StringBuilder result = new StringBuilder()
        for (int i = 0; i < str.length(); i++){
            int chr = (int) str.charAt(i)
            if(chr >= 19968 && chr <= 171941){ //汉字范围 \u4e00-\u9fa5 (中文)
                result.append("\\u").append(Integer.toHexString(chr))
            }else{
                result.append(str.charAt(i))
            }
        }
        return result
    }

}


