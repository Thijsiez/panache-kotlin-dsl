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

import ch.icken.processor.model.KSClassDeclarationWithProperties
import ch.icken.processor.model.KSClassDeclarationWrapper
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.ksp.toClassName

internal sealed class EntityProcessor(options: Map<String, String>) : Processor(options) {

    protected val KSClassDeclarationWrapper.columnsBaseClassName get() = ksClassDeclaration.columnsBaseClassName

    protected val KSDeclaration.columnsBaseClassName get() = columnsObjectName + SUFFIX_BASE

    protected fun createColumnsBaseClass(entity: KSClassDeclarationWithProperties): TypeSpec {
        val columnsBaseClassConstructorParameter = ParameterSpec
            .builder(
                name = PARAM_NAME_PARENT,
                type = StringClassName.copy(nullable = true)
            )
            .defaultValue("%L", null)
            .build()
        val columnsBaseClassConstructor = FunSpec.constructorBuilder()
            .addModifiers(KModifier.INTERNAL)
            .addParameter(columnsBaseClassConstructorParameter)
            .build()

        val mappedSuperclass = entity.superTypes
            .map { it.resolve().declaration }
            .singleOrNull { it.hasAnnotation(JAKARTA_PERSISTENCE_MAPPED_SUPERCLASS) }

        val columnsBaseClassBuilder = TypeSpec.classBuilder(entity.columnsBaseClassName)
            .addModifiers(KModifier.OPEN)
            .addTypeVariable(TypeVariableName(TYPE_VARIABLE_NAME_COLUMNS))
            .addGeneratedAnnotation()
            .primaryConstructor(columnsBaseClassConstructor)
            .addProperties(entity.mapProperties(::createColumnProperty))

        if (mappedSuperclass != null) {
            //Since PanacheEntity is not part of the entities being processed, no Columns base class is generated
            //When this entity's superclass is PanacheEntity, we do not want to specify a superclass
            //Instead, we'll add the "id" property from PanacheEntity to this one
            if (mappedSuperclass.isClass(HIBERNATE_PANACHE_ENTITY)) {
                columnsBaseClassBuilder.addProperty(createPanacheEntityIdProperty())
            } else {
                val mappedSuperclassName = ClassName(mappedSuperclass.generatedPackageName,
                    mappedSuperclass.columnsBaseClassName)
                    .plusParameter(TypeVariableName(TYPE_VARIABLE_NAME_COLUMNS))
                columnsBaseClassBuilder.superclass(mappedSuperclassName)
            }
        }

        return columnsBaseClassBuilder.build()
    }

    private fun createColumnProperty(ksProperty: KSPropertyDeclaration): PropertySpec {
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
            .initializer("%T(%P)", ColumnClassName, "\${$PARAM_NAME_PARENT.orEmpty()}$propertyName")
            .addGeneratedAnnotation()
            .build()
    }

    private fun createJoinColumnProperty(propertyName: String, propertyTypeDeclaration: KSDeclaration): PropertySpec {
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

    private fun createPanacheEntityIdProperty(): PropertySpec {
        val idPropertyName = "id"

        val idColumnPropertyTypeName = ColumnClassName.plusParameter(TypeVariableName(TYPE_VARIABLE_NAME_COLUMNS))
            .plusParameter(LongClassName.copy(nullable = true))

        return PropertySpec.builder(idPropertyName, idColumnPropertyTypeName)
            .initializer("%T(%P)", ColumnClassName, "\${$PARAM_NAME_PARENT.orEmpty()}$idPropertyName")
            .addGeneratedAnnotation()
            .build()
    }

    internal companion object {
        //region Class Names
        private val ColumnClassName = ClassName(QUERY_PACKAGE, "Column")
        internal val StringClassName = ClassName("kotlin", "String")
        //endregion
        //region Constants
        private const val PARAM_NAME_PARENT = "parent"
        internal const val PARAM_NAME_TYPE = "type"
        private const val SUFFIX_BASE = "Base"
        private const val TYPE_VARIABLE_NAME_COLUMNS = "Columns"
        //endregion
        //region Names
        internal const val HIBERNATE_PANACHE_ENTITY: String = "io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity"
        internal const val JAKARTA_PERSISTENCE_JOIN_COLUMN: String = "jakarta.persistence.JoinColumn"
        internal const val JAKARTA_PERSISTENCE_MAPPED_SUPERCLASS: String = "jakarta.persistence.MappedSuperclass"
        internal const val PROCESSOR_COLUMN_TYPE: String = "ch.icken.processor.ColumnType"
        //endregion
    }
}
