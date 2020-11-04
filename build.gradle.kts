
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.math.BigDecimal.valueOf

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    idea
    `java-library`
//    checkstyle
    jacoco
    eclipse
    `maven-publish`
    id("com.google.protobuf") version "0.8.13"
}

repositories {
    maven("https://plugins.gradle.org/m2/")
}

defaultTasks("clean", "build")

project.group = "com.xxx"
project.version = file("version.txt").readText(Charsets.UTF_8).trim()

val Project.gprUsername: String? get() = this.properties["gprUsername"] as String?
val Project.gprPassword: String? get() = this.properties["gprPassword"] as String?

tasks.jar {
    enabled = false
}

allprojects {
    repositories {
        mavenCentral()
        jcenter()
        mavenLocal()
    }
}

object Versions {
//    const val checkstyle = "8.28"
    const val hamcrest = "2.2"
    const val mockito = "3.2.4"
    const val jacoco = "0.8.2"
    const val junit5 = "5.6.0"
    const val logback = "1.2.3"
    const val agrona = "1.3.0"
    const val potobuf = "3.13.0"
    const val grpc = "1.33.0"
    const val javaxAnnotation = "1.3.2"
    const val hdrHistogram = "2.1.12"
}

subprojects {

    apply(plugin = "idea")
//    apply(plugin = "checkstyle")
    apply(plugin = "jacoco")
    apply(plugin = "eclipse")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = project.group
    version = project.version

    dependencies {
        testImplementation("org.hamcrest", "hamcrest", Versions.hamcrest)
        testImplementation("org.mockito", "mockito-junit-jupiter", Versions.mockito)
        testImplementation("org.junit.jupiter", "junit-jupiter-api", Versions.junit5)
        testImplementation("org.junit.jupiter", "junit-jupiter-params", Versions.junit5)
        testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", Versions.junit5)

        implementation("ch.qos.logback", "logback-classic", Versions.logback)

    }

    tasks.jar {
        enabled = true
    }

//    checkstyle {
//        toolVersion = Versions.checkstyle
//        sourceSets = singletonList(project.sourceSets.main.get())
//    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
        finalizedBy("jacocoTestReport")
    }
    jacoco {
        toolVersion = Versions.jacoco
    }
    tasks.jacocoTestReport {
        reports {
            xml.isEnabled = true
        }
    }

    tasks.compileJava {
        options.encoding = "UTF-8"
        options.isDeprecation = true
    }
    tasks.compileTestJava {
        options.encoding = "UTF-8"
        options.isDeprecation = true
    }
    tasks.test {
        testLogging {
            showStandardStreams = true
            exceptionFormat = TestExceptionFormat.FULL
        }
        systemProperties(mapOf(
            // ADD COMMON SYSTEM PROPERTIES FOR TESTS HERE
            "exampleProperty" to "exampleValue"
        ))
        reports.html.isEnabled = false // Disable individual test reports
    }

    tasks.javadoc {
        title = "<h1>Price Service</h1>"
    }

    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/stankevichevg/last-price-service-grpc")
                credentials {
                    username = project.gprUsername
                    password = project.gprPassword
                }
            }
        }
        publications {
            create<MavenPublication>("LastPriceService") {
                from(components["java"])
                pom {
                    name.set("Last value price gRPC service")
                    description.set(
                        """
                            Last value price service.
                            
                            Service for keeping track of the last price for financial instruments.
                            Producers will use the service to publish prices and consumers will use it to obtain them.
                        """
                    )
                    url.set("https://github.com/stankevichevg/last-price-service-grpc.git")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("stiachevg")
                            name.set("Evgenii Stankevich")
                            email.set("stankevich.evg@gmail.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/stankevichevg/last-price-service-grpc.git")
                        developerConnection.set("scm:git:ssh://github.com/stankevichevg/last-price-service-grpc.git")
                        url.set("https://github.com/stankevichevg/last-price-service-grpc")
                    }
                }
            }
        }
    }
}

val jacocoAggregateMerge by tasks.creating(JacocoMerge::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    executionData(
        project(":service:core").buildDir.absolutePath + "/jacoco/test.exec"
    )
    dependsOn(
        ":service:core:test"
    )
}

@Suppress("UnstableApiUsage")
val jacocoAggregateReport by tasks.creating(JacocoReport::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    executionData(jacocoAggregateMerge.destinationFile)
    reports {
        xml.isEnabled = true
    }
    additionalClassDirs(files(subprojects.flatMap { project ->
        listOf("java", "kotlin").map { project.buildDir.path + "/classes/$it/main" }
    }))
    additionalSourceDirs(files(subprojects.flatMap { project ->
        listOf("java", "kotlin").map { project.file("src/main/$it").absolutePath }
    }))
    dependsOn(jacocoAggregateMerge)
}

tasks {
    jacocoTestCoverageVerification {
        executionData.setFrom(jacocoAggregateMerge.destinationFile)
        violationRules {
            rule {
                limit {
                    minimum = valueOf(0.3)
                }
            }
        }
        additionalClassDirs(files(subprojects.flatMap { project ->
            listOf("java", "kotlin").map { project.buildDir.path + "/classes/$it/main" }
        }))
        additionalSourceDirs(files(subprojects.flatMap { project ->
            listOf("java", "kotlin").map { project.file("src/main/$it").absolutePath }
        }))
        dependsOn(jacocoAggregateReport)
    }
    check {
        finalizedBy(jacocoTestCoverageVerification)
    }
}

project(":service:core") {

}

project(":service:transport") {

    apply(plugin = "java")
    apply(plugin = "com.google.protobuf")

    dependencies {
        api("javax.annotation", "javax.annotation-api", Versions.javaxAnnotation)
        api("io.grpc", "grpc-stub", Versions.grpc)
        api("io.grpc", "grpc-protobuf", Versions.grpc)
    }

    protobuf {
        protoc {
            artifact = "com.google.protobuf:protoc:" + Versions.potobuf
        }
        plugins {
            id("grpc") {
                artifact = "io.grpc:protoc-gen-grpc-java:" + Versions.grpc
            }
        }
        generateProtoTasks {
            ofSourceSet("main").forEach {
                it.plugins { id("grpc") }
            }
        }
    }
}

project(":service:server") {

    dependencies {
        implementation(project(":service:transport"))
        implementation(project(":service:core"))
        implementation("io.grpc", "grpc-stub", Versions.grpc)
        implementation("io.grpc", "grpc-protobuf", Versions.grpc)
        implementation("io.grpc", "grpc-netty-shaded", Versions.grpc)
    }

}

project(":service:client") {

    dependencies {
        api(project(":service:transport"))
        implementation("io.grpc", "grpc-netty-shaded", Versions.grpc)
    }

}

project(":performance-analysis") {

    dependencies {
        implementation(project(":service:client"))
        implementation(project(":service:server"))
        implementation("org.hdrhistogram", "HdrHistogram", Versions.hdrHistogram)
    }

}

tasks.register<Copy>("copyTestLogs") {
    from(".")
    include("**/build/test-output/**")
    include("**/*.log")
    exclude("build")
    into("build/test_logs")
    includeEmptyDirs = false
}