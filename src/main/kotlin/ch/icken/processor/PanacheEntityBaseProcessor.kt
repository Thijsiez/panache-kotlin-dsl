/*
 * Copyright 2023-2026 Thijs Koppen
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

import ch.icken.processor.model.KSClassDeclarationWithProperties
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

internal class PanacheEntityBaseProcessor(
    options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : EntityProcessor(options) {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (valid, invalid) = resolver.getSymbolsWithAnnotation(JAKARTA_PERSISTENCE_ENTITY)
            .partition(KSAnnotated::validate)

        valid.filterPanacheEntities()
            .map { it.withColumnProperties() }
            .forEach(::createColumnsObject)

        return invalid
    }

    fun createColumnsObject(entity: KSClassDeclarationWithProperties) {
        val targetPackageName = entity.generatedPackageName
        val columnsObjectName = entity.columnsObjectName
        logger.info("Generating $targetPackageName.$columnsObjectName (${entity.propertiesSize} columns)")

        // Generate constructor
        val columnsBaseClassConstructorParameter = ParameterSpec
            .builder(
                name = PARAM_NAME_CLASS_COLUMNS_BASE_CONSTRUCTOR,
                type = StringClassName.copy(nullable = true)
            )
            .defaultValue("%L", null)
            .build()
        val columnsBaseClassConstructor = FunSpec.constructorBuilder()
            .addModifiers(KModifier.INTERNAL)
            .addParameter(columnsBaseClassConstructorParameter)
            .build()

        // Generate base class
        val columnsBaseClassName = entity.columnsBaseClassName
        val columnsBaseClass = TypeSpec.classBuilder(columnsBaseClassName)
            //TODO superclass that is the base columns class for the mapped superclass
            .addModifiers(KModifier.OPEN)
            .addTypeVariable(TypeVariableName(TYPE_VARIABLE_NAME_COLUMNS))
            .addGeneratedAnnotation()
            .primaryConstructor(columnsBaseClassConstructor)
            .addProperties(entity.mapProperties(::createColumnProperty))
            .build()

        // Generate implementation
        val columnsObjectSuperclassTypeName = ClassName(targetPackageName, columnsBaseClassName)
            .plusParameter(ClassName(targetPackageName, columnsObjectName))
        val columnsObject = TypeSpec.objectBuilder(columnsObjectName)
            .superclass(columnsObjectSuperclassTypeName)
            .addGeneratedAnnotation()
            .build()

        // Generate actual source code file
        FileSpec.builder(targetPackageName, columnsObjectName)
            .addType(columnsBaseClass)
            .addType(columnsObject)
            .addAnnotation(suppressFileAnnotation)
            .addGeneratedAnnotation()
            .build()
            .writeTo(codeGenerator, Dependencies(false))
    }

    fun createColumnProperty(ksProperty: KSPropertyDeclaration): PropertySpec {
        val propertyName = ksProperty.simpleName.asString()
        val propertyType = ksProperty.type.resolve()

        val isJoinColumn = ksProperty.hasAnnotation(JAKARTA_PERSISTENCE_JOIN_COLUMN)
        if (isJoinColumn) return createJoinColumnProperty(propertyName, propertyType.declaration)

        //The value from get() can be any type. In the case of @ColumnType, it will be a KSType
        @Suppress("kotlin:S6530")
        val specifiedColumnType = ksProperty.annotation(PROCESSOR_COLUMN_TYPE)
            ?.arguments
            ?.get(PARAM_NAME_TYPE) as? KSType
        val columnTypeName = (specifiedColumnType ?: propertyType).toClassName()
            .copy(nullable = propertyType.isMarkedNullable)
        val columnPropertyTypeName = ColumnClassName.plusParameter(TypeVariableName(TYPE_VARIABLE_NAME_COLUMNS))
            .plusParameter(columnTypeName)

        return PropertySpec.builder(propertyName, columnPropertyTypeName)
            .initializer("%T(%P)", ColumnClassName,
                "\${$PARAM_NAME_CLASS_COLUMNS_BASE_CONSTRUCTOR.orEmpty()}$propertyName")
            .addGeneratedAnnotation()
            .build()
    }

    fun createJoinColumnProperty(propertyName: String, propertyTypeDeclaration: KSDeclaration): PropertySpec {
        val joinColumnPropertyTypeName = ClassName(propertyTypeDeclaration.generatedPackageName,
            propertyTypeDeclaration.columnsBaseClassName)
            .plusParameter(TypeVariableName(TYPE_VARIABLE_NAME_COLUMNS))

        val joinColumnPropertyGetter = FunSpec.getterBuilder()
            .addStatement("return %T(%S)", joinColumnPropertyTypeName, "$propertyName.")
            .build()

        return PropertySpec.builder(propertyName, joinColumnPropertyTypeName)
            .getter(joinColumnPropertyGetter)
            .addGeneratedAnnotation()
            .build()
    }

    companion object {
        //region Class Names
        internal val ColumnClassName = ClassName(QUERY_PACKAGE, "Column")
        internal val StringClassName = ClassName("kotlin", "String")
        //endregion
        //region Constants
        internal const val TYPE_VARIABLE_NAME_COLUMNS = "Columns"
        internal const val PARAM_NAME_TYPE = "type"
        //endregion
        //region Names
        internal const val JAKARTA_PERSISTENCE_JOIN_COLUMN: String = "jakarta.persistence.JoinColumn"
        internal const val PROCESSOR_COLUMN_TYPE: String = "ch.icken.processor.ColumnType"
        //endregion
    }
}

class PanacheEntityBaseProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        PanacheEntityBaseProcessor(environment.options, environment.codeGenerator, environment.logger)
}
