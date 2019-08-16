package fr.shiranuit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Crawler {

    private static Pattern l1 = Pattern.compile("<a.*?href=\"([^\"]+)\".*?>", Pattern.MULTILINE);
    private static Pattern l2 = Pattern.compile("<link.*?href=\"([^\"]+)\".*?>", Pattern.MULTILINE);
    private static Pattern anchor = Pattern.compile("(.+?/?)#[^/]+$");

    private String url;

    private UniqueQueue<String> links = new UniqueQueue<>();
    private Queue<String> erroredLinks = new LinkedBlockingQueue<>();
    private Map<String, Set<String>> sources = new HashMap<>();
    private AtomicInteger visited = new AtomicInteger(0);
    private AtomicInteger speed = new AtomicInteger(0);
    private AtomicInteger lastVisitedCount = new AtomicInteger(0);
    private StopWatch time = new StopWatch();
    private StopWatch stopWatch = new StopWatch();
    private AtomicInteger running = new AtomicInteger(0);

    public Crawler(String url) {
        this.url = url;
    }

    public static Set<String> getLinks(HttpURLConnection handle) {
        Set<String> links = new HashSet<>();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(handle.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine + "\n");
            }
            in.close();

            Matcher m1 = l1.matcher(response);

            while (m1.find()) {
                links.add(m1.group(1));
            }

            Matcher m2 = l2.matcher(response);

            while (m2.find()) {
                links.add(m2.group(1));
            }
        } catch (IOException e) {

        }
        return links;
    }

    private static HttpURLConnection request(URI url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            //connection.setConnectTimeout(2000);
            //connection.setReadTimeout(4000);

            return connection;
        } catch (IOException e) {

        }
        return null;
    }

    private void proccessLink(UniqueQueue queue, String link, URI uri, Map<String, Set<String>> sources) {
        if (link.startsWith("/")) {
            Matcher mAnchor = anchor.matcher(link);
            String proccessedLink = "";

            if (mAnchor.find()) {
                proccessedLink = uri.getScheme() + "://" + uri.getHost() + mAnchor.group(1);
            } else {
                proccessedLink = uri.getScheme() + "://" + uri.getHost() + link;
            }

            queue.push(proccessedLink);

            if (sources.containsKey(proccessedLink)) {
                Set<String> lSources = sources.get(proccessedLink);
                lSources.add(uri.toString());
            } else {
                Set<String> lSources = Collections.synchronizedSet(new HashSet<>());

                lSources.add(uri.toString());

                sources.put(proccessedLink, lSources);
            }
        }
    }

    private void populateLinks(UniqueQueue queue, HttpURLConnection handle, URI uri, Map<String, Set<String>> sources) {
        Set<String> links = getLinks(handle);

        Iterator<String> it = links.iterator();
        while (it.hasNext()) {
            String link = it.next();

            proccessLink(queue, link, uri, sources);
        }
    }

    private void calcSpeed(int visited) {
        if (time.getEllapsedTime() / 1_000_000_000 >= 10) {
            int diff = visited - lastVisitedCount.get();
            lastVisitedCount.set(visited);
            time.reset();
            speed.set((int) Math.round(diff / 10D));
        }
    }


    private class Task implements Runnable {

        @Override
        public void run() {

            running.incrementAndGet();

            try {
                String link = links.pop();
                if (link != null) {

                    URI uri = URI.create(link);

                    HttpURLConnection handle = request(uri);

                    if (handle != null) {
                        int code = 0;
                        try {
                            code = handle.getResponseCode();
                            if (code == 404) {
                                erroredLinks.add(link);
                            } else {
                                populateLinks(links, handle, uri, sources);
                            }
                        } catch (IOException e) {

                        }
                        System.out.println(String.format("[Time: %s][Speed: %d/s][Running: %d]  [Visited: %d][Remaining: %d][Errored: %d]  [Code: %d]%s", stopWatch.getReadableTime(), speed.get(), running.get(), visited.get(), links.size(), erroredLinks.size(), code, link));
                        calcSpeed(visited.incrementAndGet());
                    }
                }
            } catch (Exception e) {

            }
            running.decrementAndGet();

        }

    }

    public void run() {
        links.clear();
        erroredLinks.clear();
        sources.clear();
        visited.set(0);
        lastVisitedCount.set(0);
        speed.set(0);
        time.reset();
        stopWatch.reset();

        links.push(url);

        ThreadPoolExecutor pool = (ThreadPoolExecutor)Executors.newFixedThreadPool(96);


        while (links.size() > 0 || running.get() > 0) {
            if (links.size() > 0 && running.get() < 96) {
                pool.submit(new Task());
            }
        }
        pool.shutdown();

        long time = stopWatch.getEllapsedTime();

        for (String link : erroredLinks) {
            System.out.println("===================================================");
            System.out.println("[404] " + link);
            System.out.println("[Sources]");
            if (sources.containsKey(link)) {
                for (String src : sources.get(link)) {
                    System.out.println("- " + src);
                }
            }
        }
    }
}
