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

@file:Suppress("unused")

package ch.icken.query

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.panache.common.Sort
import org.jboss.logging.Logger

class Order<Entity : PanacheEntityBase, Id : Any, Columns> internal constructor(
    private val companion: PanacheCompanionBase<Entity, Id>,
    private val component: Component.QueryComponent<Entity, Id, Columns>,
    private val expressions: List<OrderByExpression>
) {

    private val sort = expressions.fold(Sort.empty()) { acc, expression ->
        val nullPrecedence = expression.nullPrecedence
        if (nullPrecedence == null) {
            acc.and(expression.columnName, expression.direction)
        } else {
            acc.and(expression.columnName, expression.direction, nullPrecedence)
        }
    }

    internal inline fun <R> withCompiledComponent(block: Component.Compiled.() -> R): R {
        val message = expressions.fold("ORDER BY") { acc, expression ->
            val direction = when (expression.direction) {
                Sort.Direction.Ascending -> "ASC"
                Sort.Direction.Descending -> "DESC"
            }
            val nullPrecedence = when (expression.nullPrecedence) {
                Sort.NullPrecedence.NULLS_FIRST -> " NULLS FIRST"
                Sort.NullPrecedence.NULLS_LAST -> " NULLS LAST"
                null -> ""
            }
            "$acc ${expression.columnName} $direction$nullPrecedence,"
        }.dropLast(1)

        return component.withCompiled {
            LOG.debug(message)
            block()
        }
    }

    //region Terminal operations
    /**
     * Finds entities matching the preceding query, sorted by the columns and directions as defined in `orderBy`.
     *
     * May be used to chain functionality not (yet) abstracted by this library, like
     * [page][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.page] and
     * [project][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.project].
     *
     * @return  a new [PanacheQuery][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery] instance
     * @see     io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.find
     */
    fun find() = withCompiledComponent { companion.find(query, sort, params) }

    /**
     * Streams all entities matching the preceding query, sorted by the columns and directions as defined in `orderBy`.
     * This function is a shortcut for `find().stream()`.
     *
     * WARNING: this function requires a transaction to be active,
     * otherwise the underlying cursor may be closed before the end of the stream.
     *
     * @return  a new [Stream][java.util.stream.Stream] instance containing all results, without paging
     * @see     io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.stream
     */
    fun stream() = withCompiledComponent { companion.stream(query, sort, params) }

    /**
     * Finds all entities matching the preceding query, sorted by the columns and directions as defined in `orderBy`.
     * This function is a shortcut for `find().list()`.
     *
     * @return  a new [List][kotlin.collections.List] instance containing all results, without paging
     * @see     io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.list
     */
    fun multiple() = find().list()
    //endregion

    companion object {
        private val LOG: Logger = Logger.getLogger(Order::class.java)
    }

    class Builder<Columns> {
        private val expressions = mutableListOf<OrderByExpression>()

        internal fun addAscending(columnName: String) {
            expressions.add(OrderByExpression(columnName, Sort.Direction.Ascending))
        }

        internal fun addAscending(columnName: String, nullsFirst: Boolean) {
            expressions.add(OrderByExpression(columnName, Sort.Direction.Ascending, nullsFirst))
        }

        internal fun addDescending(columnName: String) {
            expressions.add(OrderByExpression(columnName, Sort.Direction.Descending))
        }

        internal fun addDescending(columnName: String, nullsFirst: Boolean) {
            expressions.add(OrderByExpression(columnName, Sort.Direction.Descending, nullsFirst))
        }

        fun <Entity: PanacheEntityBase, Id : Any> build(
            companion: PanacheCompanionBase<Entity, Id>,
            component: Component.QueryComponent<Entity, Id, Columns>,
        ) = Order(companion, component, expressions)
    }
}
