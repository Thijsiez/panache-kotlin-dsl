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

import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class PanacheEntityBaseProcessorCompileTests : ProcessorCompileTestCommon() {

    @Test
    fun testInterface() {

        // Given
        val compilation = kotlinCompilation("NotAClass.kt", """
            import jakarta.persistence.Entity

            @Entity
            interface NotAClass
        """)

        // When
        val result = compilation.compile()

        // Then
        result.assertOk()
    }

    @Test
    fun testNotPanacheEntity() {

        // Given
        val compilation = kotlinCompilation("Department.kt", """
            import jakarta.persistence.Entity

            @Entity
            class Department
        """)

        // When
        val result = compilation.compile()

        // Then
        result.assertOk()
    }

    @Test
    fun testPanacheEntityBaseWithoutProperties() {

        // Given
        val compilation = kotlinCompilation("Employee.kt", """
            import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
            import jakarta.persistence.Entity

            @Entity
            class Employee : PanacheEntityBase
        """)

        // When
        val result = compilation.compile()

        // Then
        result.assertOk()
        compilation.assertFileGenerated("EmployeeColumns.kt")

        val employeeColumns = result.loadGeneratedClass("EmployeeColumns")
        employeeColumns.assertNumberOfProperties(0)
    }

    @Test
    fun testPanacheEntityWithTransientProperty() {

        // Given
        val compilation = kotlinCompilation("Product.kt", """
            import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
            import jakarta.persistence.Entity
            import jakarta.persistence.Transient

            @Entity
            class Product(

                @Transient
                var name: String

            ) : PanacheEntityBase
        """)

        // When
        val result = compilation.compile()

        // Then
        result.assertOk()
        compilation.assertFileGenerated("ProductColumns.kt")

        val productColumns = result.loadGeneratedClass("ProductColumns")
        productColumns.assertNumberOfProperties(0)
    }

    @Test
    fun testPanacheEntitiesWithJoinColumnAndMappedProperty() {

        // Given
        val compilation = compilation(
            kotlin("Post.kt", """
                import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
                import jakarta.persistence.Entity
                import jakarta.persistence.OneToMany

                @Entity
                class Post(

                    @OneToMany(mappedBy = "post")
                    val comments: MutableList<Comment> = mutableListOf()

                ) : PanacheEntity()
            """),
            kotlin("Comment.kt", """
                import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
                import jakarta.persistence.Entity
                import jakarta.persistence.JoinColumn
                import jakarta.persistence.ManyToOne

                @Entity
                class Comment(

                    @JoinColumn(name = "post_id")
                    @ManyToOne(optional = false)
                    val post: Post

                ) : PanacheEntity()
            """)
        )

        // When
        val result = compilation.compile()

        // Then
        result.assertOk()
        compilation.assertFileGenerated("PostColumns.kt")
        compilation.assertFileGenerated("CommentColumns.kt")

        val postColumns = result.loadGeneratedClass("PostColumns")
        postColumns.assertNumberOfProperties(1)
        postColumns.assertHasColumnOfType("id", Long::class)

        val commentColumns = result.loadGeneratedClass("CommentColumns")
        commentColumns.assertNumberOfProperties(2)
        commentColumns.assertHasColumnOfType("id", Long::class)
        commentColumns.assertHasPropertyOfType("post", "PostColumnsBase")
    }

    @Test
    fun testPanacheEntityWithColumnTypeAnnotation() {

        // Given
        val compilation = kotlinCompilation("User.kt", """
            import ch.icken.processor.ColumnType
            import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
            import jakarta.persistence.Entity

            @Entity
            class User(

                @ColumnType(CharSequence::class)
                var username: String

            ) : PanacheEntity()
        """)

        // When
        val result = compilation.compile()

        // Then
        result.assertOk()
        compilation.assertFileGenerated("UserColumns.kt")

        val userColumns = result.loadGeneratedClass("UserColumns")
        userColumns.assertNumberOfProperties(2)
        userColumns.assertHasColumnOfType("id", Long::class)
        userColumns.assertHasColumnOfType("username", CharSequence::class)
    }
}
