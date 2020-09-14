/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.common.io.filesystem;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class FileOperationsTests {

    @Test
    // Force an IOException by trying to delete a locked file.
    public void deleteLockedFile() throws IOException {
        Path dir = null;
        File temp = null;
        try {
            dir = Files.createTempDirectory(null);
            temp = File.createTempFile("file", null, dir.toFile());
        } catch (IOException e) {
            System.out.println(e);
            Assert.fail();
        }

        try {
            RandomAccessFile file = new RandomAccessFile(temp, "rw");
            FileChannel channel = file.getChannel();
            channel.lock();
        } catch (FileNotFoundException e) {
            System.out.println(e);
            Assert.fail();
        } finally {
            HashMap<Integer, File> map = new HashMap<>();
            map.put(0, dir.toFile());
            FileOperations.cleanupDirectories(map);
        }
    }

}
