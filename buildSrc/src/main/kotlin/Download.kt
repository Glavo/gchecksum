/*
 * Copyright 2023 Glavo
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

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.net.*

fun Project.downloadFile(url: String, file: File): File {
    try {
        file.parentFile.mkdirs()

        val connection = URL(url).openConnection()
        connection.connect()

        val length = connection.contentLengthLong

        if (connection is HttpURLConnection && file.exists()
            && length == file.length()
            && connection.lastModified == file.lastModified()
        ) {
            logger.info("$url has not changed")
            connection.disconnect()
            return file
        }

        logger.info("Download $url to $file")

        file.outputStream().use { output ->
            connection.getInputStream().use { input ->
                input.transferTo(output)
            }
        }

        if (connection is HttpURLConnection && connection.lastModified > 0) {
            file.setLastModified(connection.lastModified)
        }

        return file
    } catch (e: Throwable) {
        throw GradleException("Failed to download from $url to $file ", e)
    }
}
