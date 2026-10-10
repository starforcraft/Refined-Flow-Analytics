plugins {
    id("com.refinedmods.refinedarchitect.fabric")
}

repositories {
    maven {
        name = "Refined Storage"
        url = uri("https://maven.creeperhost.net")
        content { includeGroup("com.refinedmods.refinedstorage") }
    }
}

val modVersion: String by project
val refinedstorageVersion: String by project

refinedarchitect {
    modId = "refinedflowanalytics"
    version = modVersion
    fabric()
}

base { archivesName.set("refinedflowanalytics-fabric") }

val commonJava by configurations.existing
val commonResources by configurations.existing

dependencies {
    api(libs.apiguardian)
    compileOnly(project(":common"))
    commonJava(project(path = ":common", configuration = "commonJava"))
    commonResources(project(path = ":common", configuration = "commonResources"))
    api("com.refinedmods.refinedstorage:refinedstorage-fabric:$refinedstorageVersion")
    implementation(libs.cloth.config)
    compileOnly(libs.modmenu)
}

repositories {
    maven { url = uri("https://maven.terraformersmc.com/") }
    maven { url = uri("https://maven.shedaniel.me/") }
}
