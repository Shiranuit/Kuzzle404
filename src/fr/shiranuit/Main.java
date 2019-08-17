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

        Crawler crawler = new Crawler("https://kuzzle.io/", new String[] {
                "kuzzle.io",
                "dl.kuzzle.io",
                "docs.kuzzle.io",
                "info.kuzzle.io",
                "blog.kuzzle.io",
        });
        crawler.run();
    }
}
