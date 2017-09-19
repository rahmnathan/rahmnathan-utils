package com.github.rahmnathan.http.control;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class HttpClient {
    private static final Logger logger = Logger.getLogger(HttpClient.class.getName());

    public static String getResponseAsString(String url) {
        HttpURLConnection connection = getUrlConnection(url);

        if (connection != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                reader.lines().forEachOrdered(response::append);

                return response.toString();
            } catch (IOException e) {
                logger.severe(e.toString());
            } finally {
                connection.disconnect();
            }
        }

        return "";
    }

    public static byte[] getResponseAsBytes(String urlString){
        HttpURLConnection connection = getUrlConnection(urlString);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if(connection != null) {
            try (InputStream inputStream = connection.getInputStream()) {
                byte[] buffer = new byte[1024];
                int i;

                while (-1 != (i = inputStream.read(buffer))) {
                    baos.write(i);
                }

                return baos.toByteArray();
            } catch (Exception e) {
                logger.severe(e.toString());
            }
        }

        return new byte[0];
    }

    private static HttpURLConnection getUrlConnection(String urlString){
        try {
            return  (HttpURLConnection) new URL(urlString).openConnection();
        } catch (IOException e) {
            logger.severe(e.toString());
            return null;
        }
    }
}