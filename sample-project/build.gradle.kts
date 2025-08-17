import org.gradle.api.tasks.JavaExec

plugins {
    java
    application
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.mybatis:mybatis:3.5.16")
    implementation("com.h2database:h2:2.2.224")
    implementation("org.slf4j:slf4j-simple:2.0.13")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("com.example.Main")
}

tasks.withType<JavaExec> {
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.test {
    useJUnitPlatform()
}
