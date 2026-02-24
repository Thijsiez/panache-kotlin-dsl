/*
 * Copyright 2026 Thijs Koppen
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

import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class MappedSuperclassProcessorCompileTests : ProcessorCompileTestCommon() {

    @Test
    fun testMappedSuperclass() {

        // Given
        val compilation = kotlinCompilation("NamedPanacheEntity.kt", """
            import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
            import jakarta.persistence.Column
            import jakarta.persistence.MappedSuperclass

            @MappedSuperclass
            open class NamedPanacheEntity(
            
                @Column(name = "name", nullable = false)
                var name: String
            
            ) : PanacheEntity()
        """)

        // When
        val result = compilation.compile()

        // Then
        result.assertOk()
        compilation.assertNumberOfFiles(1)
        compilation.assertHasFile("NamedPanacheEntityColumnsBase.kt")

        val namedPanacheEntityColumns = result.loadClass("NamedPanacheEntityColumnsBase")
        namedPanacheEntityColumns.assertNumberOfDeclaredMemberProperties(2)
        namedPanacheEntityColumns.assertDeclaresColumnOfType("id", Long::class)
        namedPanacheEntityColumns.assertDeclaresColumnOfType("name", String::class)
    }

    @Test
    fun testPanacheEntityWithMappedSuperclass() {

        // Given
        val compilation = compilation(
            kotlin("Base.kt", """
                import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
                import jakarta.persistence.Id
                import jakarta.persistence.MappedSuperclass

                @MappedSuperclass
                open class Base : PanacheEntityBase {

                    @Id
                    val id: Long? = null

                }
            """),
            kotlin("Derived.kt", """
                import jakarta.persistence.Entity

                @Entity
                class Derived(

                    val name: String

                ) : Base()
            """)
        )

        // When
        val result = compilation.compile()

        // Then
        result.assertOk()
        compilation.assertNumberOfFiles(2)
        compilation.assertHasFile("BaseColumnsBase.kt")
        compilation.assertHasFile("DerivedColumns.kt")

        val baseColumns = result.loadClass("BaseColumnsBase")
        baseColumns.assertNumberOfDeclaredMemberProperties(1)
        baseColumns.assertDeclaresColumnOfType("id", Long::class)

        val derivedColumns = result.loadClass("DerivedColumnsBase")
        derivedColumns.assertNumberOfDeclaredMemberProperties(1)
        derivedColumns.assertDeclaresColumnOfType("name", String::class)
    }
}
