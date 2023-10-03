package ch.icken.query

@JvmInline
value class ColumnName<T : Any?>(internal val name: String) {
    infix fun eq(value: T) = BooleanExpression.EqualTo(name, hql(value))
    infix fun neq(value: T) = BooleanExpression.NotEqualTo(name, hql(value))
    infix fun lt(value: T) = BooleanExpression.LessThan(name, hql(value))
    infix fun gt(value: T) = BooleanExpression.GreaterThan(name, hql(value))
    infix fun lte(value: T) = BooleanExpression.LessThanOrEqualTo(name, hql(value))
    infix fun gte(value: T) = BooleanExpression.GreaterThanOrEqualTo(name, hql(value))

    fun between(min: T, maxInclusive: T) = BooleanExpression.Between(name, hql(min), hql(maxInclusive))
    fun notBetween(min: T, maxInclusive: T) = BooleanExpression.NotBetween(name, hql(min), hql(maxInclusive))

    infix fun `in`(values: Collection<T>) = BooleanExpression.In(name, values.map { hql(it) })
    infix fun notIn(values: Collection<T>) = BooleanExpression.NotIn(name, values.map { hql(it) })

    companion object {
        /**
         * Convert to Hibernate query language format
         */
        private fun hql(value: Any?): String = when (value) {
            null -> "NULL"
            is Boolean -> value.toString().uppercase()
            is Int, is Long -> value.toString()
            //TODO Float, Double
            is String -> "'$value'"
            //TODO LocalDate, LocalDateTime
            else -> TODO()
        }
    }
}

fun <T> ColumnName<T?>.isNull() = BooleanExpression.IsNull(name)
fun <T> ColumnName<T?>.isNotNull() = BooleanExpression.IsNotNull(name)

infix fun ColumnName<String?>.like(expression: String) = BooleanExpression.Like(name, expression)
infix fun ColumnName<String?>.notLike(expression: String) = BooleanExpression.NotLike(name, expression)
