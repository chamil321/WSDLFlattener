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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * This class is written to reproduce WSDL with reference contents
 */
public class Modifier {

    private static final String WSDL_URL = "wsdl_url";
    private static final String SchemaLocation = "schemaLocation";

    private static Parser parser;
    private static Document subDocument;
    private static ParentNode grandParent = null;
    private static boolean isfirst = true;
    private static boolean isFirstPrompt = true;
    private static Properties prop = new Properties();
    private static ArrayList<List<String>> credentialList;
    private static ArrayList<String> credList;
    private static final Logger log = Logger.getLogger(Modifier.class.getName());



    public static void main(String[] args){

        getInputData();
        parser = new Parser();
        Document document = parser.getWSDL(prop.getProperty(WSDL_URL));
        Element root = document.getRootElement();
        flattenWSDL(root,0);
        String result = document.toXML();
        System.out.println(result);
        //writeFile(result);
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
        Elements childrenCopy = current.getChildElements();
        for (int i = 0; i < childrenCopy.size(); i++) {
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
        if(serverResponse==401){
            if(isFirstPrompt) {
                credList = new ArrayList();
                Scanner scanner = new Scanner(System.in);
                System.out.print("Enter credentials:" + attributeValue + "\nUsername: ");
                credList.add(scanner.next());
                System.out.print("Password: ");
                credList.add(scanner.next());
                isFirstPrompt = false;
            }
            Authenticator authenticator = new BasicAuth(credList.get(0).toString(),credList.get(1).toString());
            subDocument = parser.getWSDL(authenticator.authenticate(attributeValue));

        }else if(serverResponse==403){
            System.out.println("Error- 403");
        }else{
            subDocument = parser.getWSDL(attributeValue);
        }
        Element subRoot = subDocument.getRootElement();
        for(int k=0;k<parent.getChildCount();k++) {
            if (parent.getChild(k) instanceof Element) {
                Element child = (Element) parent.getChild(k);
                for (int j = 0; j < child.getAttributeCount(); j++) {
                    if (child.getAttribute(j).getValue().toString().equalsIgnoreCase(attributeValue.toString())) {
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

    /*public static void restructure(Element current,int depth) {
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
    }*/

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
        } catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    public static void getInputData() {
        try {
            prop.load(new FileInputStream("config.properties"));
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //credentialList = new ArrayList();
        /*for (final Map.Entry<Object, Object> entry : prop.entrySet()) {
            String key = (String) entry.getKey();
            if(!key.equalsIgnoreCase(WSDL_URL)){
                List<String> reference = Arrays.asList(entry.getValue().toString().split(","));
                //System.out.println(reference.get(0));
                credentialList.add(reference);
            }
        }*/
    }

    private static List getCredentials(String attributeValue) {
        for (java.util.List<String> reference :credentialList) {
            if(reference.get(0).equalsIgnoreCase(attributeValue)){
                return reference;
            }
        }
        return null;
    }
}


