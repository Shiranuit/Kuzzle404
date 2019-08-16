package fr.shiranuit;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {

    public static void main(String[] args) throws IOException {

//        URL url = new URL("https://next-docs.kuzzle.io/core/1/guides/getting-started/running-kuzzle/");
//        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
//        connection.setInstanceFollowRedirects(true);
//        connection.setRequestMethod("GET");
//
//        connection.getResponseCode();
//
//        for (String link : Crawler.getLinks(connection)) {
//            System.out.println(link);
//        }

        Crawler crawler = new Crawler("https://next-docs.kuzzle.io/core/1/guides/getting-started/running-kuzzle/");
        crawler.run();
    }
}
