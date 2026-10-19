/*
 * Copyright 2023-2026 Thijs Koppen
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

package ch.icken.processor

import ch.icken.processor.model.KSClassDeclarationWithIdTypeName
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.ksp.writeTo

internal class PanacheCompanionBaseProcessor(
    options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : Processor(options) {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (valid, invalid) = resolver.getSymbolsWithAnnotation(JAKARTA_PERSISTENCE_ENTITY)
            .partition(KSAnnotated::validate)

        valid.filterPanacheEntities()
            .mapNotNull { it.withIdTypeName() }
            .forEach(::createEntityExtensions)

        return invalid
    }

    @OptIn(ExperimentalKotlinPoetApi::class)
    internal fun createEntityExtensions(entity: KSClassDeclarationWithIdTypeName) {
        val targetPackageName = entity.generatedPackageName
        val extensionFileName = entity.extensionFileName
        logger.info("Generating $targetPackageName.$extensionFileName")

        //region Names and types
        val entitySimpleName = entity.simpleName
        val entityClassName = entity.toClassName()
        val entityCompanionObjectClassName = entityClassName.nestedClass(CLASS_NAME_COMPANION)
        val columnsObjectClassName = ClassName(targetPackageName, entity.columnsObjectName)

        val expressionTypeName = ExpressionClassName
            .plusParameter(columnsObjectClassName)
        val expressionParameterTypeName = LambdaTypeName.get(
            receiver = columnsObjectClassName,
            returnType = expressionTypeName
        )
        val queryComponentTypeName = QueryComponentClassName
            .plusParameter(entityClassName)
            .plusParameter(entity.idTypeName)
            .plusParameter(columnsObjectClassName)

        val orderExpressionParameterTypeName = LambdaTypeName.get(
            receiver = columnsObjectClassName,
            returnType = UNIT,
            contextParameters = listOf(
                OrderBuilderClassName.plusParameter(columnsObjectClassName)
            )
        )
        val orderTypeName = OrderClassName
            .plusParameter(entityClassName)
            .plusParameter(entity.idTypeName)
            .plusParameter(columnsObjectClassName)

        val setterExpressionParameterTypeName = LambdaTypeName.get(
            receiver = columnsObjectClassName,
            returnType = SetterExpressionClassName
        )
        val setterUpdateComponentTypeName = SetterUpdateComponentClassName
            .plusParameter(entityClassName)
            .plusParameter(entity.idTypeName)
            .plusParameter(columnsObjectClassName)
        val logicalUpdateComponentTypeName = LogicalUpdateComponentClassName
            .plusParameter(entityClassName)
            .plusParameter(entity.idTypeName)
            .plusParameter(columnsObjectClassName)
        //endregion

        //region where, and, or, orderBy
        val where = FunSpec.builder(FUNCTION_NAME_WHERE)
            .receiver(entityCompanionObjectClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterTypeName)
            .returns(queryComponentTypeName)
            .addStatement("return %M($PARAM_NAME_EXPRESSION(%T))",
                MemberName(QUERY_PACKAGE, FUNCTION_NAME_WHERE), columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_WHERE$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Starts building a SELECT or DELETE query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [QueryComponent][$QUERY_PACKAGE.Component.QueryComponent] instance
                """.trimIndent())
            .build()

        val and = FunSpec.builder(FUNCTION_NAME_AND)
            .receiver(queryComponentTypeName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterTypeName)
            .returns(queryComponentTypeName)
            .addStatement("return $FUNCTION_NAME_AND($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_AND$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Adds an AND operator to this SELECT/DELETE query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [QueryComponent][$QUERY_PACKAGE.Component.QueryComponent] instance
                """.trimIndent())
            .build()
        val or = FunSpec.builder(FUNCTION_NAME_OR)
            .receiver(queryComponentTypeName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterTypeName)
            .returns(queryComponentTypeName)
            .addStatement("return $FUNCTION_NAME_OR($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_OR$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Adds an OR operator to this SELECT/DELETE query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [QueryComponent][$QUERY_PACKAGE.Component.QueryComponent] instance
                """.trimIndent())
            .build()

        val orderBy = FunSpec.builder(FUNCTION_NAME_ORDER_BY)
            .receiver(queryComponentTypeName)
            .addParameter(PARAM_NAME_EXPRESSION, orderExpressionParameterTypeName)
            .returns(orderTypeName)
            .addStatement("return $FUNCTION_NAME_ORDER_BY(%T, $PARAM_NAME_EXPRESSION)", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_ORDER_BY$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Adds sort options to this SELECT/DELETE query as defined in `expression`.
                This operation is optional and must be the last operation in the chain,
                after which you can only `find()`, `stream()`, or `multiple()`.

                Allows for multiple columns, for example:
                ```
                val result = User.where { ... }
                    .orderBy {
                        lastName.desc()
                        firstName.asc()
                    }
                    .multiple()
                ```
                
                @param  expression  declare the columns to be sorted, and their direction (required)
                @return             a new [Order][$QUERY_PACKAGE.Order] instance
                """.trimIndent())
            .build()
        //endregion

        //region count, delete, find, stream
        val count = FunSpec.builder(FUNCTION_NAME_COUNT)
            .receiver(entityCompanionObjectClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterTypeName)
            .returns(LongClassName)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_COUNT()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_COUNT$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Counts the number of entities matching the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             the number of entities counted
                @see                io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.count
                """.trimIndent())
            .build()

        val delete = FunSpec.builder(FUNCTION_NAME_DELETE)
            .receiver(entityCompanionObjectClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterTypeName)
            .returns(LongClassName)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_DELETE()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_DELETE$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Deletes all entities matching the returned `expression`.
                
                WARNING: the default Panache implementation behind this function uses a bulk delete query
                and ignores cascading rules from the JPA model.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             the number of entities deleted
                @see                io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.delete
                """.trimIndent())
            .build()

        val find = FunSpec.builder(FUNCTION_NAME_FIND)
            .receiver(entityCompanionObjectClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterTypeName)
            .returns(PanacheQueryClassName.plusParameter(entityClassName))
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_FIND()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_FIND$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Finds entities matching the returned `expression`.
                
                May be used to chain functionality not (yet) abstracted by this library, like
                [page][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.page] and
                [project][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.project].
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [PanacheQuery][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery] instance
                @see                io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.find
                """.trimIndent())
            .build()

        val stream = FunSpec.builder(FUNCTION_NAME_STREAM)
            .receiver(entityCompanionObjectClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterTypeName)
            .returns(StreamClassName.plusParameter(entityClassName))
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_STREAM()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_STREAM$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Streams all entities matching the returned `expression`.
                This function is a shortcut for `find().stream()`.
                
                WARNING: this function requires a transaction to be active,
                otherwise the underlying cursor may be closed before the end of the stream.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [Stream][java.util.stream.Stream] instance containing all results,
                without paging
                @see                io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.stream
                """.trimIndent())
            .build()
        //endregion

        //region single, multiple
        val single = FunSpec.builder(FUNCTION_NAME_SINGLE)
            .receiver(entityCompanionObjectClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterTypeName)
            .returns(entityClassName)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).single()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_SINGLE$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Finds a single result matching the returned `expression`, or throws if there is not exactly one.
                This function is a shortcut for `find().singleResult()`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             the single result
                @throws             jakarta.persistence.NoResultException when there is no result
                @throws             jakarta.persistence.NonUniqueResultException when there are multiple results
                @see                io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.singleResult
                """.trimIndent())
            .build()
        val singleSafe = FunSpec.builder(FUNCTION_NAME_SINGLE_SAFE)
            .receiver(entityCompanionObjectClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterTypeName)
            .returns(SingleResultClassName.plusParameter(WildcardTypeName.producerOf(entityClassName)))
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).singleSafe()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_SINGLE_SAFE$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Finds a single result matching the returned `expression`, but does not throw if there is not exactly one.
                This function is a shortcut for `find().singleResultSafe()`.
                
                See [singleSafe][$QUERY_PACKAGE.Component.QueryComponent.singleSafe] for more details.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a [SingleResult][$QUERY_PACKAGE.SingleResult] instance
                @see                $QUERY_PACKAGE.singleResultSafe
                """.trimIndent())
            .build()

        val multiple = FunSpec.builder(FUNCTION_NAME_MULTIPLE)
            .receiver(entityCompanionObjectClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterTypeName)
            .returns(ListClassName.plusParameter(entityClassName))
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).multiple()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_MULTIPLE$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Finds all entities matching the returned `expression`. This function is a shortcut for `find().list()`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [List][kotlin.collections.List] instance containing all results,
                without paging
                @see                io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.list
                """.trimIndent())
            .build()
        //endregion

        //region update, updateAll
        val updateExtensionFunction = MemberName(QUERY_PACKAGE, FUNCTION_NAME_UPDATE)
        val update = FunSpec.builder(FUNCTION_NAME_UPDATE)
            .receiver(entityCompanionObjectClassName)
            .addParameter(PARAM_NAME_SETTER, setterExpressionParameterTypeName)
            .returns(setterUpdateComponentTypeName)
            .addStatement("return %M(%T, $PARAM_NAME_SETTER)", updateExtensionFunction, columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_UPDATE$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Starts building an UPDATE query by [set][$QUERY_PACKAGE.Column.set]-ing a new value.
                
                @param  setter  build and return a
                [SetterExpression][$QUERY_PACKAGE.SetterExpression]
                @return         a new
                [SetterUpdateComponent][$QUERY_PACKAGE.Component.UpdateComponent.SetterUpdateComponent] instance
                """.trimIndent())
            .build()
        val updateMultiple = FunSpec.builder(FUNCTION_NAME_UPDATE)
            .receiver(entityCompanionObjectClassName)
            .addParameter(PARAM_NAME_SETTERS, setterExpressionParameterTypeName, KModifier.VARARG)
            .returns(setterUpdateComponentTypeName)
            .addStatement("return %M(%T, $PARAM_NAME_SETTERS)", updateExtensionFunction, columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_UPDATE_MULTIPLE$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Starts building an UPDATE query by [set][$QUERY_PACKAGE.Column.set]-ing multiple new values.
                
                @param  setters build and return multiple
                [SetterExpression][$QUERY_PACKAGE.SetterExpression]s
                @return         a new
                [SetterUpdateComponent][$QUERY_PACKAGE.Component.UpdateComponent.SetterUpdateComponent] instance
                """.trimIndent())
            .build()

        val updateAll = FunSpec.builder(FUNCTION_NAME_UPDATE_ALL)
            .receiver(entityCompanionObjectClassName)
            .addParameter(PARAM_NAME_SETTER, setterExpressionParameterTypeName)
            .returns(IntClassName)
            .addStatement("return %M(%T, $PARAM_NAME_SETTER).executeWithoutWhere()",
                updateExtensionFunction, columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_UPDATE_ALL$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Updates all entities of this type by [set][$QUERY_PACKAGE.Column.set]-ing a new value.
                
                WARNING: this function updates ALL entities without a WHERE clause.
                
                WARNING: this function requires a transaction to be active.
                
                @param  setter  build and return a
                [SetterExpression][$QUERY_PACKAGE.SetterExpression]
                @return         the number of entities updated
                @see            io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.update
                """.trimIndent())
            .build()
        val updateAllMultiple = FunSpec.builder(FUNCTION_NAME_UPDATE_ALL)
            .receiver(entityCompanionObjectClassName)
            .addParameter(PARAM_NAME_SETTERS, setterExpressionParameterTypeName, KModifier.VARARG)
            .returns(IntClassName)
            .addStatement("return %M(%T, $PARAM_NAME_SETTERS).executeWithoutWhere()",
                updateExtensionFunction, columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_UPDATE_ALL_MULTIPLE$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Updates all entities of this type by [set][$QUERY_PACKAGE.Column.set]-ing multiple new values.
                
                WARNING: this function updates ALL entities without a WHERE clause.
                
                WARNING: this function requires a transaction to be active.
                
                @param  setters build and return multiple
                [SetterExpression][$QUERY_PACKAGE.SetterExpression]s
                @return         the number of entities updated
                @see            io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.update
                """.trimIndent())
            .build()
        //endregion

        //region whereUpdate, andUpdate, orUpdate
        val whereUpdate = FunSpec.builder(FUNCTION_NAME_WHERE)
            .receiver(setterUpdateComponentTypeName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterTypeName)
            .returns(logicalUpdateComponentTypeName)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_WHERE_UPDATE$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Adds a WHERE clause to this UPDATE query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new
                [LogicalUpdateComponent][$QUERY_PACKAGE.Component.UpdateComponent.LogicalUpdateComponent] instance
                """.trimIndent())
            .build()

        val andUpdate = FunSpec.builder(FUNCTION_NAME_AND)
            .receiver(logicalUpdateComponentTypeName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterTypeName)
            .returns(logicalUpdateComponentTypeName)
            .addStatement("return $FUNCTION_NAME_AND($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_AND_UPDATE$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Adds an AND operator to this UPDATE query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new
                [LogicalUpdateComponent][$QUERY_PACKAGE.Component.UpdateComponent.LogicalUpdateComponent] instance
                """.trimIndent())
            .build()
        val orUpdate = FunSpec.builder(FUNCTION_NAME_OR)
            .receiver(logicalUpdateComponentTypeName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterTypeName)
            .returns(logicalUpdateComponentTypeName)
            .addStatement("return $FUNCTION_NAME_OR($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_OR_UPDATE$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Adds an OR operator to this UPDATE query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new
                [LogicalUpdateComponent][$QUERY_PACKAGE.Component.UpdateComponent.LogicalUpdateComponent] instance
                """.trimIndent())
            .build()
        //endregion

        //region andExpression, orExpression
        val andExpression = FunSpec.builder(FUNCTION_NAME_AND)
            .receiver(expressionTypeName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterTypeName)
            .returns(expressionTypeName)
            .addStatement("return $FUNCTION_NAME_AND($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_AND_EXPRESSION$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Adds an AND operator to this query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [Expression][$QUERY_PACKAGE.Expression] instance
                """.trimIndent())
            .build()
        val orExpression = FunSpec.builder(FUNCTION_NAME_OR)
            .receiver(expressionTypeName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterTypeName)
            .returns(expressionTypeName)
            .addStatement("return $FUNCTION_NAME_OR($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_OR_EXPRESSION$entitySimpleName"))
            .addGeneratedAnnotation()
            .addKdoc("""
                Adds an OR operator to this query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [Expression][$QUERY_PACKAGE.Expression] instance
                """.trimIndent())
            .build()
        //endregion

        val functions = listOf(where, and, or, orderBy,
            count, delete, find, stream,
            single, singleSafe, multiple,
            update, updateMultiple, updateAll, updateAllMultiple,
            whereUpdate, andUpdate, orUpdate,
            andExpression, orExpression)

        FileSpec.builder(targetPackageName, extensionFileName)
            .addFunctions(functions)
            .addAnnotation(suppressFileAnnotation)
            .addGeneratedAnnotation()
            .build()
            .writeTo(codeGenerator, Dependencies(false))
    }

    private fun jvmNameAnnotation(name: String) =
        AnnotationSpec.builder(JvmNameClassName)
            .addMember("%S", name)
            .build()

    internal companion object {
        //region Class Names
        private val ExpressionClassName = ClassName(QUERY_PACKAGE, "Expression")
        private val SetterUpdateComponentClassName =
            ClassName("$QUERY_PACKAGE.Component.UpdateComponent", "SetterUpdateComponent")
        private val IntClassName = ClassName("kotlin", "Int")
        private val JvmNameClassName = ClassName("kotlin.jvm", "JvmName")
        internal val ListClassName = ClassName("kotlin.collections", "List")
        private val LogicalUpdateComponentClassName =
            ClassName("$QUERY_PACKAGE.Component.UpdateComponent", "LogicalUpdateComponent")
        private val OrderClassName = ClassName(QUERY_PACKAGE, "Order")
        private val OrderBuilderClassName = OrderClassName.nestedClass("Builder")
        private val PanacheQueryClassName = ClassName("io.quarkus.hibernate.orm.panache.kotlin", "PanacheQuery")
        private val QueryComponentClassName = ClassName("$QUERY_PACKAGE.Component", "QueryComponent")
        private val SetterExpressionClassName = ClassName(QUERY_PACKAGE, "SetterExpression")
        private val SingleResultClassName = ClassName(QUERY_PACKAGE, "SingleResult")
        private val StreamClassName = ClassName("java.util.stream", "Stream")
        //endregion
        //region Constant
        private const val CLASS_NAME_COMPANION = "Companion"
        private const val FUNCTION_NAME_AND = "and"
        private const val FUNCTION_NAME_AND_EXPRESSION = "andExpression"
        private const val FUNCTION_NAME_AND_UPDATE = "andUpdate"
        private const val FUNCTION_NAME_COUNT = "count"
        private const val FUNCTION_NAME_DELETE = "delete"
        private const val FUNCTION_NAME_FIND = "find"
        private const val FUNCTION_NAME_MULTIPLE = "multiple"
        private const val FUNCTION_NAME_OR = "or"
        private const val FUNCTION_NAME_OR_EXPRESSION = "orExpression"
        private const val FUNCTION_NAME_OR_UPDATE = "orUpdate"
        private const val FUNCTION_NAME_ORDER_BY = "orderBy"
        private const val FUNCTION_NAME_SINGLE = "single"
        private const val FUNCTION_NAME_SINGLE_SAFE = "singleSafe"
        private const val FUNCTION_NAME_STREAM = "stream"
        private const val FUNCTION_NAME_UPDATE = "update"
        private const val FUNCTION_NAME_UPDATE_ALL = "updateAll"
        private const val FUNCTION_NAME_UPDATE_ALL_MULTIPLE = "updateAllMultiple"
        private const val FUNCTION_NAME_UPDATE_MULTIPLE = "updateMultiple"
        private const val FUNCTION_NAME_WHERE = "where"
        private const val FUNCTION_NAME_WHERE_UPDATE = "whereUpdate"
        private const val PARAM_NAME_EXPRESSION = "expression"
        private const val PARAM_NAME_SETTER = "setter"
        private const val PARAM_NAME_SETTERS = "setters"
        //endregion
    }
}

class PanacheCompanionBaseProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        PanacheCompanionBaseProcessor(environment.options, environment.codeGenerator, environment.logger)
}
