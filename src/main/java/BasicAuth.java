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
import java.util.Scanner;

/**
 * This class is written to implement basic auth authentication
 */
public class BasicAuth extends Authenticator{

    private String username,password;
    private URL url;
    private HttpURLConnection connection = null;
    private InputStream inputStream;

    public BasicAuth(String username,String password){
        this.username=username;
        this.password=password;
    }


    public InputStream authenticate(String urlString) {
        String authString = username+":"+password;
        String encoding = org.apache.xerces.impl.dv.util.Base64.encode(authString.getBytes());
        try {
            url = new URL(urlString);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setRequestProperty  ("Authorization", "Basic " + encoding);
            if(connection.getResponseCode() == 403){
                Scanner scanner = new Scanner(System.in);
                System.out.print("Authentication error!\nEnter credentials:" + urlString + "\nUsername: ");
                String username = scanner.next();
                System.out.print("Password: ");
                String password = scanner.next();

                Authenticator authenticator = new BasicAuth(username.toString(),password.toString());
                return  authenticator.authenticate(urlString);

            }else {
                inputStream = connection.getInputStream();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inputStream;

    }
}
