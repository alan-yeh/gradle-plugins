package cn.yerl.gradle.plugin.nexus

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.maven.MavenDeployment
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.plugins.signing.SigningPlugin


class NexusPlugin implements Plugin<Project> {
    private NexusPluginExtension extension;
    private Project project;

    @Override
    void apply(Project project) {
        this.project = project;

        project.plugins.apply(MavenPlugin)
        project.plugins.apply(SigningPlugin)

        extension = project.extensions.create('nexus', NexusPluginExtension.class, project);

        configureArchiveTasks()
        configureSigning()
        configurePom()
        configureUpload()
        configureExampleTask()
    }

    private boolean isRootProject(){
        project.rootProject == project;
    }
    private getUploadTaskPath(){
        this.isRootProject() ? ":$uploadTaskName" : "${project.path}:$uploadTaskName"
    }

    private getInstallTaskPath(){
        this.isRootProject() ? ":$MavenPlugin.INSTALL_TASK_NAME" : "$project.path:$MavenPlugin.INSTALL_TASK_NAME"
    }

    private getUploadTaskName(){
        "upload${Dependency.ARCHIVES_CONFIGURATION.capitalize()}"
    }


    private void configureSigning() {
        project.afterEvaluate {
            if(extension.signatoryContainer.sign) {
                project.signing {
                    required {
                        // Gradle allows project.version to be of type Object and always uses the toString() representation.
                        project.gradle.taskGraph.hasTask(this.uploadTaskPath) && !project.version.toString().endsWith('SNAPSHOT')
                    }

                    sign project.configurations[Dependency.ARCHIVES_CONFIGURATION]

                    project.gradle.taskGraph.whenReady {
                        if(project.signing.required) {
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
        SignatoryContainer signatory = extension.signatoryContainer;

        if (signatory.keyId.isEmpty()){
            throw new GradleException("A GnuPG key ID is required for signing. Please set up nexus.signing.keyId.")
        }

        project.ext.set("signing.keyId", signatory.keyId);


        File keyringFile =  signatory.secretKeyRingFile.isEmpty() ?
                new File(new File(System.getProperty('user.home'), '.gnupg'), 'secring.gpg') : project.file(signatory.secretKeyRingFile)

        if (!keyringFile.exists()){
            throw new GradleException("GnuPG secret key file $keyringFile not found. Please set up nexus.signing.secretKeyRingFile '/path/to/file.gpg'")
        }else {
            project.ext.set('signing.secretKeyRingFile', keyringFile.getPath())
        }

        printf "\nThis release $project.version will be signed with your GnuPG key ${signatory.keyId} in $keyringFile.\n"

        if (signatory.password.isEmpty()){
            throw new GradleException("Password for GnuPG key is required. Please set up nexus.signing.password 'password_for_key'");
        }else {
            project.ext.set('signing.password', signatory.password)
        }
    }

    private void signPomForUpload() {
        def uploadTasks = project.tasks.withType(Upload).matching { it.path == this.uploadTaskPath }

        uploadTasks.each { task ->
            task.repositories.mavenDeployer() {
                beforeDeployment { MavenDeployment deployment ->
                    project.signing.signPom(deployment)
                }
            }
        }
    }

    private void signInstallPom() {
        def installTasks = project.tasks.withType(Upload).matching { it.path == this.installTaskPath }

        installTasks.each { task ->
            task.repositories.mavenInstaller() {
                beforeDeployment { MavenDeployment deployment ->
                    project.signing.signPom(deployment)
                }
            }
        }
    }

    private void configurePom() {
        project.afterEvaluate {
            project.ext.poms = []
            Task installTask = project.tasks.findByPath(MavenPlugin.INSTALL_TASK_NAME)

            if (installTask) {
                project.ext.poms << installTask.repositories.mavenInstaller().pom
            }

            project.ext.poms << project.tasks.getByName(this.uploadTaskName).repositories.mavenDeployer().pom
        }
    }

    // 配置上传
    private void configureUpload() {
        project.afterEvaluate {
            project.tasks.getByName(this.uploadTaskName){
                group = NEXUS_TASK_GROUP
            }
            project.tasks.getByName(this.uploadTaskName).repositories.mavenDeployer() {
                project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
                    if(taskGraph.hasTask(this.uploadTaskPath)) {

                        RepositoryContainer repo = extension.repositoryContainer;

                        if (repo.username.isEmpty()){
                            throw new GradleException("Username for sonatype manager is Required. Please set up nexus.repository.username.");
                        }

                        if (repo.password.isEmpty()){
                            throw new GradleException("Password for sonatype manager is Required. Please set up nexus.repository.password.");
                        }

                        if (project.version.toString().endsWith('SNAPSHOT')){
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

    static final String NEXUS_TASK_GROUP = "nexus";

    // 配置上传的包
    private void configureArchiveTasks(){
        project.afterEvaluate{
            project.plugins.withType(JavaPlugin){

                if (extension.archiveContainer.sources){
                    Task task = project.task("javaSourcesJar", type: Jar) {
                        classifier = 'sources'
                        group = NEXUS_TASK_GROUP
                        description = 'Assembles a jar archive containing the main sources of this project.'
                        from project.sourceSets.main.allSource
                    }
                    project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, task);
                }

                if (extension.archiveContainer.doc){
                    Task task = project.task("javadocJar", type: Jar) {
                        classifier = 'javadoc'
                        group = NEXUS_TASK_GROUP
                        description = 'Assembles a jar archive containing the generated Javadoc API documentation of this project.'
                        from project.tasks.getByName(project.plugins.hasPlugin(GroovyPlugin) ? GroovyPlugin.GROOVYDOC_TASK_NAME : JavaPlugin.JAVADOC_TASK_NAME)
                    }

                    project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, task);
                }
            }

            if (project.hasProperty("android")){
                if (extension.archiveContainer.sources){
                    Task task = project.task("androidSourcesJar", type: Jar) {
                        classifier = 'sources'
                        group = NEXUS_TASK_GROUP
                        description = 'Assembles a jar archive containing the main sources of this project.'
                        from project.android.sourceSets.main.java.srcDirs
                    }
                    project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, task);
                }

                if (extension.archiveContainer.doc){
                    Task androidJavadocTask = project.task("androidJavadoc", type: Javadoc){
                        group = NEXUS_TASK_GROUP
                        description = 'Assembles a jar archive containing the generated Javadoc API documentation of this project.'
                        source = project.android.sourceSets.main.java.srcDirs
                        classpath += project.files(project.android.getBootClasspath().join(File.pathSeparator))
                    }

                    Task task = project.task("androidJavadocJar", type: Jar) {
                        classifier = 'javadoc'
                        group = NEXUS_TASK_GROUP
                        description = 'Assembles a jar archive containing the generated Javadoc API documentation of this project.'
                        from androidJavadocTask.destinationDir
                    }
                    task.dependsOn(androidJavadocTask)

                    project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, task);
                }
            }
        }
    }

    private void configureExampleTask(){
        project.afterEvaluate{
            project.task("nexusExample", type: NexusExampleTask){
                group = NEXUS_TASK_GROUP
            }
        }
    }
}