/*
 * Copyright 2023 Thijs Koppen
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

sealed class BooleanExpression private constructor(
    protected val key: String,
    protected val operator: String
) {
    abstract fun compile(): Compiled
    data class Compiled internal constructor(
        val expression: String,
        val parameters: Map<String, Any>
    )

    companion object {
        private const val CHARS = //"0123456789" +
                "abcdefghijklmnopqrstuvwxyz" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        protected val uniqueParameterName: String
            get() = (0 ..< 8)
                .map { CHARS[Random.nextInt(CHARS.length)] }
                .toCharArray().concatToString()
    }

    sealed class BooleanValueExpression private constructor(
        key: String,
        operator: String,
        private val value: Any
    ) : BooleanExpression(key, operator) {
        private val parameterName: String = uniqueParameterName

        override fun compile() = Compiled(
            expression = "$key $operator :$parameterName",
            parameters = mapOf(parameterName to value)
        )

        class EqualTo internal constructor(key: String, value: Any) :
            BooleanValueExpression(key, "=", value)
        class NotEqualTo internal constructor(key: String, value: Any) :
            BooleanValueExpression(key, "!=", value)
        class LessThan internal constructor(key: String, value: Any) :
            BooleanValueExpression(key, "<", value)
        class GreaterThan internal constructor(key: String, value: Any) :
            BooleanValueExpression(key, ">", value)
        class LessThanOrEqualTo internal constructor(key: String, value: Any) :
            BooleanValueExpression(key, "<=", value)
        class GreaterThanOrEqualTo internal constructor(key: String, value: Any) :
            BooleanValueExpression(key, ">=", value)

        class In internal constructor(key: String, values: Collection<Any>) :
            BooleanValueExpression(key, "IN", values)
        class NotIn internal constructor(key: String, values: Collection<Any>) :
            BooleanValueExpression(key, "NOT IN", values)

        class Like internal constructor(key: String, expression: String) :
            BooleanValueExpression(key, "LIKE", expression)
        class NotLike internal constructor(key: String, expression: String) :
            BooleanValueExpression(key, "NOT LIKE", expression)

    }

    sealed class BetweenExpression private constructor(
        key: String,
        operator: String,
        private val min: Any,
        private val maxIncl: Any
    ) : BooleanExpression(key, operator) {
        private val minParameterName: String = uniqueParameterName
        private val maxInclParameterName: String = uniqueParameterName

        override fun compile() = Compiled(
            expression = "$key $operator :$minParameterName AND :$maxInclParameterName",
            parameters = mapOf(minParameterName to min, maxInclParameterName to maxIncl)
        )

        class Between internal constructor(key: String, min: Any, maxIncl: Any) :
            BetweenExpression(key, "BETWEEN", min, maxIncl)
        class NotBetween internal constructor(key: String, min: Any, maxIncl: Any) :
            BetweenExpression(key, "NOT BETWEEN", min, maxIncl)
    }

    sealed class IsExpression private constructor(
        key: String,
        operator: String
    ) : BooleanExpression(key, operator) {
        override fun compile() = Compiled(
            expression = "$key $operator NULL",
            parameters = emptyMap()
        )

        class IsNull internal constructor(key: String) :
            IsExpression(key, "IS")
        class IsNotNull internal constructor(key: String) :
            IsExpression(key, "IS NOT")
    }
}
