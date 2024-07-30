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

@file:Suppress("unused")

package ch.icken.query

import ch.icken.query.BooleanExpression.BetweenExpression.Between
import ch.icken.query.BooleanExpression.BetweenExpression.NotBetween
import ch.icken.query.BooleanExpression.BooleanValueExpression.*
import ch.icken.query.BooleanExpression.IsExpression.IsNotNull
import ch.icken.query.BooleanExpression.IsExpression.IsNull

class Column<T : Any?>(internal val name: String)

//region eq
private fun eq(name: String, value: Any?) = if (value == null) IsNull(name) else EqualTo(name, value)
@JvmName("eq")
infix fun <T : Any> Column<T>.eq(value: T) = eq(name, value)
@JvmName("eqNullable")
infix fun <T : Any> Column<T?>.eq(value: T?) = eq(name, value)
//endregion
//region neq
private fun neq(name: String, value: Any?) = if (value == null) IsNotNull(name) else NotEqualTo(name, value)
@JvmName("neq")
infix fun <T : Any> Column<T>.neq(value: T) = neq(name, value)
@JvmName("neqNullable")
infix fun <T : Any> Column<T?>.neq(value: T?) = neq(name, value)
//endregion
//region lt
private fun lt(name: String, value: Any) = LessThan(name, value)
@JvmName("lt")
infix fun <T : Any> Column<T>.lt(value: T) = lt(name, value)
@JvmName("ltNullable")
infix fun <T : Any> Column<T?>.lt(value: T) = lt(name, value)
//endregion
//region gt
private fun gt(name: String, value: Any) = GreaterThan(name, value)
@JvmName("gt")
infix fun <T : Any> Column<T>.gt(value: T) = gt(name, value)
@JvmName("gtNullable")
infix fun <T : Any> Column<T?>.gt(value: T) = gt(name, value)
//endregion
//region lte
private fun lte(name: String, value: Any) = LessThanOrEqualTo(name, value)
@JvmName("lte")
infix fun <T : Any> Column<T>.lte(value: T) = lte(name, value)
@JvmName("lteNullable")
infix fun <T : Any> Column<T?>.lte(value: T) = lte(name, value)
//endregion
//region gte
private fun gte(name: String, value: Any) = GreaterThanOrEqualTo(name, value)
@JvmName("gte")
infix fun <T : Any> Column<T>.gte(value: T) = gte(name, value)
@JvmName("gteNullable")
infix fun <T : Any> Column<T?>.gte(value: T) = gte(name, value)
//endregion

//region in
private fun `in`(name: String, values: Collection<Any>) = In(name, values)
@JvmName("in")
infix fun <T : Any> Column<T>.`in`(values: Collection<T>) = `in`(name, values)
@JvmName("inNullable")
infix fun <T : Any> Column<T?>.`in`(values: Collection<T>) = `in`(name, values)
//endregion
//region notIn
private fun notIn(name: String, values: Collection<Any>) = NotIn(name, values)
@JvmName("notIn")
infix fun <T : Any> Column<T>.notIn(values: Collection<T>) = notIn(name, values)
@JvmName("notInNullable")
infix fun <T : Any> Column<T?>.notIn(values: Collection<T>) = notIn(name, values)
//endregion

//region like
private fun like(name: String, expression: String?) =
    if (expression == null) IsNull(name) else Like(name, expression)
@JvmName("like")
infix fun Column<String>.like(expression: String) = like(name, expression)
@JvmName("likeNullable")
infix fun Column<String?>.like(expression: String?) = like(name, expression)
//endregion
//region notLike
private fun notLike(name: String, expression: String?) =
    if (expression == null) IsNotNull(name) else NotLike(name, expression)
@JvmName("notLike")
infix fun Column<String>.notLike(expression: String) = notLike(name, expression)
@JvmName("notLikeNullable")
infix fun Column<String?>.notLike(expression: String?) = notLike(name, expression)
//endregion

//region between
private fun between(name: String, min: Any?, maxIncl: Any?) = when {
    min != null && maxIncl != null -> Between(name, min, maxIncl)
    min != null && maxIncl == null -> GreaterThanOrEqualTo(name, min)
    min == null && maxIncl != null -> LessThanOrEqualTo(name, maxIncl)
    else -> IsNull(name)
}
@JvmName("between")
fun <T : Any> Column<T>.between(min: T, maxIncl: T) = between(name, min, maxIncl)
@JvmName("betweenNullable")
fun <T : Any> Column<T?>.between(min: T?, maxIncl: T?) = between(name, min, maxIncl)
//endregion
//region notBetween
private fun notBetween(name: String, min: Any?, maxIncl: Any?) = when {
    min != null && maxIncl != null -> NotBetween(name, min, maxIncl)
    min != null && maxIncl == null -> LessThan(name, min)
    min == null && maxIncl != null -> GreaterThan(name, maxIncl)
    else -> IsNotNull(name)
}
@JvmName("notBetween")
fun <T : Any> Column<T>.notBetween(min: T, maxIncl: T) = notBetween(name, min, maxIncl)
@JvmName("notBetweenNullable")
fun <T : Any> Column<T?>.notBetween(min: T?, maxIncl: T?) = notBetween(name, min, maxIncl)
//endregion
