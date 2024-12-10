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
import ch.icken.query.Component.UpdateComponent.InitialUpdateComponent
import ch.icken.query.Component.UpdateComponent.InitialUpdateComponent.SetterExpression
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.panache.common.Sort
import org.jboss.logging.Logger

sealed class Component<Entity : PanacheEntityBase, Id : Any, Columns> private constructor(
    protected val companion: PanacheCompanionBase<Entity, Id>
) {
    //region compile
    internal abstract fun compile(): Compiled
    class Compiled internal constructor(val component: String, val parameters: Map<String, Any>)

    internal inline fun <R> withCompiled(block: Compiled.() -> R): R {
        val compiled = compile()
        LOG.debug(compiled.parameters.entries.fold(compiled.component) { acc, param ->
            acc.replace(":${param.key}", param.value.toString())
        })
        return compiled.block()
    }
    //endregion

    companion object {
        private val LOG: Logger = Logger.getLogger(Component::class.java)
    }

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
        /**
         * Counts the number of entities matching the preceding query.
         *
         * @return  the number of entities counted
         * @see     io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.count
         */
        fun count() = withCompiled { companion.count(component, parameters) }
        /**
         * Deletes all entities matching the preceding query.
         *
         * WARNING: the default Panache implementation behind this function uses a bulk delete query
         * and ignores cascading rules from the JPA model.
         *
         * @return  the number of entities deleted
         * @see     io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.delete
         */
        fun delete() = withCompiled { companion.delete(component, parameters) }
        /**
         * Finds entities matching the preceding query.
         *
         * May be used to chain functionality not (yet) abstracted by this library, like
         * [page][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.page] and
         * [project][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.project].
         *
         * @return  a new [PanacheQuery][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery] instance
         * @see     io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.find
         */
        fun find() = withCompiled { companion.find(component, parameters) }
        /**
         * Finds entities matching the preceding query and the given sort options.
         *
         * May be used to chain functionality not (yet) abstracted by this library, like
         * [page][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.page] and
         * [project][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.project].
         *
         * @param   sort    the sort strategy to use
         * @return          a new [PanacheQuery][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery] instance
         * @see             io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.find
         */
        fun find(sort: Sort) = withCompiled { companion.find(component, sort, parameters) }
        /**
         * Streams all entities matching the preceding query. This function is a shortcut for `find().stream()`.
         *
         * WARNING: this function requires a transaction to be active,
         * otherwise the underlying cursor may be closed before the end of the stream.
         *
         * @return  a new [Stream][java.util.stream.Stream] instance containing all results, without paging
         * @see     io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.stream
         */
        fun stream() = withCompiled { companion.stream(component, parameters) }
        /**
         * Streams all entities matching the preceding query and the given sort options.
         * This function is a shortcut for `find(sort).stream()`.
         *
         * WARNING: this function requires a transaction to be active,
         * otherwise the underlying cursor may be closed before the end of the stream.
         *
         * @param   sort    the sort strategy to use
         * @return          a new [Stream][java.util.stream.Stream] instance containing all results, without paging
         * @see             io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.stream
         */
        fun stream(sort: Sort) = withCompiled { companion.stream(component, sort, parameters) }

        /**
         * Finds a single result matching the preceding query, or throws if there is not exactly one.
         * This function is a shortcut for `find().singleResult()`.
         *
         * @return  the single result
         * @throws  jakarta.persistence.NoResultException when there is no result
         * @throws  jakarta.persistence.NonUniqueResultException when there are multiple results
         * @see     io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.singleResult
         */
        fun single() = find().singleResult()
        /**
         * Finds a single result matching the preceding query, but does not throw if there is not exactly one.
         * This function is a shortcut for `find().singleResultSafe()`.
         *
         * This function will always return one of the following:
         * - [NoResult][ch.icken.query.PanacheSingleResult.NoResult] when there is no result
         * - A new instance of [Result][ch.icken.query.PanacheSingleResult.Result] when there is exactly one result
         * - [NotUnique][ch.icken.query.PanacheSingleResult.NotUnique] when there are multiple results
         *
         * Below is an example of how this result could be used:
         * ```
         * val result = User.where { ... }
         * val user = when (result) {
         *     NoResult -> null //or maybe return
         *     is Result -> result.value
         *     NotUnique -> throw IllegalStateException()
         * }
         * ```
         *
         * @return  a [PanacheSingleResult][ch.icken.query.PanacheSingleResult] instance
         * @see     ch.icken.query.singleResultSafe
         */
        fun singleSafe() = find().singleResultSafe()
        /**
         * Finds all entities matching the preceding query. This function is a shortcut for `find().list()`.
         *
         * @return  a new [List][kotlin.collections.List] instance containing all results, without paging
         * @see     io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.list
         */
        fun multiple() = find().list()
        /**
         * Finds all entities matching the preceding query and the given sort options.
         * This function is a shortcut for `find(sort).list()`.
         *
         * @param   sort    the sort strategy to use
         * @return          a new [List][kotlin.collections.List] instance containing all results, without paging
         * @see             io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.list
         */
        fun multiple(sort: Sort) = find(sort).list()
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
            private val setters: Array<out Columns.() -> SetterExpression>
        ) : UpdateComponent<Entity, Id, Columns>(companion) {
            //region Chaining operations
            fun where(expression: Expression<Columns>): LogicalUpdateComponent<Entity, Id, Columns> =
                LogicalUpdateComponent.WhereUpdateComponent(companion, this, expression)
            //endregion

            //region Terminal operations
            /**
             * Updates all entities of this type.
             *
             * WARNING: this function updates ALL entities without a WHERE clause.
             *
             * WARNING: this function requires a transaction to be active.
             *
             * @return  the number of entities updated
             * @see     io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.update
             */
            fun executeWithoutWhere() = withCompiled { companion.update(component, parameters) }
            //endregion

            override fun compile(): Compiled {
                val compiledSetters = setters.map { it(columns).compile() }
                return Compiled(
                    component = compiledSetters.joinToString { it.assignment },
                    parameters = compiledSetters.mapNotNull { it.parameter }.associate { it }
                )
            }

            class SetterExpression internal constructor(private val columnName: String, private val value: Any?) {
                private val parameterName: String = generateParameterName()

                internal fun compile(): Compiled = when (value) {
                    null -> Compiled("$columnName = null", null)
                    else -> Compiled("$columnName = :$parameterName", parameterName to value)
                }
                class Compiled internal constructor(val assignment: String, val parameter: Pair<String, Any>?)
            }
        }

        sealed class LogicalUpdateComponent<Entity : PanacheEntityBase, Id : Any, Columns> private constructor(
            companion: PanacheCompanionBase<Entity, Id>,
            private val previous: UpdateComponent<Entity, Id, Columns>,
            private val operator: String,
            private val expression: Expression<Columns>
        ) : UpdateComponent<Entity, Id, Columns>(companion) {
            //region Chaining operations
            fun and(expression: Expression<Columns>): LogicalUpdateComponent<Entity, Id, Columns> =
                AndUpdateComponent(companion, this, expression)
            fun or(expression: Expression<Columns>): LogicalUpdateComponent<Entity, Id, Columns> =
                OrUpdateComponent(companion, this, expression)
            //endregion

            //region Terminal operations
            /**
             * Updates all entities matching the preceding query.
             *
             * WARNING: this function requires a transaction to be active.
             *
             * @return  the number of entities updated
             * @see     io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.update
             */
            fun execute() = withCompiled { companion.update(component, parameters) }
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
        PanacheCompanionBase<Entity, Id>.update(columns: Columns, setter: Columns.() -> SetterExpression):
        InitialUpdateComponent<Entity, Id, Columns> = InitialUpdateComponent(this, columns, arrayOf(setter))
fun <Entity : PanacheEntityBase, Id : Any, Columns>
        PanacheCompanionBase<Entity, Id>.update(columns: Columns, setters: Array<out Columns.() -> SetterExpression>):
        InitialUpdateComponent<Entity, Id, Columns> = InitialUpdateComponent(this, columns, setters)

fun <Entity : PanacheEntityBase, Id : Any, Columns>
        PanacheCompanionBase<Entity, Id>.where(expression: Expression<Columns>):
        QueryComponent<Entity, Id, Columns> = InitialQueryComponent(this, expression)
