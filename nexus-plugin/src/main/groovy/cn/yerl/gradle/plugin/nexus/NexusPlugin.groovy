package cn.yerl.gradle.plugin.nexus

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.maven.MavenDeployment
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.plugins.MavenRepositoryHandlerConvention
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.plugins.signing.SigningPlugin


class NexusPlugin implements Plugin<Project> {
    private NexusPluginExtension extension
    private Project proj

    @Override
    void apply(Project project) {
        this.proj = project

        proj.plugins.apply(MavenPlugin)
        proj.plugins.apply(SigningPlugin)

        extension = proj.extensions.create(NexusPluginExtension.NAME, NexusPluginExtension.class, proj)

        configureArchiveTasks()
        configureSigning()
        configurePom()
        configureUpload()
        configureInstall()
        configureExampleTask()
    }

    private boolean isRootProject(){
        proj.rootProject == proj
    }

    private getUploadTaskPath(){
        this.isRootProject() ? ":$UPLOAD_TASK_NAME" : "${proj.path}:$UPLOAD_TASK_NAME"
    }

    private getInstallTaskPath(){
        this.isRootProject() ? ":$INSTALL_TASK_NAME" : "$proj.path:$INSTALL_TASK_NAME"
    }


    private static final String UPLOAD_TASK_NAME = "uploadArchives"
    private static final String INSTALL_TASK_NAME = "installArchives"
    private static final String NEXUS_TASK_GROUP = "nexus"

    private void configureSigning() {
        proj.afterEvaluate {
            if(extension.signatoryContainer.sign) {
                proj.signing {
                    required {
                        // Gradle allows project.version to be of type Object and always uses the toString() representation.
                        proj.gradle.taskGraph.hasTask(this.uploadTaskPath) && !proj.version.toString().endsWith('SNAPSHOT')
                    }

                    sign proj.configurations[Dependency.ARCHIVES_CONFIGURATION]

                    proj.gradle.taskGraph.whenReady {
                        if(proj.signing.required) {
                            getPrivateKeyForSigning()
                        }

                        signPomForUpload()
                        signInstallPom()
                    }
                }
            }
        }
    }

    private void getPrivateKeyForSigning() {
        SignatoryContainer signatory = extension.signatoryContainer

        if (signatory.keyId.isEmpty()){
            throw new GradleException("签名(Signing)任务需要配置 GnuPG Key，请在Gradle中配置签名信息[nexus.signing.keyId]")
        }

        proj.ext.set("signing.keyId", signatory.keyId);


        File keyringFile =  signatory.secretKeyRingFile.isEmpty() ?
                new File(new File(System.getProperty('user.home'), '.gnupg'), 'secring.gpg') : proj.file(signatory.secretKeyRingFile)

        if (!keyringFile.exists()){
            throw new GradleException("没有找到GnuPG 密钥[$keyringFile]，请在Gradle中指定密钥地址[nexus.signing.secretKeyRingFile]")
        }else {
            proj.ext.set('signing.secretKeyRingFile', keyringFile.getPath())
        }

        printf("\n该版本[$proj.version]将使用 GnuPg 密钥[${signatory.keyId}, $keyringFile]进行签名")

        if (signatory.password.isEmpty()){
            throw new GradleException("请在Gradle中配置GnuPG的密钥密码[nexus.signing.password]")
        }else {
            proj.ext.set('signing.password', signatory.password)
        }
    }

    private void signPomForUpload() {
        def uploadTasks = proj.tasks.withType(Upload).matching { it.path == this.uploadTaskPath }

        uploadTasks.each { task ->
            task.repositories.mavenDeployer() {
                beforeDeployment { MavenDeployment deployment ->
                    proj.signing.signPom(deployment)
                }
            }
        }
    }

    private void signInstallPom() {
        def installTasks = proj.tasks.withType(Upload).matching { it.path == this.installTaskPath }

        installTasks.each { task ->
            task.repositories.mavenInstaller() {
                beforeDeployment { MavenDeployment deployment ->
                    proj.signing.signPom(deployment)
                }
            }
        }
    }

    private void configurePom() {
        proj.afterEvaluate {
            proj.ext.poms = []
            Task installTask = proj.tasks.findByPath(INSTALL_TASK_NAME)

            if (installTask) {
                proj.ext.poms << installTask.repositories.mavenInstaller().pom
            }

            proj.ext.poms << proj.tasks.getByName(UPLOAD_TASK_NAME).repositories.mavenDeployer().pom
        }
    }

