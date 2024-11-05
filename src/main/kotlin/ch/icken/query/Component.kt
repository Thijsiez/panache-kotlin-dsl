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

package ch.icken.query

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.panache.common.Sort

sealed class Component<Entity : PanacheEntityBase, Id : Any, Columns> private constructor(
    protected val companion: PanacheCompanionBase<Entity, Id>
) {
    //region compile
    internal abstract fun compile(): Compiled
    data class Compiled internal constructor(val query: String, val parameters: Map<String, Any>)
    //endregion

    sealed class QueryComponent<Entity : PanacheEntityBase, Id : Any, Columns> private constructor(
        companion: PanacheCompanionBase<Entity, Id>,
        protected val expression: Expression<Columns>
    ) : Component<Entity, Id, Columns>(companion) {
        //region Chaining operations
        fun and(expression: Expression<Columns>): QueryComponent<Entity, Id, Columns> =
            LogicalQueryComponent.AndQueryComponent(companion, this, expression)
        fun or(expression: Expression<Columns>): QueryComponent<Entity, Id, Columns> =
            LogicalQueryComponent.OrQueryComponent(companion, this, expression)
        //endregion

        //region Terminal operations
        fun count() = with(compile()) { companion.count(query, parameters) }
        fun delete() = with(compile()) { companion.delete(query, parameters) }
        fun find() = with(compile()) { companion.find(query, parameters) }
        fun find(sort: Sort) = with(compile()) { companion.find(query, sort, parameters) }
        fun stream() = with(compile()) { companion.stream(query, parameters) }
        fun stream(sort: Sort) = with(compile()) { companion.stream(query, sort, parameters) }

        fun getSingle() = find().singleResult()
        fun getSingleSafe() = find().singleResultSafe()
        fun getMultiple() = find().list()
        fun getMultiple(sort: Sort) = find(sort).list()
        //endregion

        class InitialQueryComponent<Entity : PanacheEntityBase, Id : Any, Columns> internal constructor(
            companion: PanacheCompanionBase<Entity, Id>,
            expression: Expression<Columns>
        ) : QueryComponent<Entity, Id, Columns>(companion, expression) {
            override fun compile(): Compiled = expression.compile().let {
                Compiled(it.expression, it.parameters)
            }
        }

        sealed class LogicalQueryComponent<Entity : PanacheEntityBase, Id : Any, Columns> private constructor(
            companion: PanacheCompanionBase<Entity, Id>,
            private val previous: QueryComponent<Entity, Id, Columns>,
            private val operator: String,
            expression: Expression<Columns>
        ) : QueryComponent<Entity, Id, Columns>(companion, expression) {
            override fun compile(): Compiled {
                val compiledPrevious = previous.compile()
                val compiledExpression = expression.compile()
                return Compiled(
                    query = "${compiledPrevious.query} $operator ${compiledExpression.expression}",
                    parameters = compiledPrevious.parameters + compiledExpression.parameters
                )
            }

            class AndQueryComponent<Entity : PanacheEntityBase, Id : Any, Columns> internal constructor(
                companion: PanacheCompanionBase<Entity, Id>,
                previous: QueryComponent<Entity, Id, Columns>,
                expression: Expression<Columns>
            ) : LogicalQueryComponent<Entity, Id, Columns>(companion, previous, "AND", expression)
            class OrQueryComponent<Entity : PanacheEntityBase, Id : Any, Columns> internal constructor(
                companion: PanacheCompanionBase<Entity, Id>,
                previous: QueryComponent<Entity, Id, Columns>,
                expression: Expression<Columns>
            ) : LogicalQueryComponent<Entity, Id, Columns>(companion, previous, "OR", expression)
        }
    }

    class UpdateComponent<Entity : PanacheEntityBase, Id : Any, Columns> internal constructor(
        companion: PanacheCompanionBase<Entity, Id>,
        private val columns: Columns
        //TODO setter providers
    ) : Component<Entity, Id, Columns>(companion) {
        override fun compile(): Compiled {
            TODO("Not yet implemented")
        }

        //TODO where

        data class Setter internal constructor(val key: String, val value: Any?)
    }
}

fun <Entity : PanacheEntityBase, Id : Any, Columns>
        PanacheCompanionBase<Entity, Id>.update(columns: Columns)://TODO setter providers
        Component.UpdateComponent<Entity, Id, Columns> = Component.UpdateComponent(this, columns)

fun <Entity : PanacheEntityBase, Id : Any, Columns>
        PanacheCompanionBase<Entity, Id>.where(expression: Expression<Columns>):
        Component.QueryComponent<Entity, Id, Columns> =
    Component.QueryComponent.InitialQueryComponent(this, expression)
