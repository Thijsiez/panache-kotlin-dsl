package ch.icken.processor

import ch.icken.query.BooleanExpression
import ch.icken.query.ColumnName
import ch.icken.query.LogicalQueryComponent
import ch.icken.query.QueryComponent
import com.squareup.kotlinpoet.asClassName

internal object ClassNames {
    val AndQueryComponentClassName = LogicalQueryComponent.AndQueryComponent::class.asClassName()
    val BooleanExpressionClassName = BooleanExpression::class.asClassName()
    val ColumnNameClassName = ColumnName::class.asClassName()
    val EnumTypeOrdinalClassName = ColumnName.EnumTypeOrdinal::class.asClassName()
    val EnumTypeStringClassName = ColumnName.EnumTypeString::class.asClassName()
    val JvmNameClassName = JvmName::class.asClassName()
    val OrQueryComponentClassName = LogicalQueryComponent.OrQueryComponent::class.asClassName()
    val QueryComponentClassName = QueryComponent::class.asClassName()
    val StringClassName = String::class.asClassName()
    val SuppressClassName = Suppress::class.asClassName()
}
