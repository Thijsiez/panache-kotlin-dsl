/*
 * Copyright 2024-2025 Thijs Koppen
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

import ch.icken.query.Column
import com.tschuchort.compiletesting.*
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.*
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

@OptIn(ExperimentalCompilerApi::class)
abstract class ProcessorCompileTestCommon {

    protected fun compilation(vararg source: SourceFile) = KotlinCompilation().apply {
        inheritClassPath = true
        kspWithCompilation = true
        sources = listOf(*source)
        symbolProcessorProviders = listOf(
            PanacheCompanionBaseProcessorProvider(),
            PanacheEntityBaseProcessorProvider()
        )
        verbose = false
    }

    protected fun kotlinCompilation(sourceFileName: String, @Language("kotlin") contents: String) =
        compilation(SourceFile.kotlin(sourceFileName, contents))

    protected fun JvmCompilationResult.assertOk() = assertEquals(KotlinCompilation.ExitCode.OK, exitCode)

    protected fun KotlinCompilation.assertNumberOfFiles(expectedNumberOfFiles: Int, outputDir: String = "generated") =
        assertEquals(expectedNumberOfFiles, kspSourcesDir.resolve("kotlin").resolve(outputDir)
            .listFiles()?.filter { it.isFile }?.size ?: 0)

    protected fun KotlinCompilation.assertHasFile(fileName: String, outputDir: String = "generated") =
        assertTrue(kspSourcesDir.resolve("kotlin").resolve(outputDir).resolve(fileName).isFile)

    protected fun JvmCompilationResult.loadClass(className: String, outputPackage: String = "generated"): Class<*> =
        this.classLoader.loadClass(if (outputPackage.isBlank()) className else "$outputPackage.$className")

    protected fun Class<*>.assertNumberOfMemberProperties(expectedNumberOfProperties: Int) {
        assertEquals(expectedNumberOfProperties, kotlin.memberProperties.size)
    }

    protected fun Class<*>.assertHasMemberPropertyOfType(propertyName: String, expectedTypeName: String) {
        val property = kotlin.memberProperties.find { it.name == propertyName }
        assertNotNull(property)

        val returnType = property!!.returnType
        returnType.classifier.let { classifier ->
            assertNotNull(classifier)
            assertTrue(classifier is KClass<*>)
            assertEquals(expectedTypeName, (classifier as? KClass<*>)?.simpleName)
        }
    }

    protected fun Class<*>.assertHasColumnOfType(columnName: String, expectedType: KClass<*>) {
        val property = kotlin.memberProperties.find { it.name == columnName }
        assertNotNull(property)

        val returnType = property!!.returnType
        returnType.classifier.let { classifier ->
            assertNotNull(classifier)
            assertTrue(classifier is KClass<*>)
            assertEquals(Column::class, classifier)
        }

        val arguments = returnType.arguments
        assertNotNull(arguments)
        assertEquals(2, arguments.size)

        val type = arguments[1].type
        assertNotNull(type)
        type!!.classifier.let { classifier ->
            assertNotNull(classifier)
            assertTrue(classifier is KClass<*>)
            assertEquals(expectedType, classifier)
        }
    }

    protected fun Class<*>.assertNumberOfDeclaredMethods(expectedNumberOfMethods: Int) {
        assertEquals(expectedNumberOfMethods, declaredMethods.size)
    }

    protected fun Class<*>.assertHasDeclaredMethodWithName(methodName: String) {
        assertNotNull(declaredMethods.find { it.name == methodName })
    }
}
