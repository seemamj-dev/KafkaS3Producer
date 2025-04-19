package org.example;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class IngestionMetricsService {

    private final MeterRegistry meterRegistry;

    public IngestionMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementIngestedArticles() {
        meterRegistry.counter("news.articles.ingested.count").increment();
    }

    public void recordIngestionDuration(long millis) {
        meterRegistry.timer("news.articles.ingestion.time").record(millis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}

