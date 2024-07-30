/*
 * Copyright 2023-2024 Thijs Koppen
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

import ch.icken.processor.ClassNames.StringClassName
import ch.icken.processor.GenerationOptions.ADD_GENERATED_ANNOTATION
import ch.icken.processor.PanacheEntityBaseProcessor.Companion.MAPPED_BY
import ch.icken.processor.QualifiedNames.HibernatePanacheEntityBase
import ch.icken.processor.QualifiedNames.JakartaPersistenceEntity
import ch.icken.processor.QualifiedNames.JakartaPersistenceJoinColumn
import ch.icken.processor.QualifiedNames.JakartaPersistenceTransient
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.toClassName
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class PanacheEntityBaseTests : TestCommon() {

    @MockK
    private lateinit var resolver: Resolver

    private val codeGenerator = mockk<CodeGenerator>(relaxed = true)
    private val processor = spyk(PanacheEntityBaseProcessor(
        options = mapOf(ADD_GENERATED_ANNOTATION to "false"),
        codeGenerator = codeGenerator,
        logger = mockk<KSPLogger>().also {
            every { it.info(any()) } just Runs
        }
    ))

    //region process
    @Test
    fun testProcessValidWithColumn() {

        // Given
        val columnProperty = mockk<KSPropertyDeclaration>()
        every { columnProperty.hasAnnotation(eq(JakartaPersistenceTransient)) } returns false
        every { columnProperty.annotation(any()).nonDefaultParameter(eq(MAPPED_BY)) } returns false

        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HibernatePanacheEntityBase)) } returns true
        every { validClass.getAllProperties() } returns sequenceOf(columnProperty)

        every { resolver.getSymbolsWithAnnotation(eq(JakartaPersistenceEntity)) } returns sequenceOf(validClass)

        every { processor.createColumnNamesObject(any(), any(), any()) } just Runs

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 1) {
            processor.createColumnNamesObject(
                ksClass = eq(validClass),
                ksProperties = withArg {
                    assertEquals(1, it.size)
                    assertEquals(columnProperty, it[0])
                },
                addGeneratedAnnotation = eq(false)
            )
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessValidWithTransientProperty() {

        // Given
        val columnProperty = mockk<KSPropertyDeclaration>()
        every { columnProperty.hasAnnotation(eq(JakartaPersistenceTransient)) } returns true

        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HibernatePanacheEntityBase)) } returns true
        every { validClass.getAllProperties() } returns sequenceOf(columnProperty)

        every { resolver.getSymbolsWithAnnotation(eq(JakartaPersistenceEntity)) } returns sequenceOf(validClass)

        every { processor.createColumnNamesObject(any(), any(), any()) } just Runs

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 1) {
            processor.createColumnNamesObject(
                ksClass = eq(validClass),
                ksProperties = match { it.isEmpty() },
                addGeneratedAnnotation = eq(false)
            )
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessValidWithoutProperties() {

        // Given
        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HibernatePanacheEntityBase)) } returns true
        every { validClass.getAllProperties() } returns emptySequence()

        every { resolver.getSymbolsWithAnnotation(eq(JakartaPersistenceEntity)) } returns sequenceOf(validClass)

        every { processor.createColumnNamesObject(any(), any(), any()) } just Runs

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 1) {
            processor.createColumnNamesObject(
                ksClass = eq(validClass),
                ksProperties = match { it.isEmpty() },
                addGeneratedAnnotation = eq(false)
            )
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessValidNotPanacheEntity() {

        // Given
        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HibernatePanacheEntityBase)) } returns false

        every { resolver.getSymbolsWithAnnotation(eq(JakartaPersistenceEntity)) } returns sequenceOf(validClass)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createColumnNamesObject(any(), any(), any())
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessValidNotClass() {

        // Given
        val validFunction = mockk<KSFunctionDeclaration>()
        every { validFunction.validate(any()) } returns true

        every { resolver.getSymbolsWithAnnotation(eq(JakartaPersistenceEntity)) } returns sequenceOf(validFunction)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createColumnNamesObject(any(), any(), any())
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessInvalid() {

        // Given
        val invalidClass = mockk<KSClassDeclaration>()
        every { invalidClass.validate(any()) } returns false

        every { resolver.getSymbolsWithAnnotation(eq(JakartaPersistenceEntity)) } returns sequenceOf(invalidClass)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createColumnNamesObject(any(), any(), any())
        }
        assertEquals(1, invalid.size)
    }
    //endregion

    //region createColumnNamesObject
    @Test
    fun testCreateColumnNamesObject() {

        // Given
        val packageName = "ch.icken.model"
        val classPackageName = mockk<KSName>()
        every { classPackageName.asString() } returns packageName

        val classSimpleName = mockk<KSName>()
        every { classSimpleName.asString() } returns "Employee"

        val ksClass = mockk<KSClassDeclaration>()
        every { ksClass.packageName } returns classPackageName
        every { ksClass.simpleName } returns classSimpleName

        val firstNameSimpleName = mockk<KSName>()
        every { firstNameSimpleName.asString() } returns "firstName"

        val firstNameType = mockk<KSType>()
        every { firstNameType.toClassName() } returns StringClassName
        every { firstNameType.isMarkedNullable } returns false

        val firstNameTypeReference = mockk<KSTypeReference>()
        every { firstNameTypeReference.resolve() } returns firstNameType

        val firstName = mockk<KSPropertyDeclaration>()
        every { firstName.simpleName } returns firstNameSimpleName
        every { firstName.hasAnnotation(eq(JakartaPersistenceJoinColumn)) } returns false
        every { firstName.type } returns firstNameTypeReference

        val departmentSimpleName = mockk<KSName>()
        every { departmentSimpleName.asString() } returns "department"

        val department = mockk<KSPropertyDeclaration>()
        every { department.simpleName } returns departmentSimpleName
        every { department.hasAnnotation(eq(JakartaPersistenceJoinColumn)) } returns true
        every { department.typeName } returns "Department"

        val ksProperties = listOf(firstName, department)

        // When
        processor.createColumnNamesObject(ksClass, ksProperties, true)

        // Then
        verify(exactly = 1) {
            codeGenerator.createNewFile(any(), any(), any())
        }
    }
    //endregion
}
