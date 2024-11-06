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

import ch.icken.query.Component.QueryComponent
import ch.icken.query.Component.QueryComponent.InitialQueryComponent
import ch.icken.query.Component.UpdateComponent
import ch.icken.query.Component.UpdateComponent.InitialUpdateComponent
import ch.icken.query.Component.UpdateComponent.InitialUpdateComponent.Setter
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.panache.common.Sort

sealed class Component<Entity : PanacheEntityBase, Id : Any, Columns> private constructor(
    protected val companion: PanacheCompanionBase<Entity, Id>
) {
    //region compile
    internal abstract fun compile(): Compiled
    data class Compiled internal constructor(val component: String, val parameters: Map<String, Any>)
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
        fun count() = with(compile()) { companion.count(component, parameters) }
        fun delete() = with(compile()) { companion.delete(component, parameters) }
        fun find() = with(compile()) { companion.find(component, parameters) }
        fun find(sort: Sort) = with(compile()) { companion.find(component, sort, parameters) }
        fun stream() = with(compile()) { companion.stream(component, parameters) }
        fun stream(sort: Sort) = with(compile()) { companion.stream(component, sort, parameters) }

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
                    component = "${compiledPrevious.component} $operator ${compiledExpression.expression}",
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

    sealed class UpdateComponent<Entity : PanacheEntityBase, Id : Any, Columns> private constructor(
        companion: PanacheCompanionBase<Entity, Id>
    ) : Component<Entity, Id, Columns>(companion) {
        class InitialUpdateComponent<Entity : PanacheEntityBase, Id : Any, Columns> internal constructor(
            companion: PanacheCompanionBase<Entity, Id>,
            private val columns: Columns,
            private val setters: Array<out Columns.() -> Setter>
        ) : UpdateComponent<Entity, Id, Columns>(companion) {
            //region Chaining operations
            fun where(expression: Expression<Columns>): UpdateComponent<Entity, Id, Columns> =
                LogicalUpdateComponent.WhereUpdateComponent(companion, this, expression)
            //endregion

            //region Terminal operations
            //TODO execute on all rows
            //endregion

            override fun compile(): Compiled {
                val compiledSetters = setters.map { it(columns).compile() }
                return Compiled(
                    component = compiledSetters.joinToString { it.assignment },
                    parameters = compiledSetters.mapNotNull { it.parameter }.associate { it }
                )
            }

            data class Setter internal constructor(val columnName: String, val value: Any?) {
                private val parameterName: String = generateParameterName()

                internal fun compile(): Compiled = when (value) {
                    null -> Compiled("$columnName = null", null)
                    else -> Compiled("$columnName = $parameterName", parameterName to value)
                }
                data class Compiled internal constructor(val assignment: String, val parameter: Pair<String, Any>?)
            }
        }

        sealed class LogicalUpdateComponent<Entity : PanacheEntityBase, Id : Any, Columns> private constructor(
            companion: PanacheCompanionBase<Entity, Id>,
            private val previous: UpdateComponent<Entity, Id, Columns>,
            private val operator: String,
            private val expression: Expression<Columns>
        ) : UpdateComponent<Entity, Id, Columns>(companion) {
            //region Chaining operations
            fun and(expression: Expression<Columns>): UpdateComponent<Entity, Id, Columns> =
                AndUpdateComponent(companion, this, expression)
            fun or(expression: Expression<Columns>): UpdateComponent<Entity, Id, Columns> =
                OrUpdateComponent(companion, this, expression)
            //endregion

            //region Terminal operations
            //TODO execute
            //endregion

            override fun compile(): Compiled {
                val compiledPrevious = previous.compile()
                val compiledExpression = expression.compile()
                return Compiled(
                    component = "${compiledPrevious.component} $operator ${compiledExpression.expression}",
                    parameters = compiledPrevious.parameters + compiledExpression.parameters
                )
            }

            class AndUpdateComponent<Entity : PanacheEntityBase, Id : Any, Columns> internal constructor(
                companion: PanacheCompanionBase<Entity, Id>,
                previous: UpdateComponent<Entity, Id, Columns>,
                expression: Expression<Columns>
            ) : LogicalUpdateComponent<Entity, Id, Columns>(companion, previous, "AND", expression)
            class OrUpdateComponent<Entity : PanacheEntityBase, Id : Any, Columns> internal constructor(
                companion: PanacheCompanionBase<Entity, Id>,
                previous: UpdateComponent<Entity, Id, Columns>,
                expression: Expression<Columns>
            ) : LogicalUpdateComponent<Entity, Id, Columns>(companion, previous, "OR", expression)
            class WhereUpdateComponent<Entity : PanacheEntityBase, Id : Any, Columns> internal constructor(
                companion: PanacheCompanionBase<Entity, Id>,
                previous: UpdateComponent<Entity, Id, Columns>,
                expression: Expression<Columns>
            ) : LogicalUpdateComponent<Entity, Id, Columns>(companion, previous, "WHERE", expression)
        }
    }
}

fun <Entity : PanacheEntityBase, Id : Any, Columns>
        PanacheCompanionBase<Entity, Id>.update(columns: Columns, setter: Columns.() -> Setter):
        UpdateComponent<Entity, Id, Columns> = InitialUpdateComponent(this, columns, arrayOf(setter))
fun <Entity : PanacheEntityBase, Id : Any, Columns>
        PanacheCompanionBase<Entity, Id>.update(columns: Columns, setters: Array<out Columns.() -> Setter>):
        UpdateComponent<Entity, Id, Columns> = InitialUpdateComponent(this, columns, setters)

fun <Entity : PanacheEntityBase, Id : Any, Columns>
        PanacheCompanionBase<Entity, Id>.where(expression: Expression<Columns>):
        QueryComponent<Entity, Id, Columns> = InitialQueryComponent(this, expression)
