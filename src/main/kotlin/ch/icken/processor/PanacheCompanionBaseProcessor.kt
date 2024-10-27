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

        val className = ksClass.toClassName()
        val columnsObjectClassName = ClassName(packageName, classSimpleName + SUFFIX_OBJECT_COLUMNS)

        val idClassName = ksClass.getAllProperties()
            .filter { it.hasAnnotation(JakartaPersistenceId) }
            .single()
            .type.resolve()
            .toClassName()

        val expressionParameterType = LambdaTypeName.get(
            receiver = columnsObjectClassName,
            returnType = BooleanExpressionClassName
        )
        val queryComponentType = QueryComponentClassName
            .plusParameter(className)
            .plusParameter(idClassName)
        val groupComponentParameterType = LambdaTypeName.get(
            receiver = queryComponentType,
            returnType = queryComponentType
        )

        val whereReceiver = className.nestedClass("Companion")
        //region where
        val where = FunSpec.builder(FUNCTION_NAME_WHERE)
            .addModifiers(KModifier.INLINE)
            .receiver(whereReceiver)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .returns(queryComponentType)
            .addStatement("return %M($PARAM_NAME_EXPRESSION(%T))",
                MemberName(QueryComponentClassName.packageName, FUNCTION_NAME_WHERE), columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_WHERE$classSimpleName"))
        val whereGroup = FunSpec.builder(FUNCTION_NAME_WHERE_GROUP)
            .receiver(whereReceiver)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .addParameter(PARAM_NAME_GROUP_COMPONENT, groupComponentParameterType)
            .returns(queryComponentType)
            .addStatement("return %M($PARAM_NAME_EXPRESSION(%T), $PARAM_NAME_GROUP_COMPONENT)",
                MemberName(QueryComponentClassName.packageName, FUNCTION_NAME_WHERE_GROUP), columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_WHERE_GROUP$classSimpleName"))
        //endregion

        //region and
        val and = FunSpec.builder(FUNCTION_NAME_AND)
            .addModifiers(KModifier.INLINE)
            .receiver(queryComponentType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .returns(AndQueryComponentClassName.plusParameter(className).plusParameter(idClassName))
            .addStatement("return $FUNCTION_NAME_AND($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_AND$classSimpleName"))
        val andGroup = FunSpec.builder(FUNCTION_NAME_AND_GROUP)
            .receiver(queryComponentType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .addParameter(PARAM_NAME_GROUP_COMPONENT, groupComponentParameterType)
            .returns(queryComponentType)
            .addStatement("return $FUNCTION_NAME_AND_GROUP($PARAM_NAME_EXPRESSION(%T), $PARAM_NAME_GROUP_COMPONENT)",
                columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_AND_GROUP$classSimpleName"))
        //endregion

        //region or
        val or = FunSpec.builder(FUNCTION_NAME_OR)
            .addModifiers(KModifier.INLINE)
            .receiver(queryComponentType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .returns(OrQueryComponentClassName.plusParameter(className).plusParameter(idClassName))
            .addStatement("return $FUNCTION_NAME_OR($PARAM_NAME_EXPRESSION(%T))", columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_OR$classSimpleName"))
        val orGroup = FunSpec.builder(FUNCTION_NAME_OR_GROUP)
            .receiver(queryComponentType)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .addParameter(PARAM_NAME_GROUP_COMPONENT, groupComponentParameterType)
            .returns(queryComponentType)
            .addStatement("return $FUNCTION_NAME_OR_GROUP($PARAM_NAME_EXPRESSION(%T), $PARAM_NAME_GROUP_COMPONENT)",
                columnsObjectClassName)
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_OR_GROUP$classSimpleName"))
        //endregion

        //region count, delete, find, stream
        val count = FunSpec.builder(FUNCTION_NAME_COUNT)
            .addModifiers(KModifier.INLINE)
            .receiver(whereReceiver)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .returns(LongClassName)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_COUNT()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_COUNT$classSimpleName"))

        val delete = FunSpec.builder(FUNCTION_NAME_DELETE)
            .addModifiers(KModifier.INLINE)
            .receiver(whereReceiver)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .returns(LongClassName)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_DELETE()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_DELETE$classSimpleName"))

        val findReturns = PanacheQueryClassName.plusParameter(className)
        val find = FunSpec.builder(FUNCTION_NAME_FIND)
            .addModifiers(KModifier.INLINE)
            .receiver(whereReceiver)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .returns(findReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_FIND()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_FIND$classSimpleName"))
        val findSorted = FunSpec.builder(FUNCTION_NAME_FIND)
            .addModifiers(KModifier.INLINE)
            .receiver(whereReceiver)
            .addParameter(PARAM_NAME_SORT, SortClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .returns(findReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_FIND($PARAM_NAME_SORT)")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_FIND_SORTED$classSimpleName"))

        val streamReturn = StreamClassName.plusParameter(className)
        val stream = FunSpec.builder(FUNCTION_NAME_STREAM)
            .addModifiers(KModifier.INLINE)
            .receiver(whereReceiver)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .returns(streamReturn)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_STREAM()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_STREAM$classSimpleName"))
        val streamSorted = FunSpec.builder(FUNCTION_NAME_STREAM)
            .addModifiers(KModifier.INLINE)
            .receiver(whereReceiver)
            .addParameter(PARAM_NAME_SORT, SortClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .returns(streamReturn)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).$FUNCTION_NAME_STREAM($PARAM_NAME_SORT)")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_STREAM_SORTED$classSimpleName"))
        //endregion

        //region single
        val single = FunSpec.builder(FUNCTION_NAME_SINGLE)
            .addModifiers(KModifier.INLINE)
            .receiver(whereReceiver)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .returns(className)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).getSingle()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_SINGLE$classSimpleName"))
        val singleSafe = FunSpec.builder(FUNCTION_NAME_SINGLE_SAFE)
            .addModifiers(KModifier.INLINE)
            .receiver(whereReceiver)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .returns(PanacheSingleResultClassName.plusParameter(WildcardTypeName.producerOf(className)))
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).getSingleSafe()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_SINGLE_SAFE$classSimpleName"))
        //endregion

        //region multiple
        val multipleReturns = ListClassName.plusParameter(className)
        val multiple = FunSpec.builder(FUNCTION_NAME_MULTIPLE)
            .addModifiers(KModifier.INLINE)
            .receiver(whereReceiver)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .returns(multipleReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).getMultiple()")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_MULTIPLE$classSimpleName"))
        val multipleSorted = FunSpec.builder(FUNCTION_NAME_MULTIPLE)
            .addModifiers(KModifier.INLINE)
            .receiver(whereReceiver)
            .addParameter(PARAM_NAME_SORT, SortClassName)
            .addParameter(PARAM_NAME_EXPRESSION, expressionParameterType)
            .returns(multipleReturns)
            .addStatement("return $FUNCTION_NAME_WHERE($PARAM_NAME_EXPRESSION).getMultiple($PARAM_NAME_SORT)")
            .addAnnotation(jvmNameAnnotation("$FUNCTION_NAME_MULTIPLE_SORTED$classSimpleName"))
        //endregion

        val functions = listOf(where, whereGroup, and, andGroup, or, orGroup,
            count, delete, find, findSorted, stream, streamSorted,
            single, singleSafe, multiple, multipleSorted)

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
