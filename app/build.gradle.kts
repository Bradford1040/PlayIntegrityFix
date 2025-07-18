plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "es.bradford1040.playintegrityfix"
    compileSdk = 35
    ndkVersion = "28.1.13356709"
    buildToolsVersion = "36.0.0"

    buildFeatures {
        prefab = true
    }

    packaging {
        jniLibs {
            excludes += "**/libdobby.so"
        }
    }

    defaultConfig {
        applicationId = "es.bradford1040.playintegrityfix"
        minSdk = 26
        targetSdk = 35
        versionCode = 19200
        versionName = "v19.2"
        multiDexEnabled = false

        externalNativeBuild {
            cmake {
                abiFilters(
                    "arm64-v8a",
                    "armeabi-v7a"
                )

                arguments(
                    "-DCMAKE_BUILD_TYPE=Release",
                    "-DANDROID_STL=c++_static",
                    "-DCMAKE_BUILD_PARALLEL_LEVEL=${Runtime.getRuntime().availableProcessors()}",
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                )

                val commonFlags = setOf(
                    "-fno-exceptions",
                    "-fno-rtti",
                    "-fvisibility=hidden",
                    "-fvisibility-inlines-hidden",
                    "-ffunction-sections",
                    "-fdata-sections",
                    "-w"
                )

                cFlags += "-std=c23"
                cFlags += commonFlags

                cppFlags += "-std=c++26"
                cppFlags += commonFlags
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            multiDexEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    implementation(libs.hiddenapibypass)
}

tasks.register("updateModuleProp") {
    val modulePropFile = project.rootDir.resolve("module/module.prop")
    val versionName = project.provider { android.defaultConfig.versionName }
    val versionCode = project.provider { android.defaultConfig.versionCode }

    inputs.property("versionName", versionName)
    inputs.property("versionCode", versionCode)
    outputs.file(modulePropFile)

    doLast {
        var content = modulePropFile.readText()

        content = content.replace(Regex("version=.*"), "version=${versionName.get()}")
        content = content.replace(Regex("versionCode=.*"), "versionCode=${versionCode.get()}")

        modulePropFile.writeText(content)
    }
}

tasks.register<Copy>("copyFiles") {
    dependsOn(
        "updateModuleProp",
        "minifyReleaseWithR8",
        "stripReleaseDebugSymbols"
    )

    val moduleDir = project.layout.projectDirectory.dir("../module")
    val dexFile = project.layout.buildDirectory.file("intermediates/dex/release/minifyReleaseWithR8/classes.dex")
    val soDir = project.layout.buildDirectory.dir("intermediates/stripped_native_libs/release/stripReleaseDebugSymbols/out/lib")

    from(dexFile) {
        rename { "classes.dex" }
    }

    from(soDir) {
        eachFile {
            val abi = file.parentFile.name
            path = "zygisk/$abi.so"
        }
        include("**/*.so")
    }

    into(moduleDir)
}

tasks.register<Zip>("zip") {
    dependsOn(tasks.named("copyFiles"))

    val versionName = project.provider { android.defaultConfig.versionName }
    archiveFileName.set(versionName.map { "PlayIntegrityFix_${it}.zip" })
    destinationDirectory.set(project.layout.projectDirectory.dir("../out"))

    from(project.layout.projectDirectory.dir("../module"))
}

afterEvaluate {
    tasks.named("assembleRelease").configure {
        finalizedBy(tasks.named("zip"))
    }
}

// This project is an application and should not be consumed as a library.
// This block prevents other modules from trying to depend on it, which is
// the cause of the "Multiple artifacts exist" build error.
configurations.all {
    if (name.endsWith("Elements")) {
        isCanBeConsumed = false
    }
}
