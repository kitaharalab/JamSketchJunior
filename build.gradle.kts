import org.jetbrains.kotlin.konan.file.File

plugins {
    kotlin("jvm") version "2.0.20"
    application
    id("edu.sc.seis.launch4j") version "2.5.4"
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://artifacts.alfresco.com/nexus/content/repositories/public/") }

    // jahmm
    maven { url = uri("https://maven.scijava.org/content/groups/public/") }
}

application {
    // Define the main class for the application.
    mainClass = "jp.kthrlab.jamsketch.view.JamSketchKt"
//    mainClass = "jp.kthrlab.jamsketch.view.JamSketch"
}

dependencies {
//    implementation fileTree(dir: "libs", include: ["*.jar"], exclude: [])
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"), "exclude" to listOf<String>())))


    // cmx dependencies
    // https://mvnrepository.com/artifact/org.apache.commons/commons-math
    implementation("org.apache.commons:commons-math:2.2")
    // https://mvnrepository.com/artifact/org.apache.commons/commons-math3
    implementation("org.apache.commons:commons-math3:3.6.1")
    // https://mvnrepository.com/artifact/be.ac.ulg.montefiore.run.jahmm/jahmm
    //implementation("be.ac.ulg.montefiore.run.jahmm:jahmm:0.6.2")

    // https://mvnrepository.com/artifact/org.apache.ivy/ivy
    implementation("org.apache.ivy:ivy:2.4.0")

    // https://mvnrepository.com/artifact/nz.ac.waikato.cms.weka/weka-stable
    implementation("nz.ac.waikato.cms.weka:weka-stable:3.6.14")
    // https://mvnrepository.com/artifact/com.googlecode.javacpp/javacpp
    implementation("com.googlecode.javacpp:javacpp:0.7")
    // https://mvnrepository.com/artifact/com.googlecode.javacv/javacv
    implementation("com.googlecode.javacv:javacv:0.1")
    // https://mvnrepository.com/artifact/it.unimi.dsi/fastutil
    implementation("it.unimi.dsi:fastutil:8.5.6")
    //https://mvnrepository.com/artifact/javazoom/jlayer
    implementation("javazoom:jlayer:1.0.1")
    //https://mvnrepository.com/artifact/commons-logging/commons-logging
    implementation("commons-logging:commons-logging:1.2")

    // TensorFlow
    // https://mvnrepository.com/artifact/org.tensorflow/tensorflow-core-platform
    implementation("org.tensorflow:tensorflow-core-platform:1.0.0-rc.2")

    // added 20221212 yonamine yonamine blow 4 implementation
    implementation("xml-resolver:xml-resolver:1.2")

    // https://mvnrepository.com/artifact/xerces/xercesImpl
    implementation("xerces:xercesImpl:2.12.2")

    // https://mvnrepository.com/artifact/xalan/xalan
    implementation("xalan:xalan:2.7.2")

    // https://mvnrepository.com/artifact/xalan/serializer
    implementation("xalan:serializer:2.7.2")

    // https://mvnrepository.com/artifact/javax.websocket/javax.websocket-api
    compileOnly("javax.websocket:javax.websocket-api:1.1")

    implementation("org.glassfish.tyrus:tyrus-server:2.0.0")
    implementation("org.glassfish.tyrus:tyrus-container-grizzly-server:2.0.0")
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client:2.1.5")

    // Kotlin
    //// JSON
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")

}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.register<JavaExec>("runApp") {
    mainClass.set("jp.kthrlab.jamsketch.view.JamSketch")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("jp.kthrlab.jamsketch.view.JamSketch")
}

task("printEnv") {
    println(projectDir)
    println(sourceSets["main"].resources.srcDirs)
    println("sourceSets.main.output.asPath = ${sourceSets["main"].output.asPath}")
}

tasks.register<Jar>("jamsketchJar") {
    //Exclude the duplicate dependencies.
    duplicatesStrategy=DuplicatesStrategy.EXCLUDE

    //Specify the main class for manifest file.
    manifest {
        attributes["Main-Class"] = "jp.jamsketch.view.JamSketch"
    }

    //Include multiple dependencies.
    from({
        configurations.runtimeClasspath.map {
            if (it.asPath.File().isDirectory) it else zipTree(it)
        }
    })

    //include some file from projectDir to Jar file.
    from(projectDir) {
        //Specifying the files excluded from Jar file.
        exclude("*.java")
        exclude("*.groovy")
        exclude(".*")
        exclude("build*")
        exclude("*gradle*")
        exclude("settings*")
        exclude("*.txt")
        exclude("*.mid")
        exclude("bk")
        exclude("bin*")
    }
}

//Build exe file.
launch4j {
    headerType="gui"
    mainClassName = "JamSketch"
    outfile = "JamSketch.exe"
    // icon = "${projectDir}/src/main/resources/images/icon.ico"

    //Specify the jar task included in build.gradle.
    jarTask=tasks.getByName("jamsketchJar")
}
