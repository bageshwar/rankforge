/*
 *
 *  *Copyright [2024] [Bageshwar Pratap Narain]
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.rankforge.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Main Application
 * NOTE: This standalone application requires Spring Boot context to use JPA repositories.
 * Consider migrating to Spring Boot or using the server module instead.
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class RankForgeApplication {
    private static final Logger logger = LoggerFactory.getLogger(RankForgeApplication.class);

    public static void main(String[] args) {
        logger.error("RankForgeApplication standalone mode is not supported with JPA. " +
                "Please use the Spring Boot server module (rank-forge-server) instead, " +
                "or migrate this application to use Spring Boot.");
        System.exit(1);
    }
}
