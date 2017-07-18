package cn.yerl.gradle.plugin.profile

import org.gradle.api.GradleException

/**
 * ProfileExtension
 * Created by Alan Yeh on 2016/11/9.
 */
class ProfileExtension {
    static String NAME = 'profile'

    // 处理多现场
    def methodMissing(String name, args) {
        if (args.length == 1 && !(args[0] instanceof Closure)){
            if (this.hasProperty(name)){
                this.setProperty(name, args[0]);
            }else {
                throw new GradleException("Profile: Not supported method $name");
            }
        }

        if (flavorMap.containsKey(name)){
            throw new GradleException("Profile: 重复定义flavor $name")
        }

        def profile;
        if (name == "defaultProfile"){
            profile = defaultProfile;
        }else {
            profile = new ProfileContainer()
            flavorMap.put(name, profile);
        }

        Closure configuration = args[0];
        configuration.delegate = profile;
        configuration();
    }

    //生成的文件编码
    String encoding = "UTF-8"

    // 选择偏好
    String flavor

    // 偏好配置
    Map<String, ProfileContainer> flavorMap = new HashMap<>()

    // 配置BuildProfile类
    String classPackage = ""

    // 配置properties的文件名
    String profileFileName = "buildprofile"

    // 默认profile
    ProfileContainer defaultProfile = new ProfileContainer()
}
