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

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParentNode;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * This class is written to reproduce WSDL with reference contents
 */
public class Modifier {

    private static final String SchemaLocation = "schemaLocation";
    private static Parser parser;
    private static Authenticator authenticator;
    private static String wsdl_url;
    private static Document subDocument;
    private static ParentNode grandParent = null;
    private static boolean isfirst = true;
    private static boolean isFirstPrompt = true;
    private static ArrayList<String> credList;
    public static  ActionLogger logger = ActionLogger.getInstance();


    public static void main(String[] args){


        Scanner scanner = new Scanner(System.in);

        if (args.length > 0) {
            wsdl_url = args[0].toString();
        }else{
            System.out.println("Add WSDL URL (http://www.example.com/...) :");
            wsdl_url = scanner.next();
        }
        parser = new Parser();
        authenticator = new BasicAuth();
        Document document = parser.getWSDL(wsdl_url);
        Element root = document.getRootElement();
        flattenWSDL(root,0);
        String result = document.toXML();
        System.out.println(result);
        writeFile(result);
    }

    public  static void flattenWSDL(Element current, int depth) {
        if(current.getAttributeCount()!=0) {
            for (int i=0;i<current.getAttributeCount();i++){
                if(current.getAttribute(i).getQualifiedName().equalsIgnoreCase(SchemaLocation)){
                    importContent(current.getParent(), current.getAttribute(i).getValue());
                }
            }
        }
        Elements children = current.getChildElements();
        for (int i = 0; i < children.size(); i++) {
            flattenWSDL(children.get(i), depth+1);
        }
    }

    public static void importContent(ParentNode parent, String attributeValue) {
        int serverResponse = 0;
        URL url;
        HttpURLConnection connection = null;
        Scanner scanner = new Scanner(System.in);
        logger.log.info("importing : "+ attributeValue);
        String urlString = attributeValue;

        try {
            if(!attributeValue.contains("http")){

                url = new URL(attributeValue=wsdl_url.concat(attributeValue));
            }else {
                url = new URL(attributeValue);
            }
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            serverResponse = connection.getResponseCode();
        } catch (MalformedURLException e) {
            logger.log.info(e.toString());
        } catch (IOException e) {
            logger.log.info(e.toString());
        }
        if(serverResponse == 401){
            subDocument = parser.getWSDL(authenticator.authenticate(attributeValue));
        }else if(serverResponse == 200){
            subDocument = parser.getWSDL(attributeValue);
        }else{
            logger.log.info("Error-"+serverResponse);
            System.exit(1);
        }
        Element subRoot = subDocument.getRootElement();
        for(int k=0;k<parent.getChildCount();k++) {
            if (parent.getChild(k) instanceof Element) {
                Element child = (Element) parent.getChild(k);
                for (int j = 0; j < child.getAttributeCount(); j++) {
                    if (child.getAttribute(j).getValue().toString().equalsIgnoreCase(urlString.toString())) {
                        if(isfirst){
                            grandParent = parent.getParent();
                            grandParent.replaceChild(parent, subRoot.copy());
                            isfirst = false;
                        }else {
                            grandParent.appendChild(subRoot.copy());
                        }
                    }
                }
            }
        }
    }

    public static void writeFile(String result) {
        BufferedWriter bw = null;
        FileWriter fw = null;
        String FILENAME = "Result.wsdl";

        try {
            fw = new FileWriter(FILENAME);
            bw = new BufferedWriter(fw);
            bw.write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
    }
}


