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

sealed class BooleanExpression private constructor(
    private val key: String,
    private val operator: String,
    private val value: String
) {
    fun compile() = "$key $operator $value"

    class EqualTo internal constructor(key: String, value: String) : BooleanExpression(key, "=", value)
    class NotEqualTo internal constructor(key: String, value: String) : BooleanExpression(key, "!=", value)
    class LessThan internal constructor(key: String, value: String) : BooleanExpression(key, "<", value)
    class GreaterThan internal constructor(key: String, value: String) : BooleanExpression(key, ">", value)
    class LessThanOrEqualTo internal constructor(key: String, value: String) : BooleanExpression(key, "<=", value)
    class GreaterThanOrEqualTo internal constructor(key: String, value: String) : BooleanExpression(key, ">=", value)

    class IsNull internal constructor(key: String) : BooleanExpression(key, "IS", "NULL")
    class IsNotNull internal constructor(key: String) : BooleanExpression(key, "IS NOT", "NULL")

    class Like internal constructor(key: String, expression: String) : BooleanExpression(key, "LIKE", expression)
    class NotLike internal constructor(key: String, expression: String) : BooleanExpression(key, "NOT LIKE", expression)

    class Between internal constructor(key: String, a: String, b: String) :
        BooleanExpression(key, "BETWEEN", "$a AND $b")
    class NotBetween internal constructor(key: String, a: String, b: String) :
        BooleanExpression(key, "NOT BETWEEN", "$a AND $b")

    class In internal constructor(key: String, values: Collection<String>) :
        BooleanExpression(key, "IN", "(${values.joinToString()})")
    class NotIn internal constructor(key: String, values: Collection<String>) :
        BooleanExpression(key, "NOT IN", "(${values.joinToString()})")
}
