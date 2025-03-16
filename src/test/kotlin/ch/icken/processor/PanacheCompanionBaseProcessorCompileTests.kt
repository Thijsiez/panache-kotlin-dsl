/*
 * Copyright 2024-2025 Thijs Koppen
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

import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class PanacheCompanionBaseProcessorCompileTests : ProcessorCompileTestCommon() {

    @Test
    fun testPanacheCompanion() {

        // Given
        val compilation = kotlinCompilation("Employee.kt", """
            import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
            import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
            import jakarta.persistence.Entity

            @Entity
            class Employee : PanacheEntity() {
                companion object : PanacheCompanion<Employee>
            }
        """)

        // When
        val result = compilation.compile()

        // Then
        result.assertOk()
        compilation.assertNumberOfFiles(2)
        compilation.assertHasFile("EmployeeColumns.kt")
        compilation.assertHasFile("EmployeeExtensions.kt")

        val employeeExtensions = result.loadClass("EmployeeExtensionsKt")
        employeeExtensions.assertNumberOfDeclaredMethods(22)
        employeeExtensions.assertHasDeclaredMethodWithName("andEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("andExpressionEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("andUpdateEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("countEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("deleteEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("findEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("findSortedEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("multipleEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("multipleSortedEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("orEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("orExpressionEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("orUpdateEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("singleEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("singleSafeEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("streamEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("streamSortedEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("updateAllEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("updateAllMultipleEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("updateEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("updateMultipleEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("whereEmployee")
        employeeExtensions.assertHasDeclaredMethodWithName("whereUpdateEmployee")
    }
}
