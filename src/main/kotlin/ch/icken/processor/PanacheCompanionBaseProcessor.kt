/*
 * Copyright 2023-2025 Thijs Koppen
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

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class PanacheCompanionBaseProcessor(
    options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : ProcessorCommon(options), SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (valid, invalid) = resolver.getSymbolsWithAnnotation(JAKARTA_PERSISTENCE_ENTITY)
            .partition(KSAnnotated::validate)

        valid.filterIsInstance<KSClassDeclaration>()
            .filter { it.isSubclass(HIBERNATE_PANACHE_ENTITY_BASE) }
            //Find out what the type of the @Id column of this entity is
            .associateWith { ksClassDeclaration ->
                ksClassDeclaration.declarations
                    .filterIsInstance<KSClassDeclaration>()
                    .singleOrNull(KSClassDeclaration::isCompanionObject)
                    ?.superclassType(HIBERNATE_PANACHE_COMPANION_BASE)
                    ?.arguments
                    ?.lastOrNull()
                    ?.toTypeName()
            }
            .filterValuesNotNull()
            .forEach(::createEntityExtensions)

        return invalid
    }

    internal fun createEntityExtensions(ksClass: KSClassDeclaration, idTypeName: TypeName) {
        val packageName = ksClass.packageName.asString() + SUFFIX_PACKAGE_GENERATED
        val classSimpleName = ksClass.simpleName.asString()
        val extensionFileName = classSimpleName + SUFFIX_FILE_EXTENSIONS
        logger.info("Generating $packageName.$extensionFileName")

        //region Names and types
        val className = ksClass.toClassName()
        val columnsObjectClassName = ClassName(packageName, classSimpleName + SUFFIX_OBJECT_COLUMNS)
        val companionClassName = className.nestedClass(CLASS_NAME_COMPANION)

        val expressionType = ExpressionClassName
            .plusParameter(columnsObjectClassName)
        val expressionParameterLambdaType = LambdaTypeName.get(
            receiver = columnsObjectClassName,
            returnType = expressionType
        )
        val queryComponentType = QueryComponentClassName
            .plusParameter(className)
            .plusParameter(idTypeName)
            .plusParameter(columnsObjectClassName)

        val setterExpressionParameterLambdaType = LambdaTypeName.get(
            receiver = columnsObjectClassName,
            returnType = SetterExpressionClassName
        )
        val initialUpdateComponentType = InitialUpdateComponentClassName
            .plusParameter(className)
            .plusParameter(idTypeName)
            .plusParameter(columnsObjectClassName)
        val logicalUpdateComponentType = LogicalUpdateComponentClassName
            .plusParameter(className)
            .plusParameter(idTypeName)
            .plusParameter(columnsObjectClassName)
        //endregion

        //region where, and, or
        val where = FunSpec.builder(FUNCTION_NAME_WHERE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(queryComponentType)
            .addStatement("return %M($PARAM_NAME_EXPRESSION(%T))",
                MemberName(QUERY_PACKAGE, FUNCTION_NAME_WHERE), columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_WHERE$classSimpleName"))
            .addKdoc("""
                Starts building a SELECT or DELETE query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [QueryComponent][$QUERY_PACKAGE.Component.QueryComponent] instance
                """.trimIndent())

        val and = FunSpec.builder(FUNCTION_NAME_AND)
            .receiver(queryComponentType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(queryComponentType)
            .addStatement("return $FUNCTION_NAME_AND($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_AND$classSimpleName"))
            .addKdoc("""
                Adds an AND operator to this SELECT/DELETE query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [QueryComponent][$QUERY_PACKAGE.Component.QueryComponent] instance
                """.trimIndent())
        val or = FunSpec.builder(FUNCTION_NAME_OR)
            .receiver(queryComponentType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(queryComponentType)
            .addStatement("return $FUNCTION_NAME_OR($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_OR$classSimpleName"))
            .addKdoc("""
                Adds an OR operator to this SELECT/DELETE query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [QueryComponent][$QUERY_PACKAGE.Component.QueryComponent] instance
                """.trimIndent())
        //endregion

        //region count, delete, find, stream
        val count = FunSpec.builder(FUNCTION_NAME_COUNT)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(LongClassName)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_COUNT()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_COUNT$classSimpleName"))
            .addKdoc("""
                Counts the number of entities matching the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             the number of entities counted
                @see                io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.count
                """.trimIndent())

        val delete = FunSpec.builder(FUNCTION_NAME_DELETE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(LongClassName)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_DELETE()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_DELETE$classSimpleName"))
            .addKdoc("""
                Deletes all entities matching the returned `expression`.
                
                WARNING: the default Panache implementation behind this function uses a bulk delete query
                and ignores cascading rules from the JPA model.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             the number of entities deleted
                @see                io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.delete
                """.trimIndent())

        val findReturns = PanacheQueryClassName.plusParameter(className)
        val find = FunSpec.builder(FUNCTION_NAME_FIND)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(findReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_FIND()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_FIND$classSimpleName"))
            .addKdoc("""
                Finds entities matching the returned `expression`.
                
                May be used to chain functionality not (yet) abstracted by this library, like
                [page][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.page] and
                [project][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.project].
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [PanacheQuery][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery] instance
                @see                io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.find
                """.trimIndent())
        val findSorted = FunSpec.builder(FUNCTION_NAME_FIND)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_SORT, SortClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(findReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_FIND($PARAM_NAME_SORT)")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_FIND_SORTED$classSimpleName"))
            .addKdoc("""
                Finds entities matching the returned `expression` and the given sort options.
                
                May be used to chain functionality not (yet) abstracted by this library, like
                [page][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.page] and
                [project][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.project].
                
                @param  sort        the sort strategy to use
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [PanacheQuery][io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery] instance
                @see                io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.find
                """.trimIndent())

        val streamReturns = StreamClassName.plusParameter(className)
        val stream = FunSpec.builder(FUNCTION_NAME_STREAM)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(streamReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_STREAM()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_STREAM$classSimpleName"))
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
        val streamSorted = FunSpec.builder(FUNCTION_NAME_STREAM)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_SORT, SortClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(streamReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_STREAM($PARAM_NAME_SORT)")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_STREAM_SORTED$classSimpleName"))
            .addKdoc("""
                Streams all entities matching the returned `expression` and the given sort options.
                This function is a shortcut for `find(sort).stream()`.
                
                WARNING: this function requires a transaction to be active,
                otherwise the underlying cursor may be closed before the end of the stream.
                
                @param  sort        the sort strategy to use
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [Stream][java.util.stream.Stream] instance containing all results,
                without paging
                @see                io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.stream
                """.trimIndent())
        //endregion

        //region single, multiple
        val single = FunSpec.builder(FUNCTION_NAME_SINGLE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(className)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).single()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_SINGLE$classSimpleName"))
            .addKdoc("""
                Finds a single result matching the returned `expression`, or throws if there is not exactly one.
                This function is a shortcut for `find().singleResult()`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             the single result
                @throws             jakarta.persistence.NoResultException when there is no result
                @throws             jakarta.persistence.NonUniqueResultException when there are multiple results
                @see                io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.singleResult
                """.trimIndent())
        val singleSafe = FunSpec.builder(FUNCTION_NAME_SINGLE_SAFE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(PanacheSingleResultClassName.plusParameter(WildcardTypeName.producerOf(className)))
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).singleSafe()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_SINGLE_SAFE$classSimpleName"))
            .addKdoc("""
                Finds a single result matching the returned `expression`, but does not throw if there is not exactly one.
                This function is a shortcut for `find().singleResultSafe()`.
                
                See [singleSafe][$QUERY_PACKAGE.Component.QueryComponent.singleSafe] for more details.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a [PanacheSingleResult][$QUERY_PACKAGE.PanacheSingleResult] instance
                @see                $QUERY_PACKAGE.singleResultSafe
                """.trimIndent())

        val multipleReturns = ListClassName.plusParameter(className)
        val multiple = FunSpec.builder(FUNCTION_NAME_MULTIPLE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(multipleReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).multiple()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_MULTIPLE$classSimpleName"))
            .addKdoc("""
                Finds all entities matching the returned `expression`. This function is a shortcut for `find().list()`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [List][kotlin.collections.List] instance containing all results,
                without paging
                @see                io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.list
                """.trimIndent())
        val multipleSorted = FunSpec.builder(FUNCTION_NAME_MULTIPLE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_SORT, SortClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(multipleReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).multiple($PARAM_NAME_SORT)")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_MULTIPLE_SORTED$classSimpleName"))
            .addKdoc("""
                Finds all entities matching the returned `expression` and the given sort options.
                This function is a shortcut for `find(sort).list()`.
                
                @param  sort        the sort strategy to use
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [List][kotlin.collections.List] instance containing all results,
                without paging
                @see                io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery.list
                """.trimIndent())
        //endregion

        //region update, updateAll
        val updateExtensionFunction = MemberName(QUERY_PACKAGE, FUNCTION_NAME_UPDATE)
        val update = FunSpec.builder(FUNCTION_NAME_UPDATE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_SETTER, setterExpressionParameterLambdaType)
            .returns(initialUpdateComponentType)
            .addStatement("return %M(%T, $PARAM_NAME_SETTER)", updateExtensionFunction, columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_UPDATE$classSimpleName"))
            .addKdoc("""
                Starts building an UPDATE query by [set][$QUERY_PACKAGE.Column.set]-ing a new value.
                
                @param  setter  build and return a
                [SetterExpression][$QUERY_PACKAGE.Component.UpdateComponent.InitialUpdateComponent.SetterExpression]
                @return         a new
                [InitialUpdateComponent][$QUERY_PACKAGE.Component.UpdateComponent.InitialUpdateComponent] instance
                """.trimIndent())
        val updateMultiple = FunSpec.builder(FUNCTION_NAME_UPDATE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_SETTERS, setterExpressionParameterLambdaType, KModifier.VARARG)
            .returns(initialUpdateComponentType)
            .addStatement("return %M(%T, $PARAM_NAME_SETTERS)", updateExtensionFunction, columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_UPDATE_MULTIPLE$classSimpleName"))
            .addKdoc("""
                Starts building an UPDATE query by [set][$QUERY_PACKAGE.Column.set]-ing multiple new values.
                
                @param  setters build and return multiple
                [SetterExpression][$QUERY_PACKAGE.Component.UpdateComponent.InitialUpdateComponent.SetterExpression]s
                @return         a new
                [InitialUpdateComponent][$QUERY_PACKAGE.Component.UpdateComponent.InitialUpdateComponent] instance
                """.trimIndent())

        val updateAll = FunSpec.builder(FUNCTION_NAME_UPDATE_ALL)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_SETTER, setterExpressionParameterLambdaType)
            .returns(IntClassName)
            .addStatement("return %M(%T, $PARAM_NAME_SETTER).executeWithoutWhere()",
                updateExtensionFunction, columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_UPDATE_ALL$classSimpleName"))
            .addKdoc("""
                Updates all entities of this type by [set][$QUERY_PACKAGE.Column.set]-ing a new value.
                
                WARNING: this function updates ALL entities without a WHERE clause.
                
                WARNING: this function requires a transaction to be active.
                
                @param  setter  build and return a
                [SetterExpression][$QUERY_PACKAGE.Component.UpdateComponent.InitialUpdateComponent.SetterExpression]
                @return         the number of entities updated
                @see            io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.update
                """.trimIndent())
        val updateAllMultiple = FunSpec.builder(FUNCTION_NAME_UPDATE_ALL)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_SETTERS, setterExpressionParameterLambdaType, KModifier.VARARG)
            .returns(IntClassName)
            .addStatement("return %M(%T, $PARAM_NAME_SETTERS).executeWithoutWhere()",
                updateExtensionFunction, columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_UPDATE_ALL_MULTIPLE$classSimpleName"))
            .addKdoc("""
                Updates all entities of this type by [set][$QUERY_PACKAGE.Column.set]-ing multiple new values.
                
                WARNING: this function updates ALL entities without a WHERE clause.
                
                WARNING: this function requires a transaction to be active.
                
                @param  setters build and return multiple
                [SetterExpression][$QUERY_PACKAGE.Component.UpdateComponent.InitialUpdateComponent.SetterExpression]s
                @return         the number of entities updated
                @see            io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase.update
                """.trimIndent())
        //endregion

        //region whereUpdate, andUpdate, orUpdate
        val whereUpdate = FunSpec.builder(FUNCTION_NAME_WHERE)
            .receiver(initialUpdateComponentType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(logicalUpdateComponentType)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_WHERE_UPDATE$classSimpleName"))
            .addKdoc("""
                Adds a WHERE clause to this UPDATE query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new
                [LogicalUpdateComponent][$QUERY_PACKAGE.Component.UpdateComponent.LogicalUpdateComponent] instance
                """.trimIndent())

        val andUpdate = FunSpec.builder(FUNCTION_NAME_AND)
            .receiver(logicalUpdateComponentType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(logicalUpdateComponentType)
            .addStatement("return $FUNCTION_NAME_AND($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_AND_UPDATE$classSimpleName"))
            .addKdoc("""
                Adds an AND operator to this UPDATE query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new
                [LogicalUpdateComponent][$QUERY_PACKAGE.Component.UpdateComponent.LogicalUpdateComponent] instance
                """.trimIndent())
        val orUpdate = FunSpec.builder(FUNCTION_NAME_OR)
            .receiver(logicalUpdateComponentType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(logicalUpdateComponentType)
            .addStatement("return $FUNCTION_NAME_OR($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_OR_UPDATE$classSimpleName"))
            .addKdoc("""
                Adds an OR operator to this UPDATE query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new
                [LogicalUpdateComponent][$QUERY_PACKAGE.Component.UpdateComponent.LogicalUpdateComponent] instance
                """.trimIndent())
        //endregion

        //region andExpression, orExpression
        val andExpression = FunSpec.builder(FUNCTION_NAME_AND)
            .receiver(expressionType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(expressionType)
            .addStatement("return $FUNCTION_NAME_AND($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_AND_EXPRESSION$classSimpleName"))
            .addKdoc("""
                Adds an AND operator to this query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [Expression][$QUERY_PACKAGE.Expression] instance
                """.trimIndent())
        val orExpression = FunSpec.builder(FUNCTION_NAME_OR)
            .receiver(expressionType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(expressionType)
            .addStatement("return $FUNCTION_NAME_OR($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_OR_EXPRESSION$classSimpleName"))
            .addKdoc("""
                Adds an OR operator to this query with the returned `expression`.
                
                @param  expression  build and return an [Expression][$QUERY_PACKAGE.Expression]
                @return             a new [Expression][$QUERY_PACKAGE.Expression] instance
                """.trimIndent())
        //endregion

        val functions = listOf(where, and, or,
            count, delete, find, findSorted, stream, streamSorted,
            single, singleSafe, multiple, multipleSorted,
            update, updateMultiple, updateAll, updateAllMultiple,
            whereUpdate, andUpdate, orUpdate,
            andExpression, orExpression)

        FileSpec.builder(packageName, extensionFileName)
            .apply {
                functions.map { it.addGeneratedAnnotation() }
                    .map(FunSpec.Builder::build)
                    .forEach(::addFunction)
            }
            .addAnnotation(suppressFileAnnotation)
            .addGeneratedAnnotation()
            .build()
            .writeTo(codeGenerator, Dependencies(false))
    }

    companion object {
        //region Class Names
        internal val ExpressionClassName = ClassName(QUERY_PACKAGE, "Expression")
        internal val InitialUpdateComponentClassName =
            ClassName("$QUERY_PACKAGE.Component.UpdateComponent", "InitialUpdateComponent")
        internal val IntClassName = ClassName("kotlin", "Int")
        internal val JvmNameClassName = ClassName("kotlin.jvm", "JvmName")
        internal val ListClassName = ClassName("kotlin.collections", "List")
        internal val LogicalUpdateComponentClassName =
            ClassName("$QUERY_PACKAGE.Component.UpdateComponent", "LogicalUpdateComponent")
        internal val LongClassName = ClassName("kotlin", "Long")
        internal val PanacheQueryClassName = ClassName("io.quarkus.hibernate.orm.panache.kotlin", "PanacheQuery")
        internal val PanacheSingleResultClassName = ClassName(QUERY_PACKAGE, "PanacheSingleResult")
        internal val QueryComponentClassName = ClassName("$QUERY_PACKAGE.Component", "QueryComponent")
        internal val SetterExpressionClassName =
            ClassName("$QUERY_PACKAGE.Component.UpdateComponent.InitialUpdateComponent", "SetterExpression")
        internal val SortClassName = ClassName("io.quarkus.panache.common", "Sort")
        internal val StreamClassName = ClassName("java.util.stream", "Stream")
        //endregion
        //region Constant
        internal const val CLASS_NAME_COMPANION = "Companion"
        internal const val FUNCTION_NAME_AND = "and"
        internal const val FUNCTION_NAME_AND_EXPRESSION = "andExpression"
        internal const val FUNCTION_NAME_AND_UPDATE = "andUpdate"
        internal const val FUNCTION_NAME_COUNT = "count"
        internal const val FUNCTION_NAME_DELETE = "delete"
        internal const val FUNCTION_NAME_FIND = "find"
        internal const val FUNCTION_NAME_FIND_SORTED = "findSorted"
        internal const val FUNCTION_NAME_MULTIPLE = "multiple"
        internal const val FUNCTION_NAME_MULTIPLE_SORTED = "multipleSorted"
        internal const val FUNCTION_NAME_OR = "or"
        internal const val FUNCTION_NAME_OR_EXPRESSION = "orExpression"
        internal const val FUNCTION_NAME_OR_UPDATE = "orUpdate"
        internal const val FUNCTION_NAME_SINGLE = "single"
        internal const val FUNCTION_NAME_SINGLE_SAFE = "singleSafe"
        internal const val FUNCTION_NAME_STREAM = "stream"
        internal const val FUNCTION_NAME_STREAM_SORTED = "streamSorted"
        internal const val FUNCTION_NAME_UPDATE = "update"
        internal const val FUNCTION_NAME_UPDATE_ALL = "updateAll"
        internal const val FUNCTION_NAME_UPDATE_ALL_MULTIPLE = "updateAllMultiple"
        internal const val FUNCTION_NAME_UPDATE_MULTIPLE = "updateMultiple"
        internal const val FUNCTION_NAME_WHERE = "where"
        internal const val FUNCTION_NAME_WHERE_UPDATE = "whereUpdate"
        internal const val PARAM_NAME_EXPRESSION = "expression"
        internal const val PARAM_NAME_SETTER = "setter"
        internal const val PARAM_NAME_SETTERS = "setters"
        internal const val PARAM_NAME_SORT = "sort"
        internal const val SUFFIX_FILE_EXTENSIONS = "Extensions"
        //endregion
        //region Names
        internal const val HIBERNATE_PANACHE_COMPANION_BASE: String =
            "io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase"
        //endregion

        internal fun jvmNameAnnotation(name: String) = AnnotationSpec.builder(JvmNameClassName)
            .addMember("%S", name)
            .build()
    }
}

class PanacheCompanionBaseProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        PanacheCompanionBaseProcessor(environment.options, environment.codeGenerator, environment.logger)
}
