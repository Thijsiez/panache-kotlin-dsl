/*
 * Copyright 2023 Thijs Koppen
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

import ch.icken.processor.ClassNames.ColumnNameClassName
import ch.icken.processor.ClassNames.EnumTypeOrdinalClassName
import ch.icken.processor.ClassNames.EnumTypeStringClassName
import ch.icken.processor.ClassNames.StringClassName
import ch.icken.processor.GenerationValues.COLUMN_NAME_BASE_CLASS_PARAM_NAME
import ch.icken.processor.GenerationValues.COLUMN_NAME_BASE_CLASS_SUFFIX
import ch.icken.processor.GenerationValues.COLUMN_NAME_OBJECT_SUFFIX
import ch.icken.processor.GenerationValues.FileSuppress
import ch.icken.processor.GenerationValues.GENERATED_PACKAGE_SUFFIX
import ch.icken.processor.QualifiedNames.HibernatePanacheEntityBase
import ch.icken.processor.QualifiedNames.JakartaPersistenceColumn
import ch.icken.processor.QualifiedNames.JakartaPersistenceEntity
import ch.icken.processor.QualifiedNames.JakartaPersistenceEnumerated
import ch.icken.processor.QualifiedNames.JakartaPersistenceJoinColumn
import ch.icken.processor.QualifiedNames.KotlinEnum
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import jakarta.persistence.EnumType

class PanacheEntityBaseProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (valid, invalid) = resolver.getSymbolsWithAnnotation(JakartaPersistenceEntity)
            .partition(KSAnnotated::validate)

        val enumType = resolver.getClassDeclarationByName(KotlinEnum)?.asStarProjectedType()

        valid.filterIsInstance<KSClassDeclaration>()
            .filter { it.isSubclass(HibernatePanacheEntityBase) }
            .forEach { ksClassDeclaration ->
                val columnProperties = ksClassDeclaration.getAllProperties().filter {
                    it.hasAnnotation(JakartaPersistenceColumn) || it.hasAnnotation(JakartaPersistenceJoinColumn)
                }
                createColumnNamesObject(ksClassDeclaration, columnProperties.toList(), enumType)
            }

        return invalid
    }

    private fun createColumnNamesObject(ksClass: KSClassDeclaration, ksProperties: List<KSPropertyDeclaration>,
                                        enumType: KSType?) {
        val packageName = ksClass.packageName.asString() + GENERATED_PACKAGE_SUFFIX
        val objectName = ksClass.simpleName.asString() + COLUMN_NAME_OBJECT_SUFFIX
        val baseClassName = objectName + COLUMN_NAME_BASE_CLASS_SUFFIX
        logger.info("Generating $packageName.$objectName (${ksProperties.size} columns)")

        // Generate base class
        val baseClassBuilder = TypeSpec.classBuilder(baseClassName)
            .addModifiers(KModifier.OPEN)
            .apply {
                // Generate constructor
                val constructorParamBuilder = ParameterSpec
                    .builder(COLUMN_NAME_BASE_CLASS_PARAM_NAME, StringClassName.copy(nullable = true))
                    .defaultValue("%L", null)
                val constructorBuilder = FunSpec.constructorBuilder()
                    .addModifiers(KModifier.INTERNAL)
                    .addParameter(constructorParamBuilder.build())
                primaryConstructor(constructorBuilder.build())

                // Generate properties
                ksProperties.forEach { ksPropertyDeclaration ->
                    val propertyName = ksPropertyDeclaration.simpleName.asString()
                    val isJoinColumn = ksPropertyDeclaration.hasAnnotation(JakartaPersistenceJoinColumn)

                    val propertyBuilder = if (isJoinColumn) {
                        val joinObjectName = ksPropertyDeclaration.typeName + COLUMN_NAME_OBJECT_SUFFIX
                        val joinBaseClassName = joinObjectName + COLUMN_NAME_BASE_CLASS_SUFFIX
                        val joinBaseClass = ClassName(packageName, joinBaseClassName)

                        PropertySpec.builder(propertyName, joinBaseClass)
                            .initializer("%T(%S)", joinBaseClass, "$propertyName.")
                    } else {
                        val ksPropertyType = ksPropertyDeclaration.type.resolve()
                        val columnNameParameterType = ksPropertyType.toClassName()
                            .copy(nullable = ksPropertyType.isMarkedNullable)
                        val isEnum = enumType?.isAssignableFrom(ksPropertyType) ?: false

                        val initType = if (isEnum) {
                            val enumerated = ksPropertyDeclaration.getAnnotationEnumArgumentValue<EnumType>(
                                JakartaPersistenceEnumerated, "value") ?: EnumType.ORDINAL
                            when (enumerated) {
                                EnumType.ORDINAL -> EnumTypeOrdinalClassName
                                EnumType.STRING -> EnumTypeStringClassName
                            }
                        } else ColumnNameClassName

                        PropertySpec.builder(propertyName, ColumnNameClassName.plusParameter(columnNameParameterType))
                            .initializer("%T(%P)", initType,
                                "\${${COLUMN_NAME_BASE_CLASS_PARAM_NAME}.orEmpty()}$propertyName")
                    }

                    addProperty(propertyBuilder.build())
                }
            }

        // Generate implementation
        val objectBuilder = TypeSpec.objectBuilder(objectName)
            .superclass(ClassName(packageName, baseClassName))

        // Generate actual source code file
        FileSpec.builder(packageName, objectName)
            .addType(baseClassBuilder.build())
            .addType(objectBuilder.build())
            .addAnnotation(FileSuppress)
            .build()
            .writeTo(codeGenerator, Dependencies(false))
    }
}

class PanacheEntityBaseProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        PanacheEntityBaseProcessor(environment.codeGenerator, environment.logger)
}