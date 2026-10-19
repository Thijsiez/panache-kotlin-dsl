/*
 * Copyright 2024-2026 Thijs Koppen
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

@file:Suppress("unused")

package ch.icken.query

import ch.icken.query.Component.QueryComponent
import ch.icken.query.Component.QueryComponent.InitialQueryComponent
import ch.icken.query.Component.UpdateComponent.SetterUpdateComponent
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import org.jboss.logging.Logger
import org.jetbrains.annotations.ApiStatus

sealed class Component<Entity : PanacheEntityBase, Id : Any, Columns>(
    protected val companion: PanacheCompanionBase<Entity, Id>
) {

    //region compile
    internal abstract fun compile(): Compiled

    class Compiled internal constructor(val query: String, val params: Map<String, Any>)

    internal inline fun <R> withCompiled(block: Compiled.() -> R): R {
        val compiled = compile()
        val message = compiled.params.entries.fold(compiled.query) { acc, param ->
            acc.replace(":${param.key}", param.value.toString())
        }

        LOG.debug(message)
        return compiled.block()
    }
    //endregion

    companion object {
        private val LOG: Logger = Logger.getLogger(Component::class.java)
    }

    sealed class QueryComponent<Entity : PanacheEntityBase, Id : Any, Columns>(
        companion: PanacheCompanionBase<Entity, Id>,
        protected val expression: Expression<Columns>
    ) : Component<Entity, Id, Columns>(companion) {

        //region Intermediate operations
        @ApiStatus.Internal
        fun and(expression: Expression<Columns>): QueryComponent<Entity, Id, Columns> =
            LogicalQueryComponent.AndQueryComponent(companion, this, expression)

        @ApiStatus.Internal
        fun or(expression: Expression<Columns>): QueryComponent<Entity, Id, Columns> =
            LogicalQueryComponent.OrQueryComponent(companion, this, expression)

        @ApiStatus.Internal
        fun orderBy(columns: Columns, expression: context(Order.Builder<Columns>) Columns.() -> Unit):
                Order<Entity, Id, Columns> =
            Order.Builder<Columns>().apply { expression(this, columns) }.build(companion, this)
        //endregion

        //region Terminal operations
        /**
         * Counts the number of entities matching the preceding query.
         *
         * @return  the number of entities counted
         * @see     io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.count
         */
        fun count() = withCompiled { companion.count(query, params) }

        /**
         * Deletes all entities matching the preceding query.
         *
         * WARNING: the default Panache implementation behind this function uses a bulk delete query
         * and ignores cascading rules from the JPA model.
         *
         * @return  the number of entities deleted
         * @see     io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.delete
         */
        fun delete() = withCompiled { companion.delete(query, params) }

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
        fun find() = withCompiled { companion.find(query, params) }

        /**
         * Streams all entities matching the preceding query. This function is a shortcut for `find().stream()`.
         *
         * WARNING: this function requires a transaction to be active,
         * otherwise the underlying cursor may be closed before the end of the stream.
         *
         * @return  a new [Stream][java.util.stream.Stream] instance containing all results, without paging
         * @see     io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.stream
         */
        fun stream() = withCompiled { companion.stream(query, params) }

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
         * - [NoResult][ch.icken.query.SingleResult.NoResult] when there is no result
         * - A new instance of [Result][ch.icken.query.SingleResult.Result] when there is exactly one result
         * - [NotUnique][ch.icken.query.SingleResult.NotUnique] when there are multiple results
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
         * @return  a [SingleResult][ch.icken.query.SingleResult] instance
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
        //endregion

        class InitialQueryComponent<Entity : PanacheEntityBase, Id : Any, Columns> internal constructor(
            companion: PanacheCompanionBase<Entity, Id>,
            expression: Expression<Columns>
        ) : QueryComponent<Entity, Id, Columns>(companion, expression) {

            override fun compile(): Compiled {
                val compiledExpression = expression.compile()
                return Compiled(
                    query = compiledExpression.expression,
                    params = compiledExpression.parameters
                )
            }
        }

        sealed class LogicalQueryComponent<Entity : PanacheEntityBase, Id : Any, Columns>(
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
                    params = compiledPrevious.params + compiledExpression.parameters
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

    sealed class UpdateComponent<Entity : PanacheEntityBase, Id : Any, Columns>(
        companion: PanacheCompanionBase<Entity, Id>
    ) : Component<Entity, Id, Columns>(companion) {

        class SetterUpdateComponent<Entity : PanacheEntityBase, Id : Any, Columns> internal constructor(
            companion: PanacheCompanionBase<Entity, Id>,
            private val columns: Columns,
            private val setters: Array<out Columns.() -> SetterExpression>
        ) : UpdateComponent<Entity, Id, Columns>(companion) {

            //region Intermediate operations
            @ApiStatus.Internal
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
            fun executeWithoutWhere() = withCompiled { companion.update(query, params) }
            //endregion

            override fun compile(): Compiled {
                val compiledSetters = setters.map { it(columns).compile() }
                return Compiled(
                    query = compiledSetters.joinToString { it.assignment },
                    params = compiledSetters.mapNotNull { it.parameter }.associate { it }
                )
            }
        }

        sealed class LogicalUpdateComponent<Entity : PanacheEntityBase, Id : Any, Columns>(
            companion: PanacheCompanionBase<Entity, Id>,
            private val previous: UpdateComponent<Entity, Id, Columns>,
            private val operator: String,
            private val expression: Expression<Columns>
        ) : UpdateComponent<Entity, Id, Columns>(companion) {

            //region Intermediate operations
            @ApiStatus.Internal
            fun and(expression: Expression<Columns>): LogicalUpdateComponent<Entity, Id, Columns> =
                AndUpdateComponent(companion, this, expression)

            @ApiStatus.Internal
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
            fun execute() = withCompiled { companion.update(query, params) }
            //endregion

            override fun compile(): Compiled {
                val compiledPrevious = previous.compile()
                val compiledExpression = expression.compile()
                return Compiled(
                    query = "${compiledPrevious.query} $operator ${compiledExpression.expression}",
                    params = compiledPrevious.params + compiledExpression.parameters
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
        SetterUpdateComponent<Entity, Id, Columns> = SetterUpdateComponent(this, columns, arrayOf(setter))

fun <Entity : PanacheEntityBase, Id : Any, Columns>
        PanacheCompanionBase<Entity, Id>.update(columns: Columns, setters: Array<out Columns.() -> SetterExpression>):
        SetterUpdateComponent<Entity, Id, Columns> = SetterUpdateComponent(this, columns, setters)

fun <Entity : PanacheEntityBase, Id : Any, Columns>
        PanacheCompanionBase<Entity, Id>.where(expression: Expression<Columns>):
        QueryComponent<Entity, Id, Columns> = InitialQueryComponent(this, expression)
