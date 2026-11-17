import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.20"
    id("org.jetbrains.compose") version "1.7.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
}

// NewPipeExtractor-এর সংস্করণ: বিল্ড ব্যর্থ হলে https://github.com/TeamNewPipe/NewPipeExtractor/releases থেকে সর্বশেষ ট্যাগ বসান
val extractorVersion = "v0.26.5"

kotlin { jvmToolchain(17) }

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
    implementation("uk.co.caprica:vlcj:4.8.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")
    implementation("com.github.TeamNewPipe:NewPipeExtractor:$extractorVersion")
}

compose.desktop {
    application {
        mainClass = "com.safetube.app.MainKt"
        jvmArgs += listOf("-Xmx1g", "-Dfile.encoding=UTF-8")
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "MuslimTube"
            packageVersion = "1.0.0"
            description = "মুসলিম টিউব — ফিল্টারসহ ইউটিউব ক্লায়েন্ট"
            vendor = "Zakwan Ahmad"
            // CI এখানে VLC বসিয়ে দেয় (resources/windows-x64/vlc), যাতে আলাদা VLC ইনস্টল লাগে না
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            modules("java.sql", "jdk.unsupported", "java.naming", "java.management")
            windows {
                iconFile.set(project.file("packaging/icon.ico"))
                menuGroup = "মুসলিম টিউব"
                shortcut = true
                dirChooser = true
                upgradeUuid = "7d6c0b1e-5b0a-4f6e-9a55-3c2d6b1f9a11"
            }
            linux { iconFile.set(project.file("packaging/icon.png")) }
        }
    }
}
