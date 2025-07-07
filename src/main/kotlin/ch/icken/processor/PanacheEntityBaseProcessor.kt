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
import jakarta.persistence.*

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
            //Find which properties are mapped to columns and thus can be queried against
            .associateWith { ksClassDeclaration ->
                ksClassDeclaration.getAllProperties()
                    //When a property is annotated as @Transient, it is not mapped to a column in the database
                    // Therefore, this property can never be used in a query, so we won't generate a Column for it
                    .filterNot { it.hasAnnotation(JakartaPersistenceTransient) }
                    //When a property is mapped by another entity, we'd have to use a JOIN to use it in a query
                    // Currently, a JOIN is not supported syntax-wise, so we won't generate Columns for these properties
                    .filterNot { it.annotation(JakartaPersistenceManyToMany).isParameterSet(PARAM_NAME_MAPPED_BY) }
                    .filterNot { it.annotation(JakartaPersistenceOneToMany).isParameterSet(PARAM_NAME_MAPPED_BY) }
                    .filterNot { it.annotation(JakartaPersistenceOneToOne).isParameterSet(PARAM_NAME_MAPPED_BY) }
                    .toList()
            }
            .forEach(::createColumnsObject)

        return invalid
    }

    internal fun createColumnsObject(ksClass: KSClassDeclaration, ksProperties: List<KSPropertyDeclaration>) {
        val packageName = ksClass.packageName.asString() + SUFFIX_PACKAGE_GENERATED
        val objectName = ksClass.simpleName.asString() + SUFFIX_OBJECT_COLUMNS
        logger.info("Generating $packageName.$objectName (${ksProperties.size} columns)")

        // Generate constructor
        val constructorBuilder = FunSpec.constructorBuilder()
            .addModifiers(KModifier.INTERNAL)
            .addParameter(ParameterSpec
                .builder(PARAM_NAME_COLUMNS_BASE_CLASS, StringClassName.copy(nullable = true))
                .defaultValue("%L", null)
                .build())

        // Generate base class
        val baseClassName = objectName + SUFFIX_CLASS_COLUMNS_BASE
        val baseClassBuilder = TypeSpec.classBuilder(baseClassName)
            .addModifiers(KModifier.OPEN)
            .addTypeVariable(TypeVariableName(TYPE_VARIABLE_NAME_COLUMNS))
            .addGeneratedAnnotation()
            .primaryConstructor(constructorBuilder.build())
            .addProperties(ksProperties.map(::createColumnProperty))

        // Generate implementation
        val objectBuilder = TypeSpec.objectBuilder(objectName)
            .superclass(ClassName(packageName, baseClassName)
                .plusParameter(ClassName(packageName, objectName)))
            .addGeneratedAnnotation()

        // Generate actual source code file
        FileSpec.builder(packageName, objectName)
            .addType(baseClassBuilder.build())
            .addType(objectBuilder.build())
            .addAnnotation(suppressFileAnnotation)
            .addGeneratedAnnotation()
            .build()
            .writeTo(codeGenerator, Dependencies(false))
    }

    internal fun createColumnProperty(ksProperty: KSPropertyDeclaration): PropertySpec {
        val propertyName = ksProperty.simpleName.asString()
        val propertyType = ksProperty.type.resolve()

        val isJoinColumn = ksProperty.hasAnnotation(JakartaPersistenceJoinColumn)
        if (isJoinColumn) {
            val propertyTypeDeclaration = propertyType.declaration
            val joinPackageName = propertyTypeDeclaration.packageName.asString() + SUFFIX_PACKAGE_GENERATED
            val joinObjectName = propertyTypeDeclaration.simpleName.asString() + SUFFIX_OBJECT_COLUMNS
            val joinBaseClassType = ClassName(joinPackageName, joinObjectName + SUFFIX_CLASS_COLUMNS_BASE)
                .plusParameter(TypeVariableName(TYPE_VARIABLE_NAME_COLUMNS))

            return PropertySpec.builder(propertyName, joinBaseClassType)
                .getter(FunSpec.getterBuilder()
                    .addStatement("return %T(%S)", joinBaseClassType, "$propertyName.")
                    .build())
                .addGeneratedAnnotation()
                .build()
        }

        @Suppress("kotlin:S6530")//False positive
        //The value from get() can be any type. In the case of @ColumnType, it will be a KSType
        val columnTypeParameter = ((ksProperty.annotation(ProcessorColumnType)
            ?.arguments
            ?.get(PARAM_NAME_TYPE) as? KSType)
            ?.toClassName()
            ?: propertyType.toClassName())
            .copy(nullable = propertyType.isMarkedNullable)
        val columnType = ColumnClassName.plusParameter(TypeVariableName(TYPE_VARIABLE_NAME_COLUMNS))
            .plusParameter(columnTypeParameter)

        return PropertySpec.builder(propertyName, columnType)
            .initializer("%T(%P)", ColumnClassName, "\${${PARAM_NAME_COLUMNS_BASE_CLASS}.orEmpty()}$propertyName")
            .addGeneratedAnnotation()
            .build()
    }

    companion object {
        //region Class Names
        internal val ColumnClassName = ch.icken.query.Column::class.asClassName()
        internal val StringClassName = String::class.asClassName()
        //endregion
        //region Constants
        internal const val PARAM_NAME_COLUMNS_BASE_CLASS = "parent"
        internal const val PARAM_NAME_MAPPED_BY = "mappedBy"
        internal const val SUFFIX_CLASS_COLUMNS_BASE = "Base"
        internal const val TYPE_VARIABLE_NAME_COLUMNS = "Columns"
        internal const val PARAM_NAME_TYPE = "type"
        //endregion
        //region Names
        internal val JakartaPersistenceJoinColumn: String = JoinColumn::class.java.name
        internal val JakartaPersistenceManyToMany: String = ManyToMany::class.java.name
        internal val JakartaPersistenceOneToMany: String = OneToMany::class.java.name
        internal val JakartaPersistenceOneToOne: String = OneToOne::class.java.name
        internal val JakartaPersistenceTransient: String = Transient::class.java.name
        internal val ProcessorColumnType: String = ColumnType::class.java.name
        //endregion
    }
}

class PanacheEntityBaseProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        PanacheEntityBaseProcessor(environment.options, environment.codeGenerator, environment.logger)
}
