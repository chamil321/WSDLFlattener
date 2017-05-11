/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * This class is written to create singleton logger
 */
public class ActionLogger {

    public static final Logger log = Logger.getLogger("Action");
    private static ActionLogger instance = null;

    public static ActionLogger getInstance(){
        if(instance == null){
            createLogger();
            instance = new ActionLogger();
        }
        return  instance;
    }

    private static void createLogger(){
        FileHandler fileHandler;
        try {
            // This block configure the logger with handler and formatter
            fileHandler = new FileHandler("ActionFlow.log");
            log.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            log.setUseParentHandlers(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
