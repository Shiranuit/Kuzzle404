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
import java.util.stream.Collectors;

public class Crawler {

    private static Pattern l1 = Pattern.compile("<a.*?href=\"([^\"]+)\".*?>", Pattern.MULTILINE);
    private static Pattern l2 = Pattern.compile("<link.*?href=\"([^\"]+)\".*?>", Pattern.MULTILINE);
    private static Pattern anchor = Pattern.compile("(.+?/?)#[^/]+$");
    private static Pattern http = Pattern.compile("^https?:", Pattern.MULTILINE);

    private String url;

    private UniqueQueue<String> links = new UniqueQueue<>();
    private Queue<String> erroredLinks = new LinkedBlockingQueue<>();
    private Map<String, Set<String>> sources = Collections.synchronizedMap(new HashMap<>());
    private Set<String> authorized = new HashSet<>();
    private UniqueQueue<String> externalLinks = new UniqueQueue<>();

    private AtomicInteger visited = new AtomicInteger(0);
    private AtomicInteger speed = new AtomicInteger(0);
    private AtomicInteger lastVisitedCount = new AtomicInteger(0);
    private AtomicInteger running = new AtomicInteger(0);

    private StopWatch time = new StopWatch();
    private StopWatch stopWatch = new StopWatch();

    public Crawler(String url) {
        this(url, new String[] {});
    }

    public Crawler(String url, String[] authorizedLinks) {
        this.url = url;
        for (int i = 0; i < authorizedLinks.length; i++) {
            authorized.add(authorizedLinks[i]);
        }
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

            return connection;
        } catch (IOException e) {

        }
        return null;
    }

    private void pushSource(String link, String source) {
        if (sources.containsKey(link)) {
            Set<String> lSources = sources.get(link);
            lSources.add(source);
        } else {
            Set<String> lSources = Collections.synchronizedSet(new HashSet<>());

            lSources.add(source);

            sources.put(link, lSources);
        }
    }

    private void proccessLink(String link, URI uri) {
        if (link.startsWith("/")) {
            Matcher mAnchor = anchor.matcher(link);
            String proccessedLink = "";

            if (mAnchor.find()) {
                proccessedLink = uri.getScheme() + "://" + uri.getHost() + mAnchor.group(1);
            } else {
                proccessedLink = uri.getScheme() + "://" + uri.getHost() + link;
            }

            links.push(proccessedLink);

            pushSource(proccessedLink, uri.toString());
        } else if (http.matcher(link).find()) {
            try {
                URI url = URI.create(link);
                if (authorized.contains(url.getHost())) {
                    links.push(link);
                    pushSource(link, uri.toString());
                } else {
                    externalLinks.push(link);
                    pushSource(link, uri.toString());
                }
            } catch (Exception e) {

            }
        }
    }

    private void populateLinks(HttpURLConnection handle, URI uri) {
        Set<String> links = getLinks(handle);

        Iterator<String> it = links.iterator();
        while (it.hasNext()) {
            String link = it.next();

            proccessLink(link, uri);
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


    private class CrawlLink implements Runnable {

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
                                populateLinks(handle, uri);
                            }
                        } catch (IOException e) {

                        }
                        System.out.println(String.format("[Time: %s][Speed: %d/s][Running: %d]  [Visited: %d][Remaining: %d][Externals: %d][Errored: %d]  [Code: %d]%s", stopWatch.getReadableTime(), speed.get(), running.get(), visited.get(), links.size(), externalLinks.size(), erroredLinks.size(), code, link));
                        calcSpeed(visited.incrementAndGet());
                    }
                }
            } catch (Exception e) {

            }
            running.decrementAndGet();

        }

    }

    private class CheckLink implements Runnable {



        @Override
        public void run() {
            running.incrementAndGet();
            try {
                String link = externalLinks.pop();

                URI uri = URI.create(link);

                HttpURLConnection handle = request(uri);

                if (handle != null) {
                    int code = 0;
                    try {
                        code = handle.getResponseCode();
                        if (code == 404) {
                            erroredLinks.add(link);
                        }
                        System.out.println(String.format("[Time: %s][Speed: %d/s][Running: %d]  [Visited: %d][Remaining: %d][Externals: %d][Errored: %d]  [Code: %d]%s", stopWatch.getReadableTime(), speed.get(), running.get(), visited.get(), links.size(), externalLinks.size(), erroredLinks.size(), code, link));
                        calcSpeed(visited.incrementAndGet());
                    } catch (IOException e) {

                    }
                }
            } catch (Exception e) {

            }
            running.decrementAndGet();
        }
    }

    private List<String> sortSet(Set<String> set) {
        List<String> sortedList = set.stream().collect(Collectors.toList());
        Collections.sort(sortedList);
        return sortedList;
    }

    private List<String> sortQueue(Queue<String> queue) {
        List<String> sortedList = new ArrayList<>();

        while (queue.size() > 0) {
            sortedList.add(queue.poll());
        }
        Collections.sort(sortedList);
        return sortedList;
    }

    public void run() {
        run(96);
    }

    public void run(int threads) {
        links.clear();
        erroredLinks.clear();
        sources.clear();
        visited.set(0);
        lastVisitedCount.set(0);
        speed.set(0);
        time.reset();
        stopWatch.reset();

        links.push(url);



        ThreadPoolExecutor pool = (ThreadPoolExecutor)Executors.newFixedThreadPool(threads);

        while (links.size() > 0 || running.get() > 0) {
            if (links.size() > 0 && running.get() < threads) {
                pool.submit(new CrawlLink());
            }
        }

        long time = stopWatch.getEllapsedTime();

        while (externalLinks.size() > 0 || running.get() > 0) {
            if (externalLinks.size() > 0 && running.get() < threads) {
                pool.submit(new CheckLink());
            }
        }

        pool.shutdown();

        for (String link : sortQueue(erroredLinks)) {
            System.out.println("===================================================");
            System.out.println("[404] " + link);
            System.out.println("[Sources]");
            if (sources.containsKey(link)) {
                for (String src : sortSet(sources.get(link))) {
                    System.out.println("- " + src);
                }
            }
        }
    }
}
