/*
 * Copyright 2023-2024 Thijs Koppen
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

import kotlin.random.Random

sealed class BooleanExpression<Columns> private constructor(
    protected val key: String,
    protected val operator: String
) : Expression<Columns>() {
    companion object {
        private const val CHARS = //"0123456789" +
                "abcdefghijklmnopqrstuvwxyz" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        protected fun generateParameterName() = (0 ..< 8)
            .map { CHARS[Random.nextInt(CHARS.length)] }
            .toCharArray().concatToString()
    }

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
