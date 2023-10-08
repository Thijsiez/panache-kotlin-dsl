package ch.icken.query

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase

class WhereQueryComponent<Entity : PanacheEntityBase, Id : Any> internal constructor(
    companion: PanacheCompanionBase<Entity, Id>,
    expression: BooleanExpression
) : InitialQueryComponent<Entity, Id>(companion, expression)

class WhereGroupQueryComponent<Entity : PanacheEntityBase, Id : Any> internal constructor(
    companion: PanacheCompanionBase<Entity, Id>,
    expression: BooleanExpression,
    private val groupComponent: QueryComponent<Entity, Id>.() -> QueryComponent<Entity, Id>
) : QueryComponent<Entity, Id>(companion) {
    private val initialComponent = InitialQueryComponent(companion, expression)

    override fun compile() = "(${groupComponent.invoke(initialComponent).compile()})"
}

fun <Entity : PanacheEntityBase, Id : Any> PanacheCompanionBase<Entity, Id>.where(expression: BooleanExpression) =
    WhereQueryComponent(this, expression)

fun <Entity : PanacheEntityBase, Id : Any> PanacheCompanionBase<Entity, Id>.whereGroup(
    expression: BooleanExpression,
    groupComponent: QueryComponent<Entity, Id>.() -> QueryComponent<Entity, Id>
) = WhereGroupQueryComponent(this, expression, groupComponent)