/*
 * Copyright (C) 2017 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.vsct.tock.bot.admin.test.xray

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 *
 */
fun main(args: Array<String>) {
    logger.info { "Start tests" }
    val result = XrayService().executePlans()
    if (!result) {
        logger.error { "At least one test fail" }
        System.exit(1)
    } else {
        logger.info { "All tests pass" }
    }
}