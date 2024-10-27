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
    options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : ProcessorCommon(options), SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (valid, invalid) = resolver.getSymbolsWithAnnotation(JakartaPersistenceEntity)
            .partition(KSAnnotated::validate)

        valid.filterIsInstance<KSClassDeclaration>()
            .filter { it.isSubclass(HibernatePanacheEntityBase) }
            .associateWith { ksClassDeclaration ->
                ksClassDeclaration.getAllProperties()
                    .filterNot { it.hasAnnotation(JakartaPersistenceTransient) }
                    .filterNot { it.annotation(JakartaPersistenceManyToMany).isParameterSet(PARAM_NAME_MAPPED_BY) }
                    .filterNot { it.annotation(JakartaPersistenceOneToMany).isParameterSet(PARAM_NAME_MAPPED_BY) }
                    .filterNot { it.annotation(JakartaPersistenceOneToOne).isParameterSet(PARAM_NAME_MAPPED_BY) }
                    .toList()
            }.forEach(::createColumnsObject)

        return invalid
    }

    internal fun createColumnsObject(ksClass: KSClassDeclaration, ksProperties: List<KSPropertyDeclaration>) {
        val packageName = ksClass.packageName.asString() + SUFFIX_PACKAGE_GENERATED
        val objectName = ksClass.simpleName.asString() + SUFFIX_OBJECT_COLUMNS
        val baseClassName = objectName + SUFFIX_CLASS_COLUMNS_BASE
        logger.info("Generating $packageName.$objectName (${ksProperties.size} columns)")

        // Generate base class
        val baseClassBuilder = TypeSpec.classBuilder(baseClassName)
            .addModifiers(KModifier.OPEN)
            .addAnnotationIf(generatedAnnotation, addGeneratedAnnotation)
            .apply {
                // Generate constructor
                val constructorParamBuilder = ParameterSpec
                    .builder(PARAM_NAME_COLUMNS_BASE_CLASS, StringClassName.copy(nullable = true))
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
                        val joinObjectName = ksProperty.typeName + SUFFIX_OBJECT_COLUMNS
                        val joinBaseClassName = joinObjectName + SUFFIX_CLASS_COLUMNS_BASE
                        val joinBaseClass = ClassName(packageName, joinBaseClassName)

                        PropertySpec.builder(propertyName, joinBaseClass)
                            .initializer("%T(%S)", joinBaseClass, "$propertyName.")
                    } else {
                        val ksPropertyType = ksProperty.type.resolve()
                        val columnTypeParameter = (ksProperty.columnTypeClassName ?: ksPropertyType.toClassName())
                            .copy(nullable = ksPropertyType.isMarkedNullable)

                        PropertySpec.builder(propertyName, ColumnClassName.plusParameter(columnTypeParameter))
                            .initializer("%T(%P)", ColumnClassName,
                                "\${${PARAM_NAME_COLUMNS_BASE_CLASS}.orEmpty()}$propertyName")
                    }.addAnnotationIf(generatedAnnotation, addGeneratedAnnotation)

                    addProperty(propertyBuilder.build())
                }
            }

        // Generate implementation
        val objectBuilder = TypeSpec.objectBuilder(objectName)
            .superclass(ClassName(packageName, baseClassName))
            .addAnnotationIf(generatedAnnotation, addGeneratedAnnotation)

        // Generate actual source code file
        FileSpec.builder(packageName, objectName)
            .addType(baseClassBuilder.build())
            .addType(objectBuilder.build())
            .addAnnotation(suppressFileAnnotation)
            .addAnnotationIf(generatedAnnotation, addGeneratedAnnotation)
            .build()
            .writeTo(codeGenerator, Dependencies(false))
    }
}

class PanacheEntityBaseProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        PanacheEntityBaseProcessor(environment.options, environment.codeGenerator, environment.logger)
}
