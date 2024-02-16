rootProject.name = "minecraft-library-loader"

include(":minecraft-library-loader-spigot")

pluginManagement {
    repositories {
        mavenLocal()
        maven {
            name = "aliyun-public"
            url = uri("https://maven.aliyun.com/repository/public/")
        }
        maven {
            name = "sonatype-public"
            url = uri("https://oss.sonatype.org/content/groups/public/")
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

buildscript {
    repositories {
        mavenLocal()
        maven {
            name = "aliyun-public"
            url = uri("https://maven.aliyun.com/repository/public/")
        }
        maven {
            name = "sonatype-public"
            url = uri("https://oss.sonatype.org/content/groups/public/")
        }
        mavenCentral()
    }
    dependencies {
        classpath("org.apache.commons:commons-lang3:3.14.0")
    }
}
