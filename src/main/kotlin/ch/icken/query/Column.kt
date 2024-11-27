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

import ch.icken.query.Expression.BooleanExpression.BetweenExpression.Between
import ch.icken.query.Expression.BooleanExpression.BetweenExpression.NotBetween
import ch.icken.query.Expression.BooleanExpression.BooleanValueExpression.*
import ch.icken.query.Expression.BooleanExpression.IsExpression.IsNotNull
import ch.icken.query.Expression.BooleanExpression.IsExpression.IsNull

class Column<Columns, Type : Any?>(internal val name: String) {
    /**
     * Creates a [SetterExpression][ch.icken.query.Component.UpdateComponent.InitialUpdateComponent.SetterExpression]
     * for this column
     *
     * @param   value   the new value for this column
     * @return          a new `SetterExpression` instance
     */
    infix fun set(value: Type) = Component.UpdateComponent.InitialUpdateComponent.SetterExpression(name, value)
}

//region eq
private fun <Columns> eq(name: String, value: Any?): Expression<Columns> =
    if (value == null) IsNull(name) else EqualTo(name, value)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the '=' operator.
 *
 * @param   value   the value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("eq")
infix fun <Columns, Type : Any> Column<Columns, Type>.eq(value: Type) = eq<Columns>(name, value)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the '=' operator when `value` is not null,
 * or using 'IS NULL' when `value` is null.
 *
 * @param   value   the value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("eqNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.eq(value: Type?) = eq<Columns>(name, value)
//endregion
//region neq
private fun <Columns> neq(name: String, value: Any?): Expression<Columns> =
    if (value == null) IsNotNull(name) else NotEqualTo(name, value)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the '!=' operator.
 *
 * @param   value   the value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("neq")
infix fun <Columns, Type : Any> Column<Columns, Type>.neq(value: Type) = neq<Columns>(name, value)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the '!=' operator when `value` is not null,
 * or using 'IS NOT NULL' when `value` is not null.
 *
 * @param   value   the value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("neqNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.neq(value: Type?) = neq<Columns>(name, value)
//endregion
//region lt
private fun <Columns> lt(name: String, value: Any): Expression<Columns> = LessThan(name, value)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the '<' operator.
 *
 * @param   value   the value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("lt")
infix fun <Columns, Type : Any> Column<Columns, Type>.lt(value: Type) = lt<Columns>(name, value)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the '<' operator.
 *
 * @param   value   the value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("ltNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.lt(value: Type) = lt<Columns>(name, value)
//endregion
//region gt
private fun <Columns> gt(name: String, value: Any): Expression<Columns> = GreaterThan(name, value)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the '>' operator.
 *
 * @param   value   the value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("gt")
infix fun <Columns, Type : Any> Column<Columns, Type>.gt(value: Type) = gt<Columns>(name, value)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the '>' operator.
 *
 * @param   value   the value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("gtNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.gt(value: Type) = gt<Columns>(name, value)
//endregion
//region lte
private fun <Columns> lte(name: String, value: Any): Expression<Columns> = LessThanOrEqualTo(name, value)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the '<=' operator.
 *
 * @param   value   the value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("lte")
infix fun <Columns, Type : Any> Column<Columns, Type>.lte(value: Type) = lte<Columns>(name, value)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the '<=' operator.
 *
 * @param   value   the value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("lteNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.lte(value: Type) = lte<Columns>(name, value)
//endregion
//region gte
private fun <Columns> gte(name: String, value: Any): Expression<Columns> = GreaterThanOrEqualTo(name, value)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the '>=' operator.
 *
 * @param   value   the value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("gte")
infix fun <Columns, Type : Any> Column<Columns, Type>.gte(value: Type) = gte<Columns>(name, value)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the '>=' operator.
 *
 * @param   value   the value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("gteNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.gte(value: Type) = gte<Columns>(name, value)
//endregion

//region in
private fun <Columns> `in`(name: String, values: Collection<Any>): Expression<Columns> = In(name, values)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the 'IN' operator.
 *
 * @param   values  the values to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("in")
infix fun <Columns, Type : Any> Column<Columns, Type>.`in`(values: Collection<Type>) = `in`<Columns>(name, values)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the 'IN' operator.
 *
 * @param   values  the values to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("inNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.`in`(values: Collection<Type>) = `in`<Columns>(name, values)
//endregion
//region notIn
private fun <Columns> notIn(name: String, values: Collection<Any>): Expression<Columns> = NotIn(name, values)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the 'NOT IN' operator.
 *
 * @param   values  the values to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("notIn")
infix fun <Columns, Type : Any> Column<Columns, Type>.notIn(values: Collection<Type>) = notIn<Columns>(name, values)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the 'NOT IN' operator.
 *
 * @param   values  the values to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("notInNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.notIn(values: Collection<Type>) = notIn<Columns>(name, values)
//endregion

//region like
private fun <Columns> like(name: String, pattern: String?): Expression<Columns> =
    if (pattern == null) IsNull(name) else Like(name, pattern)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the 'LIKE' operator.
 *
 * @param   pattern the pattern to be matched
 * @return          a new `Expression` instance
 */
@JvmName("like")
infix fun <Columns> Column<Columns, String>.like(pattern: String) = like<Columns>(name, pattern)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the 'LIKE' operator when `pattern` is not null,
 * or using 'IS NULL' when `pattern` is null.
 *
 * @param   pattern the pattern to be matched
 * @return          a new `Expression` instance
 */
@JvmName("likeNullable")
infix fun <Columns> Column<Columns, String?>.like(pattern: String?) = like<Columns>(name, pattern)
//endregion
//region notLike
private fun <Columns> notLike(name: String, pattern: String?): Expression<Columns> =
    if (pattern == null) IsNotNull(name) else NotLike(name, pattern)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the 'NOT LIKE' operator.
 *
 * @param   pattern the pattern to be matched
 * @return          a new `Expression` instance
 */
@JvmName("notLike")
infix fun <Columns> Column<Columns, String>.notLike(pattern: String) = notLike<Columns>(name, pattern)
/**
 * Creates an [Expression][ch.icken.query.Expression] using the 'NOT LIKE' operator when `pattern` is not null,
 * or using 'IS NOT NULL' when `pattern` is not null.
 *
 * @param   pattern the pattern to be matched
 * @return          a new `Expression` instance
 */
@JvmName("notLikeNullable")
infix fun <Columns> Column<Columns, String?>.notLike(pattern: String?) = notLike<Columns>(name, pattern)
//endregion

//region between
private fun <Columns> between(name: String, min: Any?, maxIncl: Any?): Expression<Columns> = when {
    min != null && maxIncl != null -> Between(name, min, maxIncl)
    min != null && maxIncl == null -> GreaterThanOrEqualTo(name, min)
    min == null && maxIncl != null -> LessThanOrEqualTo(name, maxIncl)
    else -> IsNull(name)
}
/**
 * Creates an [Expression][ch.icken.query.Expression] using the 'BETWEEN' operator.
 *
 * @param   min     the minimum value to be compared with
 * @param   maxIncl the maximum (inclusive) value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("between")
fun <Columns, Type : Any> Column<Columns, Type>.between(min: Type, maxIncl: Type) =
    between<Columns>(name, min, maxIncl)
/**
 * Creates an [Expression][ch.icken.query.Expression] using exactly one of the following:
 * - the 'BETWEEN' operator when both `min` and `maxIncl` are not null
 * - the '>=' operator when `min` is not null and `maxIncl` is null
 * - the '<=' operator when `min` is null and `maxIncl` is not null
 * - 'IS NULL' when both `min` and `maxIncl` are null
 *
 * @param   min     the minimum value to be compared with
 * @param   maxIncl the maximum (inclusive) value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("betweenNullable")
fun <Columns, Type : Any> Column<Columns, Type?>.between(min: Type?, maxIncl: Type?) =
    between<Columns>(name, min, maxIncl)
//endregion
//region notBetween
private fun <Columns> notBetween(name: String, min: Any?, maxIncl: Any?): Expression<Columns> = when {
    min != null && maxIncl != null -> NotBetween(name, min, maxIncl)
    min != null && maxIncl == null -> LessThan(name, min)
    min == null && maxIncl != null -> GreaterThan(name, maxIncl)
    else -> IsNotNull(name)
}
/**
 * Creates an [Expression][ch.icken.query.Expression] using the 'NOT BETWEEN' operator.
 *
 * @param   min     the minimum value to be compared with
 * @param   maxIncl the maximum (inclusive) value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("notBetween")
fun <Columns, Type : Any> Column<Columns, Type>.notBetween(min: Type, maxIncl: Type) =
    notBetween<Columns>(name, min, maxIncl)
/**
 * Creates an [Expression][ch.icken.query.Expression] using exactly one of the following:
 * - the 'NOT BETWEEN' operator when both `min` and `maxIncl` are not null
 * - the '<' operator when `min` is not null and `maxIncl` is null
 * - the '>' operator when `min` is null and `maxIncl` is not null
 * - 'IS NOT NULL' when both `min` and `maxIncl` are null
 *
 * @param   min     the minimum value to be compared with
 * @param   maxIncl the maximum (inclusive) value to be compared with
 * @return          a new `Expression` instance
 */
@JvmName("notBetweenNullable")
fun <Columns, Type : Any> Column<Columns, Type?>.notBetween(min: Type?, maxIncl: Type?) =
    notBetween<Columns>(name, min, maxIncl)
//endregion
