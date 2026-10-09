plugins {
    id("com.refinedmods.refinedarchitect.common")
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
    version = modVersion
    common()
    testing()
}

base {
    archivesName.set("refinedflowanalytics-common")
}

dependencies {
    api(libs.apiguardian)
    api("com.refinedmods.refinedstorage:refinedstorage-common:$refinedstorageVersion")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
