package cn.yerl.gradle.plugin.profile

/**
 * ProfileContainer
 * Created by Alan Yeh on 2016/12/4.
 */
class ProfileContainer {
    def classFields = [];
    def propertyFields = [];

    // 添加BuildProfile类的属性
    def classField(type, field, value) {
        classFields << [type, field, value];
    }

    // 添加properties的属性
    def propertyField(key, value) {
        propertyFields << [key, value];
    }
}
