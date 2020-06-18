/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.datastore

import androidx.datastore.handlers.NoOpCorruptionHandler
import androidx.datastore.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * Public factory for creating DataStore instances.
 */
class DataStoreFactory {
    /**
     * Create an instance of SingleProcessDataStore. The user is responsible for ensuring that
     * there is never more than one DataStore acting on a file at a time.
     *
     * T is the type DataStore acts on. The type T must be immutable. Mutating a type used in
     * DataStore invalidates any guarantees that DataStore provides and will result in
     * potentially serious, hard-to-catch bugs. We strongly recommend using protocol buffers:
     * https://developers.google.com/protocol-buffers/docs/javatutorial - which provides
     * immutability guarantees, a simple API and efficient serialization.
     */
    @JvmOverloads
    fun <T> create(
        /**
         * Function which returns the file that the new DataStore will act on. The function
         * must return the same path every time. No two instances of PreferenceDataStore
         * should act on the same file at the same time.
         */
        produceFile: () -> File,
        /**
         * Serializer for the type T used with DataStore. The type T must be immutable.
         */
        serializer: Serializer<T>,
        /**
         * The corruptionHandler is invoked if DataStore encounters a
         * {@link Serializer.CorruptionException} when attempting to read data. CorruptionExceptions
         * are thrown when the data can not be de-serialized.
         */
        corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
        /**
         * Migrations are run before any access to data can occur. Migrations must be idempotent.
         */
        migrationProducers: List<() -> DataMigration<T>> = listOf(),
        /**
         * The scope in which IO operations and transform functions will execute.
         */
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    ): DataStore<T> =
        SingleProcessDataStore(
            produceFile = produceFile,
            serializer = serializer,
            corruptionHandler = corruptionHandler ?: NoOpCorruptionHandler(),
            initTasksList = listOf(DataMigrationInitializer.getInitializer(migrationProducers)),
            scope = scope
        )
}