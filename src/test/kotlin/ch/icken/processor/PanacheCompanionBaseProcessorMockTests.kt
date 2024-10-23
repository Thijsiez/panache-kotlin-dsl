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

import ch.icken.processor.ClassNames.LongClassName
import ch.icken.processor.GenerationOptions.ADD_GENERATED_ANNOTATION
import ch.icken.processor.QualifiedNames.HibernatePanacheCompanionBase
import ch.icken.processor.QualifiedNames.HibernatePanacheEntityBase
import ch.icken.processor.QualifiedNames.JakartaPersistenceEntity
import ch.icken.processor.QualifiedNames.JakartaPersistenceId
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class PanacheCompanionBaseProcessorMockTests : ProcessorMockTestCommon() {

    @MockK
    private lateinit var resolver: Resolver

    private val codeGenerator = mockk<CodeGenerator>(relaxed = true)
    private val processor = spyk(PanacheCompanionBaseProcessor(
        options = mapOf(ADD_GENERATED_ANNOTATION to "false"),
        codeGenerator = codeGenerator,
        logger = mockk<KSPLogger>().also {
            every { it.info(any()) } just Runs
        }
    ))

    //region process
    @Test
    fun testProcessValid() {

        // Given
        val companionObject = mockk<KSClassDeclaration>()
        every { companionObject.isCompanionObject } returns true
        every { companionObject.isSubclass(eq(HibernatePanacheCompanionBase)) } returns true

        val qualifiedPackageName = "ch.icken.model"
        val packageName = mockk<KSName>()
        every { packageName.asString() } returns qualifiedPackageName

        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HibernatePanacheEntityBase)) } returns true
        every { validClass.declarations } returns sequenceOf(companionObject)
        every { validClass.packageName } returns packageName

        every { resolver.getSymbolsWithAnnotation(eq(JakartaPersistenceEntity)) } returns sequenceOf(validClass)

        every { processor.createQueryBuilderExtensions(any(), any(), any()) } just Runs

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 1) {
            processor.createQueryBuilderExtensions(
                originalPackageName = eq(qualifiedPackageName),
                ksClasses = withArg {
                    assertEquals(1, it.size)
                    assertEquals(validClass, it[0])
                },
                addGeneratedAnnotation = eq(false)
            )
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessValidWithCompanionNotPanacheCompanion() {

        // Given
        val companionObject = mockk<KSClassDeclaration>()
        every { companionObject.isCompanionObject } returns true
        every { companionObject.isSubclass(eq(HibernatePanacheCompanionBase)) } returns false

        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HibernatePanacheEntityBase)) } returns true
        every { validClass.declarations } returns sequenceOf(companionObject)

        every { resolver.getSymbolsWithAnnotation(eq(JakartaPersistenceEntity)) } returns sequenceOf(validClass)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createQueryBuilderExtensions(any(), any(), any())
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessValidWithNotCompanion() {

        // Given
        val companionObject = mockk<KSClassDeclaration>()
        every { companionObject.isCompanionObject } returns false

        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HibernatePanacheEntityBase)) } returns true
        every { validClass.declarations } returns sequenceOf(companionObject)

        every { resolver.getSymbolsWithAnnotation(eq(JakartaPersistenceEntity)) } returns sequenceOf(validClass)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createQueryBuilderExtensions(any(), any(), any())
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessValidWithNotClass() {

        // Given
        val function = mockk<KSFunctionDeclaration>()

        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HibernatePanacheEntityBase)) } returns true
        every { validClass.declarations } returns sequenceOf(function)

        every { resolver.getSymbolsWithAnnotation(eq(JakartaPersistenceEntity)) } returns sequenceOf(validClass)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createQueryBuilderExtensions(any(), any(), any())
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessValidWithoutCompanion() {

        // Given
        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HibernatePanacheEntityBase)) } returns true
        every { validClass.declarations } returns emptySequence()

        every { resolver.getSymbolsWithAnnotation(eq(JakartaPersistenceEntity)) } returns sequenceOf(validClass)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createQueryBuilderExtensions(any(), any(), any())
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
            processor.createQueryBuilderExtensions(any(), any(), any())
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessNotClass() {

        // Given
        val validFunction = mockk<KSFunctionDeclaration>()
        every { validFunction.validate(any()) } returns true

        every { resolver.getSymbolsWithAnnotation(eq(JakartaPersistenceEntity)) } returns sequenceOf(validFunction)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createQueryBuilderExtensions(any(), any(), any())
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessInvalid() {

        // Given
        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns false

        every { resolver.getSymbolsWithAnnotation(eq(JakartaPersistenceEntity)) } returns sequenceOf(validClass)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createQueryBuilderExtensions(any(), any(), any())
        }
        assertEquals(1, invalid.size)
    }
    //endregion

    //region createQueryBuilderExtensions
    @Test
    fun testCreateQueryBuilderExtensions() {

        // Given
        val packageName = "ch.icken.model"
        val simpleName = "Employee"
        val className = ClassName(packageName, simpleName)

        val classSimpleName = mockk<KSName>()
        every { classSimpleName.asString() } returns simpleName

        val idPropertyType = mockk<KSType>()
        every { idPropertyType.toClassName() } returns LongClassName

        val idPropertyTypeReference = mockk<KSTypeReference>()
        every { idPropertyTypeReference.resolve() } returns idPropertyType

        val idProperty = mockk<KSPropertyDeclaration>()
        every { idProperty.hasAnnotation(eq(JakartaPersistenceId)) } returns true
        every { idProperty.type } returns idPropertyTypeReference

        val ksClass = mockk<KSClassDeclaration>()
        every { ksClass.toClassName() } returns className
        every { ksClass.simpleName } returns classSimpleName
        every { ksClass.getAllProperties() } returns sequenceOf(idProperty)

        val ksClasses = listOf(ksClass)

        // When
        processor.createQueryBuilderExtensions(packageName, ksClasses, false)

        // Then
        verify(exactly = 1) {
            codeGenerator.createNewFile(any(), any(), any())
        }
    }
    //endregion
}
