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

package com.rankforge.server.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to serve React SPA
 * Serves index.html for all routes to support client-side routing
 * Author bageshwar.pn
 * Date 2024
 */
@Controller
public class ReactAppController {

    /**
     * Serve React app for root path
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    /**
     * Catch-all handler for React Router
     * Serves index.html for all non-API routes
     */
    @GetMapping(value = {
        "/rankings",
        "/games",
        "/games/**"
    })
    public String serveReactApp() {
        return "forward:/index.html";
    }
}
