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

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParentNode;
import nu.xom.ParsingException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    private static Modifier modifier = new Modifier();
    private static Document document,subDocument;
    private static Element root,subRoot;
    private static String SCHEMELOCATION = "schemaLocation";
    private static final Logger log = Logger.getLogger(Modifier.class.getName());
    private static Properties prop = new Properties();
    private static InputStream input = null;

    public static void main(String[] args) throws FileNotFoundException {
        input = new FileInputStream("config.properties");
        try {
            prop.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
        document = getWSDL(prop.getProperty("url"));
        root = document.getRootElement();
        modifier.flattenWSDL(root,0);
        String result = document.toXML();
        System.out.println(result);
    }

    public static Document getWSDL(String input) {
        Document doc = null;
        try {
            Builder parser = new Builder();
            doc = parser.build(input);
        } catch (ParsingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }

    public static Document getWSDLStream(InputStream inputStream) {
        Document doc = null;

            Builder parser = new Builder();
        try {
            doc = parser.build(inputStream);
        } catch (ParsingException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return doc;
    }


    public  void flattenWSDL(Element current, int depth) {
        if(current.getAttributeCount()!=0) {
            for (int i=0;i<current.getAttributeCount();i++){
                if(current.getAttribute(i).getQualifiedName().equalsIgnoreCase(SCHEMELOCATION)){
                    Attribute attribute = current.getAttribute(i);
                    String attributeValue = attribute.getValue();
                    importContent(current.getParent(), attributeValue);
                }
            }
        }
        Elements children = current.getChildElements();
        for (int i = 0; i < children.size(); i++) {
            flattenWSDL(children.get(i), depth+1);
        }
    }

    private void importContent(ParentNode parent, String attributeValue) {
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
        if(serverResponse==401){
            String authString = prop.getProperty("username")+":"+prop.getProperty("password");
            String encoding = org.apache.xerces.impl.dv.util.Base64.encode(authString.getBytes());
            try {
                url = new URL(attributeValue);
                connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.setRequestProperty  ("Authorization", "Basic " + encoding);
                inputStream = (InputStream) connection.getInputStream();
                subDocument = modifier.getWSDLStream(inputStream);
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }else{
            subDocument = modifier.getWSDL(attributeValue);
        }
        subRoot = subDocument.getRootElement();
        for(int k=0;k<parent.getChildCount();k++) {
            if (parent.getChild(k) instanceof Element) {
                Element child = (Element) parent.getChild(k);
                for (int j = 0; j < child.getAttributeCount(); j++) {
                    if (child.getAttribute(j).getValue().toString().equalsIgnoreCase(attributeValue.toString())) {
                        parent.replaceChild(parent.getChild(k), subRoot.copy());
                    }
                }
            }
        }
    }
}


