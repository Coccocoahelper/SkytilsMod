/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2023 Skytils
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net")
        maven("https://maven.minecraftforge.net/")
        maven("https://repo.essential.gg/repository/maven-releases/")
        maven("https://jitpack.io") {
            mavenContent {
                includeGroupAndSubgroups("com.github")
            }
        }
    }

    plugins {
        val kotlinVersion = "1.9.22"
        kotlin("jvm") version kotlinVersion apply false
        kotlin("plugin.serialization") version kotlinVersion apply false
        id("gg.essential.multi-version.root") version "0.4.1"
        id("gg.essential.loom") apply false
        id("gg.essential.defaults") apply false
    }
}

rootProject.name = "SkytilsMod"
rootProject.buildFileName = "root.gradle.kts"

include(":mod")
project(":mod").apply {
    projectDir = file("./mod")
    buildFileName = "root.gradle.kts"
}
listOf(
    "1.8.9-forge",
    "1.16.2-forge",
    "1.16.2-fabric",
    "1.20.4-fabric",
).forEach { version ->
    include(":mod:$version")
    project(":mod:$version").apply {
        projectDir = file("./mod/versions/$version")
        buildFileName = "../../build.gradle.kts"
    }
}
includeBuild("events")
includeBuild("hypixel-api/types")