package fr.shiranuit;

public class StopWatch {

    private long start = 0;
    private long end = 0;
    private boolean running = false;

    public StopWatch() {
        start = System.nanoTime();
        end = System.nanoTime();
        running = true;
    }

    public void start() {
        if (running) return;

        running = true;
    }

    public long getEllapsedTime() {
        if (running) {
            return System.nanoTime() - start;
        } else {
            return end - start;
        }
    }

    public String getReadableTime(){

        long tempSec = getEllapsedTime()/(1_000_000_000L);
        long sec = tempSec % 60;
        long min = (tempSec /60) % 60;
        long hour = (tempSec /(60*60)) % 24;
        return String.format("%dh %dm %ds", hour,min,sec);

    }

    public void reset() {
        start = System.nanoTime();
        end = System.nanoTime();
    }

    public void stop() {
        if (!running) return;

        end = System.nanoTime();
        running = false;
    }

}
