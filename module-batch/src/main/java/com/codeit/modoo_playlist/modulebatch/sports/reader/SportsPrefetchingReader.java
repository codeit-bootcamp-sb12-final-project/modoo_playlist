package com.codeit.modoo_playlist.modulebatch.sports.reader;

import java.util.Iterator;
import java.util.List;

import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ItemReader;

import com.codeit.modoo_playlist.infra.client.sportsdb.dto.SportsDbEvent;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SportsPrefetchingReader implements ItemReader<SportsDbEvent>, StepExecutionListener {

    private final SportsEventLoader loader;
    private Iterator<SportsDbEvent> iterator = List.<SportsDbEvent>of().iterator();

    @Override
    public void beforeStep(StepExecution stepExecution) {
        iterator = loader.load().iterator();
    }

    @Override
    public SportsDbEvent read() {
        return iterator.hasNext() ? iterator.next() : null;
    }
}
