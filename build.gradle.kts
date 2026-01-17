plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "org.yax"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // 用 2023.1 版本编译，兼容性更好（IC = IntelliJ Community）
        create("IC", "2023.1")

        // 如果需要 Java 相关 API，可以加上
        bundledPlugin("com.intellij.java")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // 兼容 IDEA 2023.1 ~ 2025.*
            sinceBuild = "231"
            untilBuild = "259.*"
        }

        changeNotes = """
            Compatible with IntelliJ IDEA 2023.1 through 2025.x.
        """.trimIndent()
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}

