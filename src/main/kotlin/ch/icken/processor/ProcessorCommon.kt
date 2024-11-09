/*
 * Copyright 2024 Thijs Koppen
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

import ch.icken.query.Column
import ch.icken.query.Component.QueryComponent
import ch.icken.query.Component.UpdateComponent
import ch.icken.query.Expression
import ch.icken.query.PanacheSingleResult
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.asClassName
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import io.quarkus.panache.common.Sort
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.stream.Stream

abstract class ProcessorCommon(options: Map<String, String>) {

    //region Options
    protected val addGeneratedAnnotation = options[OPTION_ADD_GENERATED_ANNOTATION].toBoolean()
    //endregion

    //region Annotations
    protected val suppressFileAnnotation = AnnotationSpec.builder(SuppressClassName)
        .addMember("%S", "RedundantVisibilityModifier")
        .addMember("%S", "unused")
        .build()

    protected val generatedAnnotation = AnnotationSpec.builder(GeneratedClassName)
        .addMember("%S", javaClass.name)
        .addMember("%S", LocalDateTime.now().toString())
        .addMember("%S", "Generated using panache-kotlin-dsl")
        .build()

    protected fun jvmNameAnnotation(name: String) = AnnotationSpec.builder(JvmNameClassName)
        .addMember("%S", name)
        .build()
    //endregion

    companion object {
        //region Class Names
        internal val ColumnClassName = Column::class.asClassName()
        internal val ExpressionClassName = Expression::class.asClassName()
        internal val GeneratedClassName = Generated::class.asClassName()
        internal val InitialUpdateComponentClassName = UpdateComponent.InitialUpdateComponent::class.asClassName()
        internal val JvmNameClassName = JvmName::class.asClassName()
        internal val ListClassName = List::class.asClassName()
        internal val LogicalUpdateComponentClassName = UpdateComponent.LogicalUpdateComponent::class.asClassName()
        internal val LongClassName = Long::class.asClassName()
        internal val PanacheQueryClassName = PanacheQuery::class.asClassName()
        internal val PanacheSingleResultClassName = PanacheSingleResult::class.asClassName()
        internal val QueryComponentClassName = QueryComponent::class.asClassName()
        internal val SetterClassName = UpdateComponent.InitialUpdateComponent.Setter::class.asClassName()
        internal val SortClassName = Sort::class.asClassName()
        internal val StreamClassName = Stream::class.asClassName()
        internal val StringClassName = String::class.asClassName()
        internal val SuppressClassName = Suppress::class.asClassName()
        //endregion
        //region Constants
        internal const val CLASS_NAME_COMPANION = "Companion"
        internal const val FUNCTION_NAME_AND = "and"
        internal const val FUNCTION_NAME_AND_EXPRESSION = "andExpression"
        internal const val FUNCTION_NAME_AND_UPDATE = "andUpdate"
        internal const val FUNCTION_NAME_COUNT = "count"
        internal const val FUNCTION_NAME_DELETE = "delete"
        internal const val FUNCTION_NAME_FIND = "find"
        internal const val FUNCTION_NAME_FIND_SORTED = "findSorted"
        internal const val FUNCTION_NAME_MULTIPLE = "multiple"
        internal const val FUNCTION_NAME_MULTIPLE_SORTED = "multipleSorted"
        internal const val FUNCTION_NAME_OR = "or"
        internal const val FUNCTION_NAME_OR_EXPRESSION = "orExpression"
        internal const val FUNCTION_NAME_OR_UPDATE = "orUpdate"
        internal const val FUNCTION_NAME_SINGLE = "single"
        internal const val FUNCTION_NAME_SINGLE_SAFE = "singleSafe"
        internal const val FUNCTION_NAME_STREAM = "stream"
        internal const val FUNCTION_NAME_STREAM_SORTED = "streamSorted"
        internal const val FUNCTION_NAME_UPDATE = "update"
        internal const val FUNCTION_NAME_UPDATE_MULTIPLE = "updateMultiple"
        internal const val FUNCTION_NAME_WHERE = "where"
        internal const val FUNCTION_NAME_WHERE_UPDATE = "whereUpdate"
        internal const val PARAM_NAME_COLUMNS_BASE_CLASS = "parent"
        internal const val PARAM_NAME_EXPRESSION = "expression"
        internal const val PARAM_NAME_MAPPED_BY = "mappedBy"
        internal const val PARAM_NAME_SETTER = "setter"
        internal const val PARAM_NAME_SETTERS = "setters"
        internal const val PARAM_NAME_SORT = "sort"
        internal const val PARAM_NAME_TYPE = "type"
        internal const val SUFFIX_CLASS_COLUMNS_BASE = "Base"
        internal const val SUFFIX_FILE_EXTENSIONS = "Extensions"
        internal const val SUFFIX_OBJECT_COLUMNS = "Columns"
        internal const val SUFFIX_PACKAGE_GENERATED = ".generated"
        internal const val TYPE_VARIABLE_NAME_COLUMNS = "Columns"
        //endregion
        //region Names
        internal val HibernatePanacheCompanionBase: String = PanacheCompanionBase::class.java.name
        internal val HibernatePanacheEntityBase: String = PanacheEntityBase::class.java.name
        internal val JakartaPersistenceEntity: String = Entity::class.java.name
        internal val JakartaPersistenceId: String = Id::class.java.name
        internal val JakartaPersistenceJoinColumn: String = JoinColumn::class.java.name
        internal val JakartaPersistenceManyToMany: String = ManyToMany::class.java.name
        internal val JakartaPersistenceOneToMany: String = OneToMany::class.java.name
        internal val JakartaPersistenceOneToOne: String = OneToOne::class.java.name
        internal val JakartaPersistenceTransient: String = Transient::class.java.name
        internal val ProcessorColumnType: String = ColumnType::class.java.name
        //endregion
        //region Options
        internal const val OPTION_ADD_GENERATED_ANNOTATION = "addGeneratedAnnotation"
        //endregion
    }
}
