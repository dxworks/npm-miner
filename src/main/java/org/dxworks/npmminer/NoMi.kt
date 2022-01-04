package org.dxworks.npmminer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.nio.file.Path

val fileNames = listOf("package.json", "package-lock.json", "yarn.lock")
lateinit var baseFolder: File

fun main(args: Array<String>) {
    if (args.size != 1) {
        throw IllegalArgumentException("Bad arguments! Please provide only one argument, namely the path to the folder containing the source code.")
    }

    val baseFolderArg = args[0]

    baseFolder = File(baseFolderArg)

    println("Starting NoMi (Npm Miner)\n")
    println("Reading Files...")

    val packageFiles = baseFolder.walkTopDown()
        .filter { it.isFile }
        .filterNot { it.path.contains("node_modules") }
        .filter { fileNames.contains(it.name) }
        .toList()


    val npmProjects = getNpmProjects(packageFiles)
    val ilDeps: Map<String, List<InspectorLibDependency>> = npmProjects.map { proj ->
        proj.name to
                (proj.packageLockInfo?.let {
                    getAllDeps(it)
                } ?: proj.packageInfo?.let {
                    getAllDeps(it)
                } ?: emptyList())
    }.toMap()

    val regex = Regex("[<>=~^]")

    ilDeps.forEach {
        it.value.forEach { ilDep ->
            ilDep.version = ilDep.version?.let { version -> regex.replace(version,"") }
        }
    }

    val resultsPath = Path.of("results")
    resultsPath.toFile().mkdirs()

    val modelPath = Path.of("results", "npm-model.json")
    val inspectorLibPath = Path.of("results", "il-npm-deps.json")

    println("Writing Results...")



    println("Exporting Model to $modelPath")
    jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValue(modelPath.toFile(), npmProjects)

    println("Exporting Inspector Lib results to $inspectorLibPath")
    jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValue(inspectorLibPath.toFile(), ilDeps)

    println("\nNoMi (Npm Miner) finished successfully! Please view your results in the ./results directory")


}

fun getAllDeps(it: NpmPackageInfo): List<InspectorLibDependency> =
    it.dependencies.map { InspectorLibDependency(it.key, it.value) }

fun getAllDeps(it: NpmPackageLockInfo): List<InspectorLibDependency> {
    return it.dependencies.filterNot { it.value.dev }.flatMap { extractDepsRecursively(it.key, it.value) }
}

fun extractDepsRecursively(name: String, dependency: PackageLockDependency): List<InspectorLibDependency> {
    return dependency.dependencies.filterNot { it.value.dev }.flatMap { extractDepsRecursively(it.key, it.value) } + InspectorLibDependency(name, dependency.version)
}

data class NpmProject(
    val name: String,
    val packagePath: String,
    val packageLockPath: String,
    val yarnLockPath: String,
    val packageInfo: NpmPackageInfo?,
    val packageLockInfo: NpmPackageLockInfo?,
    val yarnLockContent: List<String>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NpmPackageInfo(
    val name: String? = null,
    val version: String? = null,
    val description: String? = null,
    val license: String? = null,
    val keywords: List<String> = emptyList(),
    val homepage: String? = null,
    //val bugs: Map<String, String> = emptyMap(),
    //val repository: Map<String, String> = emptyMap(),
    val dependencies: Map<String, String> = emptyMap(),
    val devDependencies: Map<String, String> = emptyMap()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NpmPackageLockInfo(
    val name: String? = null,
    val version: String? = null,
    val dependencies: Map<String, PackageLockDependency> = emptyMap()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PackageLockDependency(
    val version: String? = null,
    val resolved: String? = null,
    val dev: Boolean = false,
    val optional: Boolean = false,
    val requires: Map<String, String> = emptyMap(),
    val dependencies: Map<String, PackageLockDependency> = emptyMap()
)

fun getNpmProjects(packageFiles: List<File>): List<NpmProject> =
    packageFiles.groupBy { it.parentFile }.map { (folder, files) ->
        println("mining project $folder...")
        val projName = folder.relativeTo(baseFolder).toString()
        val packageFile = files.find { it.name == "package.json" }
        val packageLockFile = files.find { it.name == "package-lock.json" }
        val yarnLockFile = files.find { it.name == "yarn.lock" }

        try {
            NpmProject(projName, packageFile.toString(), packageLockFile.toString(), yarnLockFile.toString(),
                packageFile?.let {
                    jacksonObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                        .readValue<NpmPackageInfo>(it)
                },
                packageLockFile?.let {
                    jacksonObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                        .readValue<NpmPackageLockInfo>(it)
                },
                File(yarnLockFile.toString()).bufferedReader().readLines()
            )
        } catch (e: Exception) {
            NpmProject(projName, packageFile.toString(), packageLockFile.toString(), yarnLockFile.toString(),null, null, null)
        }
    }
