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

package ch.icken.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class PanacheCompanionBaseProcessor(
    options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : ProcessorCommon(options), SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (valid, invalid) = resolver.getSymbolsWithAnnotation(JakartaPersistenceEntity)
            .partition(KSAnnotated::validate)

        valid.filterIsInstance<KSClassDeclaration>()
            .filter { it.isSubclass(HibernatePanacheEntityBase) }
            .filter { ksClassDeclaration ->
                ksClassDeclaration.declarations.filterIsInstance<KSClassDeclaration>()
                    .filter(KSClassDeclaration::isCompanionObject)
                    .filter { it.isSubclass(HibernatePanacheCompanionBase) }.any()
            }.forEach(::createEntityExtensions)

        return invalid
    }

    internal fun createEntityExtensions(ksClass: KSClassDeclaration) {
        val packageName = ksClass.packageName.asString() + SUFFIX_PACKAGE_GENERATED
        val classSimpleName = ksClass.simpleName.asString()
        val extensionFileName = classSimpleName + SUFFIX_FILE_EXTENSIONS
        logger.info("Generating $packageName.$extensionFileName")

        //region Names and types
        val className = ksClass.toClassName()
        val columnsObjectClassName = ClassName(packageName, classSimpleName + SUFFIX_OBJECT_COLUMNS)
        val companionClassName = className.nestedClass(CLASS_NAME_COMPANION)
        val idClassName = ksClass.getAllProperties()
            .filter { it.hasAnnotation(JakartaPersistenceId) }
            .single()
            .type.resolve()
            .toClassName()

        val expressionType = ExpressionClassName.plusParameter(columnsObjectClassName)
        val expressionParameterLambdaType = LambdaTypeName.get(
            receiver = columnsObjectClassName,
            returnType = expressionType
        )
        val queryComponentType = QueryComponentClassName
            .plusParameter(className)
            .plusParameter(idClassName)
            .plusParameter(columnsObjectClassName)

        val setterExpressionParameterLambdaType = LambdaTypeName.get(
            receiver = columnsObjectClassName,
            returnType = SetterExpressionClassName
        )
        val initialUpdateComponentType = InitialUpdateComponentClassName
            .plusParameter(className)
            .plusParameter(idClassName)
            .plusParameter(columnsObjectClassName)
        val logicalUpdateComponentType = LogicalUpdateComponentClassName
            .plusParameter(className)
            .plusParameter(idClassName)
            .plusParameter(columnsObjectClassName)
        //endregion

        //region where, and, or
        //TODO kdoc
        val where = FunSpec.builder(FUNCTION_NAME_WHERE)
            .addModifiers(KModifier.INLINE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(queryComponentType)
            .addStatement("return %M($PARAM_NAME_EXPRESSION(%T))",
                MemberName(QueryComponentClassName.packageName, FUNCTION_NAME_WHERE), columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_WHERE$classSimpleName"))

        //TODO kdoc
        val and = FunSpec.builder(FUNCTION_NAME_AND)
            .addModifiers(KModifier.INLINE)
            .receiver(queryComponentType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(queryComponentType)
            .addStatement("return $FUNCTION_NAME_AND($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_AND$classSimpleName"))
        //TODO kdoc
        val or = FunSpec.builder(FUNCTION_NAME_OR)
            .addModifiers(KModifier.INLINE)
            .receiver(queryComponentType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(queryComponentType)
            .addStatement("return $FUNCTION_NAME_OR($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_OR$classSimpleName"))
        //endregion

        //region count, delete, find, stream
        //TODO kdoc
        val count = FunSpec.builder(FUNCTION_NAME_COUNT)
            .addModifiers(KModifier.INLINE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(LongClassName)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_COUNT()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_COUNT$classSimpleName"))

        //TODO kdoc
        val delete = FunSpec.builder(FUNCTION_NAME_DELETE)
            .addModifiers(KModifier.INLINE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(LongClassName)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_DELETE()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_DELETE$classSimpleName"))

        val findReturns = PanacheQueryClassName.plusParameter(className)
        //TODO kdoc
        val find = FunSpec.builder(FUNCTION_NAME_FIND)
            .addModifiers(KModifier.INLINE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(findReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_FIND()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_FIND$classSimpleName"))
        //TODO kdoc
        val findSorted = FunSpec.builder(FUNCTION_NAME_FIND)
            .addModifiers(KModifier.INLINE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_SORT, SortClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(findReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_FIND($PARAM_NAME_SORT)")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_FIND_SORTED$classSimpleName"))

        val streamReturns = StreamClassName.plusParameter(className)
        //TODO kdoc
        val stream = FunSpec.builder(FUNCTION_NAME_STREAM)
            .addModifiers(KModifier.INLINE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(streamReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_STREAM()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_STREAM$classSimpleName"))
        //TODO kdoc
        val streamSorted = FunSpec.builder(FUNCTION_NAME_STREAM)
            .addModifiers(KModifier.INLINE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_SORT, SortClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(streamReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_STREAM($PARAM_NAME_SORT)")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_STREAM_SORTED$classSimpleName"))
        //endregion

        //region single, multiple
        //TODO kdoc
        val single = FunSpec.builder(FUNCTION_NAME_SINGLE)
            .addModifiers(KModifier.INLINE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(className)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).single()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_SINGLE$classSimpleName"))
        //TODO kdoc
        val singleSafe = FunSpec.builder(FUNCTION_NAME_SINGLE_SAFE)
            .addModifiers(KModifier.INLINE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(PanacheSingleResultClassName.plusParameter(WildcardTypeName.producerOf(className)))
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).singleSafe()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_SINGLE_SAFE$classSimpleName"))

        val multipleReturns = ListClassName.plusParameter(className)
        //TODO kdoc
        val multiple = FunSpec.builder(FUNCTION_NAME_MULTIPLE)
            .addModifiers(KModifier.INLINE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(multipleReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).multiple()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_MULTIPLE$classSimpleName"))
        //TODO kdoc
        val multipleSorted = FunSpec.builder(FUNCTION_NAME_MULTIPLE)
            .addModifiers(KModifier.INLINE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_SORT, SortClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(multipleReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).multiple($PARAM_NAME_SORT)")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_MULTIPLE_SORTED$classSimpleName"))
        //endregion

        //region update, updateAll
        val updateExtensionFunction = MemberName(InitialUpdateComponentClassName.packageName, FUNCTION_NAME_UPDATE)
        //TODO kdoc
        val update = FunSpec.builder(FUNCTION_NAME_UPDATE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_SETTER, setterExpressionParameterLambdaType)
            .returns(initialUpdateComponentType)
            .addStatement("return %M(%T, $PARAM_NAME_SETTER)", updateExtensionFunction, columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_UPDATE$classSimpleName"))
        //TODO kdoc
        val updateMultiple = FunSpec.builder(FUNCTION_NAME_UPDATE)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_SETTERS, setterExpressionParameterLambdaType, KModifier.VARARG)
            .returns(initialUpdateComponentType)
            .addStatement("return %M(%T, $PARAM_NAME_SETTERS)", updateExtensionFunction, columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_UPDATE_MULTIPLE$classSimpleName"))

        //TODO kdoc
        val updateAll = FunSpec.builder(FUNCTION_NAME_UPDATE_ALL)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_SETTER, setterExpressionParameterLambdaType)
            .returns(IntClassName)
            .addStatement("return %M(%T, $PARAM_NAME_SETTER).executeWithoutWhere()",
                updateExtensionFunction, columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_UPDATE_ALL$classSimpleName"))
        //TODO kdoc
        val updateAllMultiple = FunSpec.builder(FUNCTION_NAME_UPDATE_ALL)
            .receiver(companionClassName)
            .addParameter(PARAM_NAME_SETTERS, setterExpressionParameterLambdaType, KModifier.VARARG)
            .returns(IntClassName)
            .addStatement("return %M(%T, $PARAM_NAME_SETTERS).executeWithoutWhere()",
                updateExtensionFunction, columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_UPDATE_ALL_MULTIPLE$classSimpleName"))
        //endregion

        //region whereUpdate, andUpdate, orUpdate
        //TODO kdoc
        val whereUpdate = FunSpec.builder(FUNCTION_NAME_WHERE)
            .addModifiers(KModifier.INLINE)
            .receiver(initialUpdateComponentType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(logicalUpdateComponentType)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_WHERE_UPDATE$classSimpleName"))

        //TODO kdoc
        val andUpdate = FunSpec.builder(FUNCTION_NAME_AND)
            .addModifiers(KModifier.INLINE)
            .receiver(logicalUpdateComponentType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(logicalUpdateComponentType)
            .addStatement("return $FUNCTION_NAME_AND($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_AND_UPDATE$classSimpleName"))
        //TODO kdoc
        val orUpdate = FunSpec.builder(FUNCTION_NAME_OR)
            .addModifiers(KModifier.INLINE)
            .receiver(logicalUpdateComponentType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(logicalUpdateComponentType)
            .addStatement("return $FUNCTION_NAME_OR($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_OR_UPDATE$classSimpleName"))
        //endregion

        //region andExpression, orExpression
        //TODO kdoc
        val andExpression = FunSpec.builder(FUNCTION_NAME_AND)
            .addModifiers(KModifier.INLINE)
            .receiver(expressionType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(expressionType)
            .addStatement("return $FUNCTION_NAME_AND($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_AND_EXPRESSION$classSimpleName"))
        //TODO kdoc
        val orExpression = FunSpec.builder(FUNCTION_NAME_OR)
            .addModifiers(KModifier.INLINE)
            .receiver(expressionType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterLambdaType)
            .returns(expressionType)
            .addStatement("return $FUNCTION_NAME_OR($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_OR_EXPRESSION$classSimpleName"))
        //endregion

        val functions = listOf(where, and, or,
            count, delete, find, findSorted, stream, streamSorted,
            single, singleSafe, multiple, multipleSorted,
            update, updateMultiple, updateAll, updateAllMultiple,
            whereUpdate, andUpdate, orUpdate,
            andExpression, orExpression)

        FileSpec.builder(packageName, extensionFileName)
            .apply {
                functions.onEach { it.addAnnotationIf(generatedAnnotation, addGeneratedAnnotation) }
                    .map(FunSpec.Builder::build)
                    .forEach(::addFunction)
            }.addAnnotation(suppressFileAnnotation)
            .addAnnotationIf(generatedAnnotation, addGeneratedAnnotation)
            .build()
            .writeTo(codeGenerator, Dependencies(false))
    }
}

class PanacheCompanionBaseProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        PanacheCompanionBaseProcessor(environment.options, environment.codeGenerator, environment.logger)
}
