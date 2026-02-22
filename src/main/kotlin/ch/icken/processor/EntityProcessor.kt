/*
 * Copyright 2026 Thijs Koppen
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

import ch.icken.processor.model.KSClassDeclarationWrapper
import com.google.devtools.ksp.symbol.KSDeclaration

internal sealed class EntityProcessor(options: Map<String, String>) : Processor(options) {

    protected val KSClassDeclarationWrapper.columnsBaseClassName get() = ksClassDeclaration.columnsBaseClassName

    protected val KSDeclaration.columnsBaseClassName get() = columnsObjectName + SUFFIX_CLASS_COLUMNS_BASE

    companion object {
        //region Constants
        protected const val PARAM_NAME_CLASS_COLUMNS_BASE_CONSTRUCTOR = "parent"
        private const val SUFFIX_CLASS_COLUMNS_BASE = "Base"
        //endregion
    }
}
