plugins {
    id("java")
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.0.0-beta6"
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

group = "moe.ku6"
version = "0.0.1"

repositories {
    mavenCentral()
    // jitpack.io
    maven("https://jitpack.io")
}

dependencies {
    implementation("joda-time:joda-time:2.13.0")
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.jcommander:jcommander:2.0")
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.12.1")
    // https://mvnrepository.com/artifact/org.apache.ant/ant
    implementation("org.apache.ant:ant:1.10.15")
    // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
    implementation("org.apache.commons:commons-lang3:3.17.0")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:2.0.16")
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.5.16")
    // https://mvnrepository.com/artifact/org.fusesource.jansi/jansi
    implementation("org.fusesource.jansi:jansi:2.4.1")
    // https://mvnrepository.com/artifact/org.jline/jline
    implementation("org.jline:jline:3.29.0")
    // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // https://mvnrepository.com/artifact/com.googlecode.lanterna/lanterna
//    implementation("com.googlecode.lanterna:lanterna:3.1.3")
    // https://mvnrepository.com/artifact/net.java.dev.jna/jna
    implementation("net.java.dev.jna:jna:5.17.0")
    // https://mvnrepository.com/artifact/io.appium/java-client
    implementation("io.appium:java-client:9.4.0")
    // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // https://mvnrepository.com/artifact/dev.mobile/dadb
    implementation("dev.mobile:dadb:1.2.9")
    // https://mvnrepository.com/artifact/org.jfree/jfreesvg
    implementation("org.jfree:jfreesvg:3.4.3")

    implementation("moe.ku6:jsonwrapper:0.0.1")

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    testCompileOnly("org.projectlombok:lombok:1.18.36")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.36")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "moe.ku6.sekaimagic.SekaiMagic";
        attributes["Implementation-Version"] = project.version;
    }
}