plugins {
    id("com.refinedmods.refinedarchitect.neoforge")
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
    neoForge()
    dataGeneration(project(":common"))
}

base { archivesName.set("refinedflowanalytics-neoforge") }

val commonJava by configurations.existing
val commonResources by configurations.existing

dependencies {
    api(libs.apiguardian)
    compileOnly(project(":common"))
    commonJava(project(path = ":common", configuration = "commonJava"))
    commonResources(project(path = ":common", configuration = "commonResources"))
    api("com.refinedmods.refinedstorage:refinedstorage-neoforge:$refinedstorageVersion")
}

neoForge {
    runs.named("data") {
        programArguments.addAll("--existing-mod", "refinedstorage")
    }
}
