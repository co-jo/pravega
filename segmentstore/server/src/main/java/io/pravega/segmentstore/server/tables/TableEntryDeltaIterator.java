/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.segmentstore.server.tables;

import io.pravega.common.TimeoutTimer;
import io.pravega.common.util.AsyncIterator;
import io.pravega.common.util.BufferView;
import io.pravega.common.util.ByteArraySegment;
import io.pravega.segmentstore.contracts.ReadResult;
import io.pravega.segmentstore.contracts.tables.TableEntry;
import io.pravega.segmentstore.server.DirectSegmentAccess;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import io.pravega.segmentstore.server.reading.AsyncReadResultProcessor;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Iterates through {@link TableBucket}s in a Segment.
 * @param <T> Type of the final, converted result.
 */
@Slf4j
@ThreadSafe
@Builder
class TableEntryDeltaIterator<T> implements AsyncIterator<T> {
    //region Members

    // The maximum size (in bytes) of each read to perform on the segment.
    private static final int MAX_READ_SIZE = 2 * 1024 * 1024;

    private final DirectSegmentAccess segment;
    private final long startOffset;
    private final int maxLength;
    private final boolean shouldClear;
    private final Duration fetchTimeout;
    private final EntrySerializer entrySerializer;
    private final ConvertResult<T> resultConverter;
    private final Executor executor;

    @GuardedBy("this")
    private Iterator<Map.Entry<DeltaIteratorState, TableEntry>> currentEntry;
    @GuardedBy("this")
    private long currentBatchOffset;

    //endregion

    //region AsyncIterator Implementation

    @Override
    public CompletableFuture<T> getNext() {
        // Verify no other call to getNext() is currently executing.
        return getNextEntry()
                .thenCompose(entry -> {
                    if (entry == null) {
                        return CompletableFuture.completedFuture(null);
                    } else {
                        return this.resultConverter.apply(entry);
                    }
                });
    }

    public synchronized boolean endOfSegment() {
        return this.currentBatchOffset >= (this.startOffset + this.maxLength);
    }

    private CompletableFuture<Map.Entry<DeltaIteratorState, TableEntry>> getNextEntry() {
        val entry = getNextEntryFromBatch();
        if (entry != null) {
            return CompletableFuture.completedFuture(entry);
        }

        return fetchNextTableEntriesBatch().thenApply(val -> getNextEntryFromBatch());
    }

    private synchronized Map.Entry<DeltaIteratorState, TableEntry> getNextEntryFromBatch() {
        if (this.currentEntry != null) {
            val next = this.currentEntry.next();
            if (!this.currentEntry.hasNext()) {
                this.currentEntry = null;
            }
            return next;
        }

        return null;
    }

    private CompletableFuture<Void> fetchNextTableEntriesBatch() {
        return toEntries(currentBatchOffset)
                .thenAccept(entries -> {
                    if (!entries.isEmpty()) {
                        this.currentEntry = entries.iterator();
                    } else {
                        this.currentEntry = null;
                    }
                });
    }

    private CompletableFuture<List<Map.Entry<DeltaIteratorState, TableEntry>>> toEntries(long startOffset) {
        TimeoutTimer timer = new TimeoutTimer(this.fetchTimeout);
        int length = Math.min(maxLength, MAX_READ_SIZE);

        if (endOfSegment()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        ReadResult result = this.segment.read(startOffset, length, timer.getRemaining());
        return AsyncReadResultProcessor.processAll(result, this.executor, timer.getRemaining())
                .thenApply(data -> parseEntries(data, startOffset, length));
    }

    @SneakyThrows(IOException.class)
    private List<Map.Entry<DeltaIteratorState, TableEntry>> parseEntries(BufferView data, long startOffset, int readLength) {

        long currentOffset = startOffset;
        final long maxOffset = startOffset + readLength;

        InputStream input = data.getReader();
        List<Map.Entry<DeltaIteratorState, TableEntry>> entries = new ArrayList<>();
        try {
            while (currentOffset < maxOffset) {
                val entry = AsyncTableEntryReader.readEntryComponents(input, currentOffset, this.entrySerializer);
                boolean reachedEnd = currentOffset + entry.getHeader().getTotalLength() >= this.maxLength + startOffset;
                // We must preserve deletions to accurately construct a delta.
                byte[] value = entry.getValue() == null ? new byte[0] : entry.getValue();
                currentOffset += entry.getHeader().getTotalLength();
                entries.add(new AbstractMap.SimpleEntry<>(
                        new DeltaIteratorState(currentOffset, reachedEnd, this.shouldClear, entry.getHeader().isDeletion()),
                        TableEntry.versioned(new ByteArraySegment(entry.getKey()), new ByteArraySegment(value), entry.getVersion())));
            }

        } catch (EOFException ex) {
            input.close();
        }
        this.currentBatchOffset = currentOffset;

        return entries;
    }

    /**
     * Creates a new {@link TableIterator} that contains no elements.
     *
     * @param <T> Type of elements returned at each iteration.
     * @return A new instance of the {@link TableIterator.Builder} class.
     */
    static <T> TableEntryDeltaIterator<T> empty() {
        return new TableEntryDeltaIterator<>(
                null,
                0L,
                0,
                false,
                Duration.ofMillis(0),
                new EntrySerializer(),
                ignored -> CompletableFuture.completedFuture(null),
                ForkJoinPool.commonPool(),
                null,
                0L);
    }

    //endregion

    @FunctionalInterface
    interface ConvertResult<T> {
        CompletableFuture<T> apply(Map.Entry<DeltaIteratorState, TableEntry> entry);
    }

    //endregion

}