package cn.yerl.gradle.plugin.profile

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Profile Example Task
 * Created by Alan Yeh on 2016/12/5.
 */
class ProfileExampleTask extends DefaultTask {

    @TaskAction
    def action(){
        description = "Example for profile plugin configuration"
        println """
profile {
    flavor 'flavor_1'
    classPackage 'your.project.package'
    defaultProfile {
        propertyField 'your_property_key1', 'your_property_value1'
        propertyField 'your_property_key2', 'your_property_value2'

        classField 'String', 'your_class_string_field', '"your_class_string_value"'
        classField 'int', 'your_class_int_field', '1'
    }
    flavor_1 {
        propertyField 'your_property_key2', 'your_property_value'

        classField 'int', 'your_class_int_field', '2'
    }
}
"""
    }
}
