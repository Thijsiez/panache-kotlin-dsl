/*
 * Copyright 2024 Thijs Koppen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.icken.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class ProcessorTests {

    @Test
    fun testEmptyEntity() {

        // Given
        val compilation = kotlinCompilation("Employee.kt",
            """
            import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
            import jakarta.persistence.Entity

            @Entity
            class Employee : PanacheEntity()
            """
        )

        // When
        val result = compilation.compile()

        // Then
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        compilation.kspSourcesDir.walkTopDown().forEach { println(it.absolutePath) }
        //TODO validate generated file content
    }

    //region Utils
    private fun kotlinResource(resourceName: String, sourceFileName: String = resourceName) =
        SourceFile.kotlin(sourceFileName, javaClass.classLoader.getResource(resourceName)?.readText() ?: "")

    private fun compilation(vararg source: SourceFile) = KotlinCompilation().apply {
        inheritClassPath = true
        sources = listOf(*source)
        symbolProcessorProviders = listOf(
            PanacheCompanionBaseProcessorProvider(),
            PanacheEntityBaseProcessorProvider()
        )
        verbose = false
    }

    private fun kotlinCompilation(sourceFileName: String, @Language("kotlin") contents: String) =
        compilation(SourceFile.kotlin(sourceFileName, contents))

    private fun kotlinResourceCompilation(resourceName: String, sourceFileName: String = resourceName) =
        compilation(kotlinResource(resourceName, sourceFileName))
    //endregion
}
