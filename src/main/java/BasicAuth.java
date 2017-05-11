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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This class is written to implement basic auth authentication
 */
public class BasicAuth extends Authenticator{

    private InputStream inputStream;
    private ActionLogger logger;
    private ArrayList<List<String>> credentialList;
    private List<String> userPass;
    private static boolean isAuthError = false;

    public BasicAuth(){
        logger = ActionLogger.getInstance();
    }

    public InputStream authenticate(String urlString) {

        String username,password;
        Scanner scanner = new Scanner(System.in);
        List<String> reference;
        if(credentialList==null){
            credentialList = new ArrayList();
        }

        if(credentialList!=null && urlAvailable(urlString) && !isAuthError) {
            userPass = getCredentials(urlString);
            username = userPass.get(1);
            password = userPass.get(2);
            logger.log.info("Use credential map data");

        }else if(isAuthError){
            System.out.print("Authentication error!\nEnter credentials:" + urlString + "\nUsername: ");
            List<String> ref = getCredentials(urlString);
            ref.remove(1);
            ref.add(1,username=scanner.next());
            System.out.print("Password: ");
            ref.remove(2);
            ref.add(2,password=scanner.next());
            logger.log.info("replace data of credential map");
        }

        else{
            reference = new ArrayList<String>();
            reference.add(urlString);
            System.out.print("Enter credentials:" + urlString + "\n");
            System.out.print("Username: ");
            reference.add(username=scanner.next());
            System.out.print("Password: ");
            reference.add(password=scanner.next());
            credentialList.add(reference);
        }


        String authString = username+":"+password;
        String encoding = org.apache.xerces.impl.dv.util.Base64.encode(authString.getBytes());
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setRequestProperty  ("Authorization", "Basic " + encoding);
            logger.log.info("Access : "+urlString);
            if(connection.getResponseCode() == 403){
                return this.error403(urlString);
            }else if(connection.getResponseCode() == 200){
                inputStream = connection.getInputStream();
            }else{
                System.out.println("Error-"+connection.getResponseCode());
                logger.log.info("Error-"+connection.getResponseCode());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            logger.log.info(e.toString());
        } catch (ProtocolException e) {
            e.printStackTrace();
            logger.log.info(e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            logger.log.info(e.toString());
        }
        isAuthError = false;
        return inputStream;
    }

    public boolean urlAvailable(String url){
        for(List<String> reference :credentialList){
            if(reference.get(0).equalsIgnoreCase(url)){
                userPass = reference;
                return true;
            }
        }
        return false;
    }

    public List getCredentials(String url) {
        for (java.util.List<String> reference :credentialList) {
            if(reference.get(0).equalsIgnoreCase(url)){
                return reference;
            }
        }
        return null;
    }

    public InputStream error403(String url){
        isAuthError = true;
        return  this.authenticate(url);
    }
}
