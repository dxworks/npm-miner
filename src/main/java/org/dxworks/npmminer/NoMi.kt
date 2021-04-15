package org.dxworks.npmminer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.streams.toList

val fileNames = listOf("package.json", "package-lock.json")

@ExperimentalPathApi
fun main(args: Array<String>) {
    if (args.size != 1) {
        throw IllegalArgumentException("Bad arguments! Please provide only one argument, namely the path to the folder containing the source code.")
    }

    val baseFolderArg = args[0]

    val baseFolder = File(baseFolderArg)

    println("Starting NoMi (Npm Miner)\n")
    println("Reading Files...")

    val baseFolderPath = baseFolder.toPath()
    val packageFiles = Files.walk(baseFolderPath)
        .filter { it.isRegularFile() }
        .filter { fileNames.contains(it.name) }
        .toList()


    val npmProjects = listOf<NpmProject>()
    val ilDeps: List<InspectorLibDependency> = emptyList()


    val resultsPath = Path.of("results")
    resultsPath.toFile().mkdirs()

    val modelPath = Path.of("results", "npm-model.json")
    val inspectorLibPath = Path.of("results", "il-deps.json")

    println("Writing Results...")



    println("Exporting Model to $modelPath")
    jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValue(modelPath.toFile(), npmProjects)

    println("Exporting Inspector Lib results to $inspectorLibPath")
    jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValue(inspectorLibPath.toFile(), ilDeps)

    println("\nNoMi (Npm Miner) finished successfully! Please view your results in the ./results directory")


}

data class NpmProject(
    val name: String
    // TODO: add more fields
)