    // 配置上传
    private void configureUpload() {
        proj.afterEvaluate {
            proj.tasks.getByName(UPLOAD_TASK_NAME){
                group = NEXUS_TASK_GROUP
                description = "将归档[Archives]上传到远程Nexus仓库"
            }
            proj.tasks.getByName(UPLOAD_TASK_NAME).repositories.mavenDeployer() {
                proj.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
                    if(taskGraph.hasTask(this.uploadTaskPath)) {

                        RepositoryContainer repo = extension.repositoryContainer

                        if (repo.username.isEmpty()){
                            throw new GradleException("请在Gradle中配置用户名[nexus.repository.username]")
                        }

                        if (repo.password.isEmpty()){
                            throw new GradleException("请在Gradle中配置密码[nexus.repository.password]")
                        }

                        if (proj.version.toString().endsWith('SNAPSHOT')){
                            repository(url: repo.snapshot){
                                authentication(userName: repo.username, password: repo.password)
                            }
                        }else {
                            repository(url: repo.release){
                                authentication(userName: repo.username, password: repo.password)
                            }
                        }
                    }
                }
            }
        }
    }

    private void configureInstall(){
        Upload installUpload = (Upload)proj.getTasks().create("installArchives", Upload.class)
        installUpload.group = NEXUS_TASK_GROUP

        Configuration configuration = proj.getConfigurations().getByName("archives")
        installUpload.setConfiguration(configuration)
        MavenRepositoryHandlerConvention repositories = (MavenRepositoryHandlerConvention)(new DslObject(installUpload.getRepositories())).getConvention().getPlugin(MavenRepositoryHandlerConvention.class)
        repositories.mavenInstaller()
        installUpload.setDescription("将归档[Artifacts]安装到本地Maven仓库")
    }
    


    // 配置上传的包
    private void configureArchiveTasks(){
        proj.afterEvaluate{
            proj.plugins.withType(JavaPlugin){

                if (extension.archiveContainer.sources){
                    Task task = proj.task("javaSourcesJar", type: Jar) {
                        classifier = 'sources'
                        group = NEXUS_TASK_GROUP
                        description = "打包此项目的源代码成Jar格式归档"
                        from proj.sourceSets.main.allSource
                    }
                    proj.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, task);
                }

                if (extension.archiveContainer.doc){
                    Task task = proj.task("javadocJar", type: Jar) {
                        classifier = 'javadoc'
                        group = NEXUS_TASK_GROUP
                        description = "生成此项目的JavaDoc接口文档，并打包成Jar格式归档"
                        from proj.tasks.getByName(proj.plugins.hasPlugin(GroovyPlugin) ? GroovyPlugin.GROOVYDOC_TASK_NAME : JavaPlugin.JAVADOC_TASK_NAME)
                    }

                    proj.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, task);
                }
            }

            if (proj.hasProperty("android")){
                if (extension.archiveContainer.sources){
                    Task task = proj.task("androidSourcesJar", type: Jar) {
                        classifier = 'sources'
                        group = NEXUS_TASK_GROUP
                        description = "打包此项目的源代码成Jar格式归档"
                        from proj.android.sourceSets.main.java.srcDirs
                    }

                    proj.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, task);
                }

                if (extension.archiveContainer.doc){
                    Task androidJavadocTask = proj.task("androidJavadoc", type: Javadoc){
                        group = NEXUS_TASK_GROUP
                        description = "生成此项目的JavaDoc接口文档"
                        source = proj.android.sourceSets.main.java.srcDirs
                        classpath += proj.files(proj.android.getBootClasspath().join(File.pathSeparator))
                        options.encoding = "UTF-8"
                    }

                    Task task = proj.task("androidJavadocJar", type: Jar) {
                        classifier = 'javadoc'
                        group = NEXUS_TASK_GROUP
                        description = "将此项目的JavaDoc接口文档打包成Jar格式归档"
                        from androidJavadocTask.destinationDir
                    }
                    task.dependsOn(androidJavadocTask)

                    proj.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, task);
                }
            }
        }
    }

    private void configureExampleTask(){
        proj.afterEvaluate{
            proj.task("nexusExample", type: NexusExampleTask){
                group = NEXUS_TASK_GROUP
            }
        }
    }
}