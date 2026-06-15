package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

/**
 * A {@link RecursiveAction} that crawls a single URL: downloads and parses it, records its word
 * counts, marks it as visited, and forks off one subtask per link found on the page (up to
 * {@code maxDepth}).
 */
final class CrawlInternalTask extends RecursiveAction {

    private final String url;
    private final Instant deadline;
    private final int maxDepth;
    private final Clock clock;
    private final PageParserFactory parserFactory;
    private final List<Pattern> ignoredUrls;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;

    private CrawlInternalTask(Builder builder) {
        this.url = builder.url;
        this.deadline = builder.deadline;
        this.maxDepth = builder.maxDepth;
        this.clock = builder.clock;
        this.parserFactory = builder.parserFactory;
        this.ignoredUrls = builder.ignoredUrls;
        this.counts = builder.counts;
        this.visitedUrls = builder.visitedUrls;
    }

    @Override
    protected void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }

        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }

        // Atomic check-and-add: returns false if url was already present, so exactly one thread
        // proceeds to crawl any given URL.
        if (!visitedUrls.add(url)) {
            return;
        }

        PageParser.Result result = parserFactory.get(url).parse();

        // Atomic per-key read-modify-write, safe for concurrent updates to the same word.
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            counts.merge(e.getKey(), e.getValue(), Integer::sum);
        }

        List<CrawlInternalTask> subtasks = new ArrayList<>();
        for (String link : result.getLinks()) {
            subtasks.add(new Builder()
                    .setUrl(link)
                    .setDeadline(deadline)
                    .setMaxDepth(maxDepth - 1)
                    .setClock(clock)
                    .setParserFactory(parserFactory)
                    .setIgnoredUrls(ignoredUrls)
                    .setCounts(counts)
                    .setVisitedUrls(visitedUrls)
                    .build());
        }
        invokeAll(subtasks);
    }

    /**
     * A builder for {@link CrawlInternalTask}, used to avoid a constructor with a long parameter
     * list.
     */
    static final class Builder {
        private String url;
        private Instant deadline;
        private int maxDepth;
        private Clock clock;
        private PageParserFactory parserFactory;
        private List<Pattern> ignoredUrls;
        private Map<String, Integer> counts;
        private Set<String> visitedUrls;

        Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        Builder setDeadline(Instant deadline) {
            this.deadline = deadline;
            return this;
        }

        Builder setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        Builder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        Builder setParserFactory(PageParserFactory parserFactory) {
            this.parserFactory = parserFactory;
            return this;
        }

        Builder setIgnoredUrls(List<Pattern> ignoredUrls) {
            this.ignoredUrls = ignoredUrls;
            return this;
        }

        Builder setCounts(Map<String, Integer> counts) {
            this.counts = counts;
            return this;
        }

        Builder setVisitedUrls(Set<String> visitedUrls) {
            this.visitedUrls = visitedUrls;
            return this;
        }

        CrawlInternalTask build() {
            return new CrawlInternalTask(this);
        }
    }
}