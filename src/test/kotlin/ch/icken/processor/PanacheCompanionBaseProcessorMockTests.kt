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

import ch.icken.processor.PanacheCompanionBaseProcessor.Companion.HIBERNATE_PANACHE_COMPANION_BASE
import ch.icken.processor.PanacheCompanionBaseProcessor.Companion.LongClassName
import ch.icken.processor.ProcessorCommon.Companion.HIBERNATE_PANACHE_ENTITY_BASE
import ch.icken.processor.ProcessorCommon.Companion.JAKARTA_PERSISTENCE_ENTITY
import ch.icken.processor.ProcessorCommon.Companion.OPTION_ADD_GENERATED_ANNOTATION
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
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
        options = mapOf(OPTION_ADD_GENERATED_ANNOTATION to "false"),
        codeGenerator = codeGenerator,
        logger = mockk<KSPLogger>().also {
            every { it.info(any()) } just Runs
        }
    ))

    //region process
    @Test
    fun testProcessValid() {

        // Given
        val idTypeName = mockk<TypeName>()

        val idTypeArgument = mockk<KSTypeArgument>()
        every { idTypeArgument.toTypeName() } returns idTypeName

        val companionSuperclassType = mockk<KSType>()
        every { companionSuperclassType.arguments } returns listOf(idTypeArgument)

        val companionObject = mockk<KSClassDeclaration>()
        every { companionObject.isCompanionObject } returns true
        every { companionObject.superclassType(eq(HIBERNATE_PANACHE_COMPANION_BASE)) } returns companionSuperclassType

        val qualifiedPackageName = "ch.icken.model"
        val packageName = mockk<KSName>()
        every { packageName.asString() } returns qualifiedPackageName

        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HIBERNATE_PANACHE_ENTITY_BASE)) } returns true
        every { validClass.declarations } returns sequenceOf(companionObject)
        every { validClass.packageName } returns packageName

        every { resolver.getSymbolsWithAnnotation(eq(JAKARTA_PERSISTENCE_ENTITY)) } returns sequenceOf(validClass)

        every { processor.createEntityExtensions(any(), any()) } just Runs

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 1) {
            processor.createEntityExtensions(
                ksClass = eq(validClass),
                idTypeName = eq(idTypeName)
            )
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessValidWithCompanionNotPanacheCompanion() {

        // Given
        val companionObject = mockk<KSClassDeclaration>()
        every { companionObject.isCompanionObject } returns true
        every { companionObject.superclassType(eq(HIBERNATE_PANACHE_COMPANION_BASE)) } returns null

        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HIBERNATE_PANACHE_ENTITY_BASE)) } returns true
        every { validClass.declarations } returns sequenceOf(companionObject)

        every { resolver.getSymbolsWithAnnotation(eq(JAKARTA_PERSISTENCE_ENTITY)) } returns sequenceOf(validClass)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createEntityExtensions(any(), any())
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
        every { validClass.isSubclass(eq(HIBERNATE_PANACHE_ENTITY_BASE)) } returns true
        every { validClass.declarations } returns sequenceOf(companionObject)

        every { resolver.getSymbolsWithAnnotation(eq(JAKARTA_PERSISTENCE_ENTITY)) } returns sequenceOf(validClass)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createEntityExtensions(any(), any())
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessValidWithNotClass() {

        // Given
        val function = mockk<KSFunctionDeclaration>()

        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HIBERNATE_PANACHE_ENTITY_BASE)) } returns true
        every { validClass.declarations } returns sequenceOf(function)

        every { resolver.getSymbolsWithAnnotation(eq(JAKARTA_PERSISTENCE_ENTITY)) } returns sequenceOf(validClass)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createEntityExtensions(any(), any())
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessValidWithoutCompanion() {

        // Given
        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns true
        every { validClass.isSubclass(eq(HIBERNATE_PANACHE_ENTITY_BASE)) } returns true
        every { validClass.declarations } returns emptySequence()

        every { resolver.getSymbolsWithAnnotation(eq(JAKARTA_PERSISTENCE_ENTITY)) } returns sequenceOf(validClass)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createEntityExtensions(any(), any())
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
            processor.createEntityExtensions(any(), any())
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessNotClass() {

        // Given
        val validFunction = mockk<KSFunctionDeclaration>()
        every { validFunction.validate(any()) } returns true

        every { resolver.getSymbolsWithAnnotation(eq(JAKARTA_PERSISTENCE_ENTITY)) } returns sequenceOf(validFunction)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createEntityExtensions(any(), any())
        }
        assertEquals(0, invalid.size)
    }

    @Test
    fun testProcessInvalid() {

        // Given
        val validClass = mockk<KSClassDeclaration>()
        every { validClass.validate(any()) } returns false

        every { resolver.getSymbolsWithAnnotation(eq(JAKARTA_PERSISTENCE_ENTITY)) } returns sequenceOf(validClass)

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 0) {
            processor.createEntityExtensions(any(), any())
        }
        assertEquals(1, invalid.size)
    }
    //endregion

    //region createQueryBuilderExtensions
    @Test
    fun testCreateEntityExtensions() {

        // Given
        val packageName = "ch.icken.model"
        val simpleName = "Employee"
        val className = ClassName(packageName, simpleName)

        val packageSimpleName = mockk<KSName>()
        every { packageSimpleName.asString() } returns packageName

        val classSimpleName = mockk<KSName>()
        every { classSimpleName.asString() } returns simpleName

        val ksClass = mockk<KSClassDeclaration>()
        every { ksClass.toClassName() } returns className
        every { ksClass.packageName } returns packageSimpleName
        every { ksClass.simpleName } returns classSimpleName

        // When
        processor.createEntityExtensions(ksClass, LongClassName)

        // Then
        verify(exactly = 1) {
            codeGenerator.createNewFile(any(), any(), any())
        }
    }
    //endregion
}
