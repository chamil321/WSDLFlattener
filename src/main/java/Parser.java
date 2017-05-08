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

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class is written to parse WSDL using XOM library
 */
public class Parser {
    private Builder parser;
    private Document doc = null;

    public Parser(){
        parser = new Builder();
    }

    public Document getWSDL(String input) {

        try {
            doc = parser.build(input);
        } catch (ParsingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("IOException: " + e);
        }
        return doc;
    }

    public Document getWSDL(InputStream inputStream) {

        try {
            doc = parser.build(inputStream);
        } catch (ParsingException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return doc;
    }
}
