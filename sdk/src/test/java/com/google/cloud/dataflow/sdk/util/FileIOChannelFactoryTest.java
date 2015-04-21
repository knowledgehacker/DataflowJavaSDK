/*
 * Copyright (C) 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.dataflow.sdk.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.common.io.LineReader;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/** Tests for {@link FileIOChannelFactory}. */
@RunWith(JUnit4.class)
public class FileIOChannelFactoryTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private FileIOChannelFactory factory = new FileIOChannelFactory();

  private void testCreate(Path path) throws Exception {
    String expected = "my test string";
    try (Writer writer = Channels.newWriter(
        factory.create(path.toString(), MimeTypes.TEXT), StandardCharsets.UTF_8.name())) {
      writer.write(expected);
    }
    assertThat(Files.readLines(path.toFile(), StandardCharsets.UTF_8), Matchers.hasItems(expected));
  }

  @Test
  public void testCreateWithExistingFile() throws Exception {
    File existingFile = temporaryFolder.newFile();
    testCreate(existingFile.toPath());
  }

  @Test
  public void testCreateWithinExistingDirectory() throws Exception {
    testCreate(temporaryFolder.getRoot().toPath().resolve("file.txt"));
  }

  @Test
  public void testCreateWithNonExistentSubDirectory() throws Exception {
    testCreate(temporaryFolder.getRoot().toPath().resolve("non-existent-dir").resolve("file.txt"));
  }

  @Test
  public void testReadWithExistingFile() throws Exception {
    String expected = "my test string";
    File existingFile = temporaryFolder.newFile();
    Files.write(expected, existingFile, StandardCharsets.UTF_8);
    String data;
    try (Reader reader =
        Channels.newReader(factory.open(existingFile.getPath()), StandardCharsets.UTF_8.name())) {
      data = new LineReader(reader).readLine();
    }
    assertEquals(expected, data);
  }

  @Test(expected = FileNotFoundException.class)
  public void testReadNonExistentFile() throws Exception {
    factory.open(temporaryFolder.getRoot().toPath().resolve("non-existent-file.txt").toString());
  }

  @Test
  public void testIsReadSeekEfficient() throws Exception {
    assertTrue(factory.isReadSeekEfficient("somePath"));
  }

  @Test
  public void testMatchExact() throws Exception {
    List<String> expected = ImmutableList.of(temporaryFolder.newFile("a").toString());
    temporaryFolder.newFile("aa");
    temporaryFolder.newFile("ab");

    assertThat(factory.match(temporaryFolder.getRoot().toPath().resolve("a").toString()),
        Matchers.hasItems(expected.toArray(new String[expected.size()])));
  }

  @Test
  public void testMatchNone() throws Exception {
    List<String> expected = ImmutableList.of();
    temporaryFolder.newFile("a");
    temporaryFolder.newFile("aa");
    temporaryFolder.newFile("ab");

    assertThat(factory.match(temporaryFolder.getRoot().toPath().resolve("b*").toString()),
        Matchers.hasItems(expected.toArray(new String[expected.size()])));
  }

  @Test
  public void testMatchMultiple() throws Exception {
    List<String> expected = ImmutableList.of(temporaryFolder.newFile("a").toString(),
        temporaryFolder.newFile("aa").toString(), temporaryFolder.newFile("ab").toString());
    temporaryFolder.newFile("ba");
    temporaryFolder.newFile("bb");
    assertThat(factory.match(temporaryFolder.getRoot().toPath().resolve("a*").toString()),
        Matchers.hasItems(expected.toArray(new String[expected.size()])));
  }
}