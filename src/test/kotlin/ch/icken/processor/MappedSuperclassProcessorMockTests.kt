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

import ch.icken.processor.EntityProcessor.Companion.HIBERNATE_PANACHE_ENTITY
import ch.icken.processor.EntityProcessor.Companion.JAKARTA_PERSISTENCE_JOIN_COLUMN
import ch.icken.processor.EntityProcessor.Companion.JAKARTA_PERSISTENCE_MAPPED_SUPERCLASS
import ch.icken.processor.EntityProcessor.Companion.PROCESSOR_COLUMN_TYPE
import ch.icken.processor.EntityProcessor.Companion.StringClassName
import ch.icken.processor.Processor.Companion.HIBERNATE_PANACHE_ENTITY_BASE
import ch.icken.processor.Processor.Companion.OPTION_ADD_GENERATED_ANNOTATION
import ch.icken.processor.model.KSClassDeclarationWithProperties
import ch.icken.processor.model.KSClassDeclarationWithSuperTypes
import ch.icken.processor.model.KSClassDeclarationWithSuperTypes.Companion.JAKARTA_PERSISTENCE_TRANSIENT
import ch.icken.processor.model.KSClassDeclarationWithSuperTypes.Companion.PARAM_NAME_MAPPED_BY
import ch.icken.processor.model.withSuperTypes
import com.google.devtools.ksp.getDeclaredProperties
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
class MappedSuperclassProcessorMockTests : ProcessorMockTestCommon() {

    @MockK
    private lateinit var resolver: Resolver

    private val codeGenerator = mockk<CodeGenerator>(relaxed = true)
    private val processor = spyk(MappedSuperclassProcessor(
        options = mapOf(OPTION_ADD_GENERATED_ANNOTATION to "false"),
        codeGenerator = codeGenerator,
        logger = mockk<KSPLogger>().also {
            every { it.info(any()) } just Runs
        }
    ))

    //region process
    @Test
    fun testProcess() {

        // Given
        val columnProperty = mockk<KSPropertyDeclaration>()
        every { columnProperty.hasBackingField } returns true
        every { columnProperty.hasAnnotation(eq(JAKARTA_PERSISTENCE_TRANSIENT)) } returns false
        every { columnProperty.annotation(any()).isParameterSet(eq(PARAM_NAME_MAPPED_BY)) } returns false

        val mappedSuperclass = mockk<KSClassDeclaration>()
        every { mappedSuperclass.validate(any()) } returns true
        every { mappedSuperclass.getDeclaredProperties() } returns sequenceOf(columnProperty)

        val withSuperTypes = mockk<KSClassDeclarationWithSuperTypes>()
        every { withSuperTypes.ksClassDeclaration } returns mappedSuperclass
        every { withSuperTypes.isSubclass(eq(HIBERNATE_PANACHE_ENTITY_BASE)) } returns true
        every { withSuperTypes.withColumnProperties() } answers { callOriginal() }

        every { mappedSuperclass.withSuperTypes() } returns withSuperTypes

        every { resolver.getSymbolsWithAnnotation(eq(JAKARTA_PERSISTENCE_MAPPED_SUPERCLASS)) } returns sequenceOf(mappedSuperclass)

        every { processor.createColumnsBaseSuperclass(any()) } just Runs

        // When
        val invalid = processor.process(resolver)

        // Then
        verify(exactly = 1) {
            processor.createColumnsBaseSuperclass(withArg {
                assertEquals(mappedSuperclass, it.ksClassDeclaration)
                assertEquals(1, it.propertiesSize)
                assertEquals(columnProperty, it.properties[0])
            })
        }
        assertEquals(0, invalid.size)
    }
    //endregion

    //region createColumnsBaseSuperclass
    @Test
    fun testCreateColumnsBaseSuperclass() {

        // Given
        val mappedSuperclassPackageName = mockk<KSName>()
        every { mappedSuperclassPackageName.asString() } returns "ch.icken.model"

        val mappedSuperclassSimpleName = mockk<KSName>()
        every { mappedSuperclassSimpleName.asString() } returns "NamedPanacheEntity"

        //region superclass
        val superclassDeclaration = mockk<KSDeclaration>()
        every { superclassDeclaration.isClass(HIBERNATE_PANACHE_ENTITY) } returns true
        every { superclassDeclaration.hasAnnotation(JAKARTA_PERSISTENCE_MAPPED_SUPERCLASS) } returns true

        val superclassType = mockk<KSType>()
        every { superclassType.declaration } returns superclassDeclaration

        val superclassTypeReference = mockk<KSTypeReference>()
        every { superclassTypeReference.resolve() } returns superclassType
        //endregion

        val ksClass = mockk<KSClassDeclaration>()
        every { ksClass.packageName } returns mappedSuperclassPackageName
        every { ksClass.simpleName } returns mappedSuperclassSimpleName
        every { ksClass.superTypes } returns sequenceOf(superclassTypeReference)

        //region name
        val nameSimpleName = mockk<KSName>()
        every { nameSimpleName.asString() } returns "name"

        val nameType = mockk<KSType>()
        every { nameType.toClassName() } returns StringClassName
        every { nameType.isMarkedNullable } returns false

        val nameTypeReference = mockk<KSTypeReference>()
        every { nameTypeReference.resolve() } returns nameType

        val name = mockk<KSPropertyDeclaration>()
        every { name.simpleName } returns nameSimpleName
        every { name.type } returns nameTypeReference
        every { name.hasAnnotation(eq(JAKARTA_PERSISTENCE_JOIN_COLUMN)) } returns false
        every { name.annotation(eq(PROCESSOR_COLUMN_TYPE)) } returns null
        //endregion

        val withProperties = KSClassDeclarationWithProperties(
            ksClassDeclaration = ksClass,
            properties = listOf(name),
        )

        // When
        processor.createColumnsBaseSuperclass(withProperties)

        // Then
        verify(exactly = 1) {
            codeGenerator.createNewFile(any(), any(), any())
        }
    }
    //endregion
}
