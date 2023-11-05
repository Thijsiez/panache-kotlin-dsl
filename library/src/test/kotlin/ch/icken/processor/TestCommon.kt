/*
 * Copyright 2023 Thijs Koppen
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

import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import io.mockk.mockkStatic
import org.junit.jupiter.api.BeforeAll

abstract class TestCommon {
    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            mockkStatic(KSAnnotated::hasAnnotation)
            mockkStatic(KSAnnotation::isClass)
            mockkStatic(KSClassDeclaration::isSubclass)
            mockkStatic(KSDeclaration::isClass)
            mockkStatic(KSNode::validate)
        }
    }
}