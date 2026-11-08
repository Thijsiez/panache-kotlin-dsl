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

package ch.icken.query

import io.quarkus.panache.common.Sort
import org.jetbrains.annotations.ApiStatus

sealed class Expression<Columns> {

    //region Intermediate operations
    @ApiStatus.Internal
    fun and(expression: Expression<Columns>): Expression<Columns> = LogicalExpression.AndExpression(this, expression)

    @ApiStatus.Internal
    fun or(expression: Expression<Columns>): Expression<Columns> = LogicalExpression.OrExpression(this, expression)
    //endregion

    //region compile
    internal open fun compile(pn: ParameterName): Compiled = compileExpression(pn)

    protected abstract fun compileExpression(pn: ParameterName): Compiled

    class Compiled internal constructor(val expression: String, val parameters: Map<String, Any>)
    //endregion

    sealed class BooleanExpression<Columns>(
        protected val key: String,
        protected val operator: String
    ) : Expression<Columns>() {

        sealed class BooleanValueExpression<Columns>(
            key: String,
            operator: String,
            private val value: Any
        ) : BooleanExpression<Columns>(key, operator) {

            override fun compileExpression(pn: ParameterName): Compiled {
                val parameterName = pn.next()
                return Compiled(
                    expression = "$key $operator :$parameterName",
                    parameters = mapOf(parameterName to value)
                )
            }

            class EqualTo<Columns> internal constructor(key: String, value: Any) :
                BooleanValueExpression<Columns>(key, "=", value)

            class NotEqualTo<Columns> internal constructor(key: String, value: Any) :
                BooleanValueExpression<Columns>(key, "!=", value)

            class LessThan<Columns> internal constructor(key: String, value: Any) :
                BooleanValueExpression<Columns>(key, "<", value)

            class GreaterThan<Columns> internal constructor(key: String, value: Any) :
                BooleanValueExpression<Columns>(key, ">", value)

            class LessThanOrEqualTo<Columns> internal constructor(key: String, value: Any) :
                BooleanValueExpression<Columns>(key, "<=", value)

            class GreaterThanOrEqualTo<Columns> internal constructor(key: String, value: Any) :
                BooleanValueExpression<Columns>(key, ">=", value)

            class In<Columns> internal constructor(key: String, values: Collection<Any>) :
                BooleanValueExpression<Columns>(key, "IN", values)

            class NotIn<Columns> internal constructor(key: String, values: Collection<Any>) :
                BooleanValueExpression<Columns>(key, "NOT IN", values)

            class Like<Columns> internal constructor(key: String, pattern: String) :
                BooleanValueExpression<Columns>(key, "LIKE", pattern)

            class NotLike<Columns> internal constructor(key: String, pattern: String) :
                BooleanValueExpression<Columns>(key, "NOT LIKE", pattern)
        }

        sealed class BetweenExpression<Columns>(
            key: String,
            operator: String,
            private val min: Any,
            private val maxIncl: Any
        ) : BooleanExpression<Columns>(key, operator) {

            override fun compileExpression(pn: ParameterName): Compiled {
                val minParameterName = pn.next()
                val maxInclParameterName = pn.next()
                return Compiled(
                    expression = "$key $operator :$minParameterName AND :$maxInclParameterName",
                    parameters = mapOf(minParameterName to min, maxInclParameterName to maxIncl)
                )
            }

            class Between<Columns> internal constructor(key: String, min: Any, maxIncl: Any) :
                BetweenExpression<Columns>(key, "BETWEEN", min, maxIncl)

            class NotBetween<Columns> internal constructor(key: String, min: Any, maxIncl: Any) :
                BetweenExpression<Columns>(key, "NOT BETWEEN", min, maxIncl)
        }

        sealed class IsExpression<Columns>(
            key: String,
            operator: String
        ) : BooleanExpression<Columns>(key, operator) {

            override fun compileExpression(pn: ParameterName) = Compiled(
                expression = "$key $operator NULL",
                parameters = emptyMap()
            )

            class IsNull<Columns> internal constructor(key: String) :
                IsExpression<Columns>(key, "IS")

            class IsNotNull<Columns> internal constructor(key: String) :
                IsExpression<Columns>(key, "IS NOT")
        }
    }

    sealed class LogicalExpression<Columns>(
        private val previous: Expression<Columns>,
        private val operator: String,
        private val expression: Expression<Columns>
    ) : Expression<Columns>() {

        override fun compile(pn: ParameterName): Compiled {
            val compiledExpression = compileExpression(pn)
            return Compiled(
                expression = "(${compiledExpression.expression})",
                parameters = compiledExpression.parameters
            )
        }

        override fun compileExpression(pn: ParameterName): Compiled {
            val compiledPrevious = previous.compileExpression(pn)
            val compiledExpression = expression.compileExpression(pn)
            return Compiled(
                expression = "${compiledPrevious.expression} $operator ${compiledExpression.expression}",
                parameters = compiledPrevious.parameters + compiledExpression.parameters
            )
        }

        class AndExpression<Columns> internal constructor(
            previous: Expression<Columns>,
            expression: Expression<Columns>
        ) : LogicalExpression<Columns>(previous, "AND", expression)

        class OrExpression<Columns> internal constructor(
            previous: Expression<Columns>,
            expression: Expression<Columns>
        ) : LogicalExpression<Columns>(previous, "OR", expression)
    }
}

class OrderByExpression internal constructor(
    internal val columnName: String,
    internal val direction: Sort.Direction,
    private val nullsFirst: Boolean? = null
) {

    val nullPrecedence: Sort.NullPrecedence?
        get() = when (nullsFirst) {
            true -> Sort.NullPrecedence.NULLS_FIRST
            false -> Sort.NullPrecedence.NULLS_LAST
            null -> null
        }
}

class SetterExpression internal constructor(
    private val columnName: String,
    private val value: Any?
) {

    internal fun compile(pn: ParameterName): Compiled = when (value) {
        null -> Compiled(
            assignment = "$columnName = null",
            parameter = null
        )
        else -> {
            val parameterName = pn.next()
            Compiled(
                assignment = "$columnName = :$parameterName",
                parameter = parameterName to value
            )
        }
    }

    class Compiled internal constructor(val assignment: String, val parameter: Pair<String, Any>?)
}

class ParameterName {

    private var count = 0

    fun next() = "p${++count}"
}
