package cn.yerl.gradle.plugin

import org.gradle.api.GradleException

/**
 *
 * Created by Alan Yeh on 2016/11/9.
 */
class ProfileExtension {
    // 默认配置
    protected def _defaultClassFields = [];
    protected def _defaultPropertyFields = [];

    // 现场配置
    protected def _flavorClassFieldMap = new HashMap();
    protected def _flavorPropertyFieldMap = new HashMap();

    protected def _tempClassFields = [];
    protected def _tempPropertyFields = [];

    private boolean _isTemp = false;

    // 处理多现场
    def methodMissing(String name, args) {
        if (!(args.length == 1 && (args[0] instanceof Closure))){
            throw new GradleException("Profile: Not supported method $name");
        }

        if (_flavorPropertyFieldMap.containsKey(name)){
            throw new GradleException("Profile: 重复定义flavor $name")
        }

        _tempClassFields = [];
        _tempPropertyFields = [];
        _isTemp = true;
        if (args.length > 0) {
            args[0]()
        }
        _isTemp = false;

        _flavorClassFieldMap.put(name, _tempClassFields);
        _flavorPropertyFieldMap.put(name, _tempPropertyFields);
    }

    // 选择现场
    protected def _flavor;
    def flavor(String flavor) {
        _flavor = flavor
    }

    // 配置BuildProfile类
    protected def _classPackage;
    def classPackage(name) {
        _classPackage = name;
    }

    // 配置properties的文件名
    protected def _profileFileName = "buildprofile"
    def propertiesFileName(name){
        _profileFileName = name;
    }

    // 配置默认profile
    def defaultProfile(block) {
        block();
    }

    // 添加BuildProfile类的属性
    def classField(type, field, value) {
        if (_isTemp) {
            _tempClassFields << [type, field, value];
        } else {
            _defaultClassFields << [type, field, value];
            println "default:type: $type field:$field value:$value"
        }
    }

    // 添加properties的属性
    def propertyField(key, value) {
        if (_isTemp) {
            _tempPropertyFields << [key, value];
        } else {
            _defaultPropertyFields << [key, value];
        }
    }
}
