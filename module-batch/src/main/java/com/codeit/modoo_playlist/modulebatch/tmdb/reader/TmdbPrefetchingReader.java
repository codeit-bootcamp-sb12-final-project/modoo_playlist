package com.codeit.modoo_playlist.modulebatch.tmdb.reader;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ItemReader;

import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbFetchedContent;

public class TmdbPrefetchingReader implements ItemReader<TmdbFetchedContent>, StepExecutionListener {

    private final Supplier<List<TmdbFetchedContent>> loader;
    private Iterator<TmdbFetchedContent> iterator = List.<TmdbFetchedContent>of().iterator();

    public TmdbPrefetchingReader(Supplier<List<TmdbFetchedContent>> loader) {
        this.loader = loader;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        iterator = loader.get().iterator();
    }

    @Override
    public TmdbFetchedContent read() {
        return iterator.hasNext() ? iterator.next() : null;
    }
}
