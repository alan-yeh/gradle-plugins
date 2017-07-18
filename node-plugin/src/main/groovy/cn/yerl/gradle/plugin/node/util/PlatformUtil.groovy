package cn.yerl.gradle.plugin.node.util

/**
 * Created by alan on 2017/4/16.
 */
class PlatformUtil {
    static PlatformUtil INSTANCE = new PlatformUtil()

    final Properties props;
    PlatformUtil(){
        this.props = System.getProperties();
    }

    private String prop(String name){
        String value = this.props.getProperty(name)
        return value != null ? value : System.getProperty(name)
    }

    /**
     * 系统名
     */
    String getOSName(){
        String name = prop('os.name').toLowerCase()
        if (name.contains('windows')){
            return 'win'
        }

        if (name.contains('mac')){
            return 'darwin'
        }

        if (name.contains('linux')){
            return 'linux'
        }

        if (name.contains('freebsd')){
            return 'linux'
        }

        if (name.contains('sunos')){
            return 'sunos'
        }

        throw new IllegalArgumentException("Unsupported OS: $name")
    }

    /**
     * 系统架构
     */
    String getOSArch(){
        String arch = prop('os.arch').toLowerCase()
        if (arch.contains('64')){
            return 'x64'
        }

        if (arch == 'arm'){
            String systemArch = 'uname -m'.execute().text.trim()
            if (systemArch == 'armv8l'){
                return 'arm64'
            }else {
                return systemArch
            }
        }
        return 'x86'
    }
}
