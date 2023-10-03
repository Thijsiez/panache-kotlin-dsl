package ch.icken.query

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.panache.common.Sort

abstract class QueryComponent<Entity : PanacheEntityBase, Id : Any> internal constructor(
    private val companion: PanacheCompanionBase<Entity, Id>
) {
    abstract fun compile(): String

    //region Intermediate operations
    fun and(expression: BooleanExpression) = LogicalQueryComponent.AndQueryComponent(companion, this, expression)
    fun or(expression: BooleanExpression) = LogicalQueryComponent.OrQueryComponent(companion, this, expression)

    fun andGroup(expression: BooleanExpression,
                 groupComponent: QueryComponent<Entity, Id>.() -> QueryComponent<Entity, Id>) =
        GroupQueryComponent.AndGroupQueryComponent(companion, this, expression, groupComponent)
    fun orGroup(expression: BooleanExpression,
                groupComponent: QueryComponent<Entity, Id>.() -> QueryComponent<Entity, Id>) =
        GroupQueryComponent.OrGroupQueryComponent(companion, this, expression, groupComponent)
    //endregion

    //region Terminal operations
    fun count() = companion.count(compile())
    fun delete() = companion.delete(compile())
    fun find() = companion.find(compile())
    fun find(sort: Sort) = companion.find(compile(), sort)
    fun stream() = companion.stream(compile())
    fun stream(sort: Sort) = companion.stream(compile(), sort)
    fun update() = companion.update(compile())

    fun getSingle() = find().singleResult()
    fun getMultiple() = find().list()
    fun getMultiple(sort: Sort) = find(sort).list()
    //endregion
}
