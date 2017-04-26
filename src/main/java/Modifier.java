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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * This class is written to reproduce WSDL with reference contents
 */
public class Modifier {

    private static final String TYPES = "wsdl:types";
    private static final String Schema = "xsd:schema";
    private static final String SCHEMALOCATION = "schemaLocation";
    private static final int unauthorized = 401;

    private static Parser parser;
    private static Document document,subDocument;
    private static Element root,subRoot;
    private static Properties prop = new Properties();
    private static InputStream input = null;
    private static boolean refAvailable = false;
    private static final Logger log = Logger.getLogger(Modifier.class.getName());

    public static void main(String[] args){

        try {
            input = new FileInputStream("config.properties");
            prop.load(input);
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        parser = new Parser();
        document = parser.getWSDL(prop.getProperty("url"));
        root = document.getRootElement();
        flattenWSDL(root,0);
        if(refAvailable)
            restructure(root, 0);
        String result = document.toXML();
        System.out.println(result);
        //writeFile(result);
    }

    public  static void flattenWSDL(Element current, int depth) {
        if(current.getAttributeCount()!=0) {
            for (int i=0;i<current.getAttributeCount();i++){
                if(current.getAttribute(i).getQualifiedName().equalsIgnoreCase(SCHEMALOCATION)){
                    String attributeValue = current.getAttribute(i).getValue();
                    importContent(current.getParent(), attributeValue);
                    refAvailable = true;
                }
            }
        }
        Elements children = current.getChildElements();
        for (int i = 0; i < children.size(); i++) {
            flattenWSDL(children.get(i), depth+1);
        }
    }

    public static void importContent(ParentNode parent, String attributeValue) {
        log.info("Importing.....");
        int serverResponse = 0;
        URL url;
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            url = new URL(attributeValue);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            serverResponse = connection.getResponseCode();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(serverResponse==unauthorized){
            String authString = prop.getProperty("username")+":"+prop.getProperty("password");
            String encoding = org.apache.xerces.impl.dv.util.Base64.encode(authString.getBytes());
            try {
                url = new URL(attributeValue);
                connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.setRequestProperty  ("Authorization", "Basic " + encoding);
                inputStream = (InputStream) connection.getInputStream();
                subDocument = parser.getWSDL(inputStream);
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            subDocument = parser.getWSDL(attributeValue);
        }
        subRoot = subDocument.getRootElement();
        for(int k=0;k<parent.getChildCount();k++) {
            if (parent.getChild(k) instanceof Element) {
                Element child = (Element) parent.getChild(k);
                for (int j = 0; j < child.getAttributeCount(); j++) {
                    if (child.getAttribute(j).getValue().toString().equalsIgnoreCase(attributeValue.toString())) {
                        //parent.replaceChild(parent.getChild(k), subRoot.copy());
                        parent.getParent().appendChild(subRoot.copy());
                        //parent.getParent().replaceChild(parent, subRoot.copy());
                    }
                }
            }
        }
    }

    public static void restructure(Element current,int depth) {
        if (current.getQualifiedName().equalsIgnoreCase(TYPES)) {
            for(int i=0;i<current.getChildElements().size();i++){
                if(current.getChildElements().get(i).getQualifiedName().equalsIgnoreCase(Schema)){
                    current.getChildElements().get(i).detach();
                }
            }
        }
        Elements children = current.getChildElements();
        for (int i = 0; i < children.size(); i++) {
            restructure( children.get(i), depth+1);
        }
    }

    public static void writeFile(String result) {
        BufferedWriter bw = null;
        FileWriter fw = null;
        String FILENAME = "Result.txt";

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
        }


    }
}


