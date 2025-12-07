/*
 * Copyright 2023-2025 Thijs Koppen
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

import ch.icken.processor.PanacheCompanionBaseProcessor.Companion.ListClassName
import ch.icken.processor.PanacheEntityBaseProcessor.Companion.JAKARTA_PERSISTENCE_JOIN_COLUMN
import ch.icken.processor.PanacheEntityBaseProcessor.Companion.JAKARTA_PERSISTENCE_TRANSIENT
import ch.icken.processor.PanacheEntityBaseProcessor.Companion.PARAM_NAME_MAPPED_BY
import ch.icken.processor.PanacheEntityBaseProcessor.Companion.PARAM_NAME_TYPE
import ch.icken.processor.PanacheEntityBaseProcessor.Companion.PROCESSOR_COLUMN_TYPE
import ch.icken.processor.PanacheEntityBaseProcessor.Companion.StringClassName
import ch.icken.processor.ProcessorCommon.Companion.HIBERNATE_PANACHE_ENTITY_BASE
import ch.icken.processor.ProcessorCommon.Companion.JAKARTA_PERSISTENCE_ENTITY
import ch.icken.processor.ProcessorCommon.Companion.OPTION_ADD_GENERATED_ANNOTATION
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
class PanacheEntityBaseProcessorMockTests : ProcessorMockTestCommon() {

    @MockK
    private lateinit var resolver: Resolver

    private val codeGenerator = mockk<CodeGenerator>(relaxed = true)
    private val processor = spyk(PanacheEntityBaseProcessor(
        options = mapOf(OPTION_ADD_GENERATED_ANNOTATION to "false"),
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
        every { columnProperty.hasBackingField } returns true
        every { columnProperty.hasAnnotation(eq(JAKARTA_PERSISTENCE_TRANSIENT)) } returns false
        every { columnProperty.annotation(any()).isParameterSet(eq(PARAM_NAME_MAPPED_BY)) } returns false

        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HIBERNATE_PANACHE_ENTITY_BASE)) } returns true
        every { validClass.getAllProperties() } returns sequenceOf(columnProperty)

        every { resolver.getSymbolsWithAnnotation(eq(JAKARTA_PERSISTENCE_ENTITY)) } returns sequenceOf(validClass)

        every { processor.createColumnsObject(any(), any()) } just Runs

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 1) {
            processor.createColumnsObject(
                ksClass = eq(validClass),
                ksProperties = withArg {
                    assertEquals(1, it.size)
                    assertEquals(columnProperty, it[0])
                }
            )
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessValidWithTransientProperty() {

        // Given
        val columnProperty = mockk<KSPropertyDeclaration>()
        every { columnProperty.hasBackingField } returns true
        every { columnProperty.hasAnnotation(eq(JAKARTA_PERSISTENCE_TRANSIENT)) } returns true

        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HIBERNATE_PANACHE_ENTITY_BASE)) } returns true
        every { validClass.getAllProperties() } returns sequenceOf(columnProperty)

        every { resolver.getSymbolsWithAnnotation(eq(JAKARTA_PERSISTENCE_ENTITY)) } returns sequenceOf(validClass)

        every { processor.createColumnsObject(any(), any()) } just Runs

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 1) {
            processor.createColumnsObject(
                ksClass = eq(validClass),
                ksProperties = match { it.isEmpty() }
            )
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessValidWithoutProperties() {

        // Given
        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HIBERNATE_PANACHE_ENTITY_BASE)) } returns true
        every { validClass.getAllProperties() } returns emptySequence()

        every { resolver.getSymbolsWithAnnotation(eq(JAKARTA_PERSISTENCE_ENTITY)) } returns sequenceOf(validClass)

        every { processor.createColumnsObject(any(), any()) } just Runs

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 1) {
            processor.createColumnsObject(
                ksClass = eq(validClass),
                ksProperties = match { it.isEmpty() }
            )
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessValidNotPanacheEntity() {

        // Given
        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HIBERNATE_PANACHE_ENTITY_BASE)) } returns false

        every { resolver.getSymbolsWithAnnotation(eq(JAKARTA_PERSISTENCE_ENTITY)) } returns sequenceOf(validClass)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createColumnsObject(any(), any())
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessValidNotClass() {

        // Given
        val validFunction = mockk<KSFunctionDeclaration>()
        every { validFunction.validate(any()) } returns true

        every { resolver.getSymbolsWithAnnotation(eq(JAKARTA_PERSISTENCE_ENTITY)) } returns sequenceOf(validFunction)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createColumnsObject(any(), any())
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessInvalid() {

        // Given
        val invalidClass = mockk<KSClassDeclaration>()
        every { invalidClass.validate(any()) } returns false

        every { resolver.getSymbolsWithAnnotation(eq(JAKARTA_PERSISTENCE_ENTITY)) } returns sequenceOf(invalidClass)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createColumnsObject(any(), any())
        }
        assertEquals(1, invalid.size)
    }
    //endregion

    //region createColumnsObject
    @Test
    fun testCreateColumnsObject() {

        // Given
        val packageName = "ch.icken.model"
        val classPackageName = mockk<KSName>()
        every { classPackageName.asString() } returns packageName

        val classSimpleName = mockk<KSName>()
        every { classSimpleName.asString() } returns "Employee"

        val ksClass = mockk<KSClassDeclaration>()
        every { ksClass.packageName } returns classPackageName
        every { ksClass.simpleName } returns classSimpleName

        //region firstName (not JoinColumn, not ColumnType)
        val firstNameSimpleName = mockk<KSName>()
        every { firstNameSimpleName.asString() } returns "firstName"

        val firstNameType = mockk<KSType>()
        every { firstNameType.toClassName() } returns StringClassName
        every { firstNameType.isMarkedNullable } returns false

        val firstNameTypeReference = mockk<KSTypeReference>()
        every { firstNameTypeReference.resolve() } returns firstNameType

        val firstName = mockk<KSPropertyDeclaration>()
        every { firstName.simpleName } returns firstNameSimpleName
        every { firstName.type } returns firstNameTypeReference
        every { firstName.hasAnnotation(eq(JAKARTA_PERSISTENCE_JOIN_COLUMN)) } returns false
        every { firstName.annotation(eq(PROCESSOR_COLUMN_TYPE)) } returns null
        //endregion

        //region lastName (not JoinColum, has ColumnType)
        val lastNameSimpleName = mockk<KSName>()
        every { lastNameSimpleName.asString() } returns "lastName"

        val lastNameType = mockk<KSType>()
        every { lastNameType.toClassName() } returns ListClassName
        every { lastNameType.isMarkedNullable } returns false

        val lastNameTypeReference = mockk<KSTypeReference>()
        every { lastNameTypeReference.resolve() } returns lastNameType

        val lastNameColumnTypeParameterName = mockk<KSName>()
        every { lastNameColumnTypeParameterName.asString() } returns PARAM_NAME_TYPE

        val lastNameColumnTypeArgument = mockk<KSType>()
        every { lastNameColumnTypeArgument.toClassName() } returns StringClassName

        val lastNameColumnTypeParameter = mockk<KSValueArgument>()
        every { lastNameColumnTypeParameter.name } returns lastNameColumnTypeParameterName
        every { lastNameColumnTypeParameter.value } returns lastNameColumnTypeArgument

        val lastNameColumnType = mockk<KSAnnotation>()
        every { lastNameColumnType.arguments } returns listOf(lastNameColumnTypeParameter)

        val lastName = mockk<KSPropertyDeclaration>()
        every { lastName.simpleName } returns lastNameSimpleName
        every { lastName.type } returns lastNameTypeReference
        every { lastName.hasAnnotation(eq(JAKARTA_PERSISTENCE_JOIN_COLUMN)) } returns false
        every { lastName.annotation(eq(PROCESSOR_COLUMN_TYPE)) } returns lastNameColumnType
        //endregion

        //region department (has JoinColumn)
        val departmentSimpleName = mockk<KSName>()
        every { departmentSimpleName.asString() } returns "department"

        val departmentTypeSimpleName = mockk<KSName>()
        every { departmentTypeSimpleName.asString() } returns "Department"

        val departmentTypeDeclaration = mockk<KSDeclaration>()
        every { departmentTypeDeclaration.packageName } returns classPackageName
        every { departmentTypeDeclaration.simpleName } returns departmentTypeSimpleName

        val departmentType = mockk<KSType>()
        every { departmentType.declaration } returns departmentTypeDeclaration

        val departmentTypeReference = mockk<KSTypeReference>()
        every { departmentTypeReference.resolve() } returns departmentType

        val department = mockk<KSPropertyDeclaration>()
        every { department.simpleName } returns departmentSimpleName
        every { department.type } returns departmentTypeReference
        every { department.hasAnnotation(eq(JAKARTA_PERSISTENCE_JOIN_COLUMN)) } returns true
        //endregion

        val ksProperties = listOf(firstName, lastName, department)

        // When
        processor.createColumnsObject(ksClass, ksProperties)

        // Then
        verify(exactly = 1) {
            codeGenerator.createNewFile(any(), any(), any())
        }
    }
    //endregion
}
