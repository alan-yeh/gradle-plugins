package cn.yerl.gradle.plugin.nexus

/**
 * Created by Alan Yeh on 2016/12/4.
 */
class SignatoryContainer {
    def invokeMethod(String name, args) {
        if (this.hasProperty(name)){
            sign = Boolean.TRUE;
            setProperty(name, args[0])
        }else {
            super.invokeMethod(name, args);
        }
    }

    def sign = Boolean.FALSE;
    def keyId = '';
    def password = '';
    def secretKeyRingFile = '';
}
