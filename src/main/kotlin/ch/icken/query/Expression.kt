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

sealed class Expression<Columns> {
    //region Chaining operations
    fun and(expression: Expression<Columns>): Expression<Columns> = LogicalExpression.AndExpression(this, expression)
    fun or(expression: Expression<Columns>): Expression<Columns> = LogicalExpression.OrExpression(this, expression)
    //endregion

    //region compile
    internal fun compile(): Compiled = when (this) {
        is LogicalExpression -> {
            val compiledExpression = compileExpression()
            Compiled("(${compiledExpression.expression})", compiledExpression.parameters)
        }
        else -> compileExpression()
    }
    protected abstract fun compileExpression(): Compiled
    class Compiled internal constructor(val expression: String, val parameters: Map<String, Any>)
    //endregion

    sealed class BooleanExpression<Columns> private constructor(
        protected val key: String,
        protected val operator: String
    ) : Expression<Columns>() {
        sealed class BooleanValueExpression<Columns> private constructor(
            key: String,
            operator: String,
            private val value: Any
        ) : BooleanExpression<Columns>(key, operator) {
            private val parameterName: String = generateParameterName()

            override fun compileExpression() = Compiled(
                expression = "$key $operator :$parameterName",
                parameters = mapOf(parameterName to value)
            )

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

            class Like<Columns> internal constructor(key: String, expression: String) :
                BooleanValueExpression<Columns>(key, "LIKE", expression)
            class NotLike<Columns> internal constructor(key: String, expression: String) :
                BooleanValueExpression<Columns>(key, "NOT LIKE", expression)

        }

        sealed class BetweenExpression<Columns> private constructor(
            key: String,
            operator: String,
            private val min: Any,
            private val maxIncl: Any
        ) : BooleanExpression<Columns>(key, operator) {
            private val minParameterName: String = generateParameterName()
            private val maxInclParameterName: String = generateParameterName()

            override fun compileExpression() = Compiled(
                expression = "$key $operator :$minParameterName AND :$maxInclParameterName",
                parameters = mapOf(minParameterName to min, maxInclParameterName to maxIncl)
            )

            class Between<Columns> internal constructor(key: String, min: Any, maxIncl: Any) :
                BetweenExpression<Columns>(key, "BETWEEN", min, maxIncl)
            class NotBetween<Columns> internal constructor(key: String, min: Any, maxIncl: Any) :
                BetweenExpression<Columns>(key, "NOT BETWEEN", min, maxIncl)
        }

        sealed class IsExpression<Columns> private constructor(
            key: String,
            operator: String
        ) : BooleanExpression<Columns>(key, operator) {
            override fun compileExpression() = Compiled(
                expression = "$key $operator NULL",
                parameters = emptyMap()
            )

            class IsNull<Columns> internal constructor(key: String) :
                IsExpression<Columns>(key, "IS")
            class IsNotNull<Columns> internal constructor(key: String) :
                IsExpression<Columns>(key, "IS NOT")
        }
    }

    sealed class LogicalExpression<Columns> private constructor(
        private val previous: Expression<Columns>,
        private val operator: String,
        private val expression: Expression<Columns>
    ) : Expression<Columns>() {
        override fun compileExpression(): Compiled {
            val compiledPrevious = previous.compileExpression()
            val compiledExpression = expression.compileExpression()
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
