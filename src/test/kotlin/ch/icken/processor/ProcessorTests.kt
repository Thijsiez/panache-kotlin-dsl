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

import ch.icken.query.Column
import com.tschuchort.compiletesting.*
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

@OptIn(ExperimentalCompilerApi::class)
class ProcessorTests {

    //region PanacheEntityBaseProcessor
    @Test
    fun testInterface() {

        // Given
        val compilation = kotlinCompilation("NotAClass.kt",
            """
            import jakarta.persistence.Entity

            @Entity
            interface NotAClass
            """
        )

        // When
        val result = compilation.compile()

        // Then
        result.assertOk()
    }

    @Test
    fun testNotPanacheEntity() {

        // Given
        val compilation = kotlinCompilation("Department.kt",
            """
            import jakarta.persistence.Entity

            @Entity
            class Department
            """
        )

        // When
        val result = compilation.compile()

        // Then
        result.assertOk()
    }

    @Test
    fun testPanacheEntityBaseWithoutProperties() {

        // Given
        val compilation = kotlinCompilation("Employee.kt",
            """
            import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
            import jakarta.persistence.Entity

            @Entity
            class Employee : PanacheEntityBase
            """
        )

        // When
        val result = compilation.compile()

        // Then
        result.assertOk()
        compilation.assertFileGenerated("EmployeeColumns.kt")

        val employeeColumns = result.loadGeneratedClass("EmployeeColumns")
        employeeColumns.assertHasNoColumns()
    }

    @Test
    fun testPanacheEntityWithTransientProperty() {
        //TODO
    }

    @Test
    fun testPanacheEntityWithMappedProperty() {
        //TODO
    }

    @Test
    fun testPanacheEntity() {

        // Given
        val compilation = kotlinCompilation("User.kt",
            """
            import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
            import jakarta.persistence.Entity

            @Entity
            class User : PanacheEntity()
            """
        )

        // When
        val result = compilation.compile()

        // Then
        result.assertOk()
        compilation.assertFileGenerated("UserColumns.kt")

        val userColumns = result.loadGeneratedClass("UserColumns")
        userColumns.assertHasColumnOfType("id", Long::class)
    }

    @Test
    fun testPanacheEntityWithJoinColumn() {
        //TODO
    }

    @Test
    fun testPanacheEntityWithColumnTypeAnnotation() {
        //TODO
    }
    //endregion

    //region PanacheCompanionBaseProcessor
    //TODO
    //endregion

    //region Utils
    private fun kotlinResource(resourceName: String, sourceFileName: String = resourceName) =
        SourceFile.kotlin(sourceFileName, javaClass.classLoader.getResource(resourceName)?.readText() ?: "")

    private fun compilation(vararg source: SourceFile) = KotlinCompilation().apply {
        inheritClassPath = true
        kspWithCompilation = true
        sources = listOf(*source)
        symbolProcessorProviders = listOf(
            PanacheCompanionBaseProcessorProvider(),
            PanacheEntityBaseProcessorProvider()
        )
        verbose = false
    }

    private fun kotlinCompilation(sourceFileName: String, @Language("kotlin") contents: String) =
        compilation(SourceFile.kotlin(sourceFileName, contents))

    private fun KotlinCompilation.Result.assertOk() = assertEquals(KotlinCompilation.ExitCode.OK, exitCode)

    private fun KotlinCompilation.assertFileGenerated(fileName: String, outputDir: String = "generated") =
        assertTrue(kspSourcesDir.resolve("kotlin").resolve(outputDir).resolve(fileName).isFile)

    private fun KotlinCompilation.Result.loadGeneratedClass(className: String, outputPackage: String = "generated") =
        this.classLoader.loadClass("$outputPackage.$className").kotlin

    private fun KClass<*>.assertHasNoColumns() {
        val numberOfColumns = memberProperties
            .mapNotNull { it.returnType.classifier }
            .filterIsInstance<KClass<*>>()
            .count { it == Column::class }
        assertEquals(0, numberOfColumns)
    }

    private fun KClass<*>.assertHasColumnOfType(columnName: String, expectedType: KClass<*>) {
        val property = memberProperties.find { it.name == columnName }
        assertNotNull(property)

        val returnType = property!!.returnType
        returnType.classifier.let { classifier ->
            assertNotNull(classifier)
            assertTrue(classifier is KClass<*>)
            assertEquals(Column::class, classifier)
        }

        val arguments = returnType.arguments
        assertNotNull(arguments)
        assertEquals(1, arguments.size)

        val type = arguments[0].type
        assertNotNull(type)
        type!!.classifier.let { classifier ->
            assertNotNull(classifier)
            assertTrue(classifier is KClass<*>)
            assertEquals(expectedType, classifier)
        }
    }
    //endregion
}
