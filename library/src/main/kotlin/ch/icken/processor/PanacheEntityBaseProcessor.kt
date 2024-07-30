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

import ch.icken.processor.ClassNames.ColumnClassName
import ch.icken.processor.ClassNames.StringClassName
import ch.icken.processor.GenerationOptions.ADD_GENERATED_ANNOTATION
import ch.icken.processor.GenerationOptions.generatedAnnotation
import ch.icken.processor.GenerationValues.COLUMN_NAME_BASE_CLASS_PARAM_NAME
import ch.icken.processor.GenerationValues.COLUMN_NAME_BASE_CLASS_SUFFIX
import ch.icken.processor.GenerationValues.COLUMN_NAME_OBJECT_SUFFIX
import ch.icken.processor.GenerationValues.FileSuppress
import ch.icken.processor.GenerationValues.GENERATED_PACKAGE_SUFFIX
import ch.icken.processor.QualifiedNames.HibernatePanacheEntityBase
import ch.icken.processor.QualifiedNames.JakartaPersistenceEntity
import ch.icken.processor.QualifiedNames.JakartaPersistenceJoinColumn
import ch.icken.processor.QualifiedNames.JakartaPersistenceManyToMany
import ch.icken.processor.QualifiedNames.JakartaPersistenceOneToMany
import ch.icken.processor.QualifiedNames.JakartaPersistenceOneToOne
import ch.icken.processor.QualifiedNames.JakartaPersistenceTransient
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class PanacheEntityBaseProcessor(
    private val options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (valid, invalid) = resolver.getSymbolsWithAnnotation(JakartaPersistenceEntity)
            .partition(KSAnnotated::validate)

        val addGeneratedAnnotation = options[ADD_GENERATED_ANNOTATION].toBoolean()

        valid.filterIsInstance<KSClassDeclaration>()
            .filter { it.isSubclass(HibernatePanacheEntityBase) }
            .forEach { ksClassDeclaration ->
                val columnProperties = ksClassDeclaration.getAllProperties()
                    .filterNot { it.hasAnnotation(JakartaPersistenceTransient) }
                    .filterNot { it.annotation(JakartaPersistenceManyToMany).nonDefaultParameter(MAPPED_BY) }
                    .filterNot { it.annotation(JakartaPersistenceOneToMany).nonDefaultParameter(MAPPED_BY) }
                    .filterNot { it.annotation(JakartaPersistenceOneToOne).nonDefaultParameter(MAPPED_BY) }

                createColumnNamesObject(ksClassDeclaration, columnProperties.toList(), addGeneratedAnnotation)
            }

        return invalid
    }

    internal fun createColumnNamesObject(ksClass: KSClassDeclaration, ksProperties: List<KSPropertyDeclaration>,
                                        addGeneratedAnnotation: Boolean) {
        val packageName = ksClass.packageName.asString() + GENERATED_PACKAGE_SUFFIX
        val objectName = ksClass.simpleName.asString() + COLUMN_NAME_OBJECT_SUFFIX
        val baseClassName = objectName + COLUMN_NAME_BASE_CLASS_SUFFIX
        logger.info("Generating $packageName.$objectName (${ksProperties.size} columns)")

        // Generate base class
        val baseClassBuilder = TypeSpec.classBuilder(baseClassName)
            .addModifiers(KModifier.OPEN)
            .addAnnotationIf(GeneratedAnnotation, addGeneratedAnnotation)
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
                ksProperties.forEach { ksProperty ->
                    val propertyName = ksProperty.simpleName.asString()
                    val isJoinColumn = ksProperty.hasAnnotation(JakartaPersistenceJoinColumn)

                    val propertyBuilder = if (isJoinColumn) {
                        val joinObjectName = ksProperty.typeName + COLUMN_NAME_OBJECT_SUFFIX
                        val joinBaseClassName = joinObjectName + COLUMN_NAME_BASE_CLASS_SUFFIX
                        val joinBaseClass = ClassName(packageName, joinBaseClassName)

                        PropertySpec.builder(propertyName, joinBaseClass)
                            .initializer("%T(%S)", joinBaseClass, "$propertyName.")
                    } else {
                        val ksPropertyType = ksProperty.type.resolve()
                        val columnNameParameterType = ksPropertyType.toClassName()
                            .copy(nullable = ksPropertyType.isMarkedNullable)

                        PropertySpec.builder(propertyName, ColumnClassName.plusParameter(columnNameParameterType))
                            .initializer("%T(%P)", ColumnClassName,
                                "\${${COLUMN_NAME_BASE_CLASS_PARAM_NAME}.orEmpty()}$propertyName")
                    }.addAnnotationIf(GeneratedAnnotation, addGeneratedAnnotation)

                    addProperty(propertyBuilder.build())
                }
            }

        // Generate implementation
        val objectBuilder = TypeSpec.objectBuilder(objectName)
            .superclass(ClassName(packageName, baseClassName))
            .addAnnotationIf(GeneratedAnnotation, addGeneratedAnnotation)

        // Generate actual source code file
        FileSpec.builder(packageName, objectName)
            .addType(baseClassBuilder.build())
            .addType(objectBuilder.build())
            .addAnnotation(FileSuppress)
            .addAnnotationIf(GeneratedAnnotation, addGeneratedAnnotation)
            .build()
            .writeTo(codeGenerator, Dependencies(false))
    }

    companion object {
        internal const val MAPPED_BY = "mappedBy"

        private val GeneratedAnnotation = generatedAnnotation(PanacheEntityBaseProcessor::class.java)
    }
}

class PanacheEntityBaseProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        PanacheEntityBaseProcessor(environment.options, environment.codeGenerator, environment.logger)
}
