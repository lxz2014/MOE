/*
 * Copyright (c) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client.translation.editors;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.project.EditorConfig;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/** A PatchingEditor invokes the command patch */
@AutoFactory(implementing = Editor.Factory.class)
public class PatchingEditor implements Editor {

  private final CommandRunner cmd;
  private final FileSystem filesystem;
  private final String name;

  PatchingEditor(
      @Provided CommandRunner cmd,
      @Provided FileSystem filesystem,
      String editorName,
      @SuppressWarnings("unused") EditorConfig ignored) {
    this.cmd = cmd;
    this.filesystem = filesystem;
    name = editorName;
  }

  /**
   * Returns a description of what this editor will do.
   */
  @Override
  public String getDescription() {
    return name;
  }

  /**
   * Applies a patch to copied contents of the input Codebase, returning a new Codebase with the
   * results of the patch.
   */
  @Override
  public Codebase edit(Codebase input, Map<String, String> options) {
    File tempDir = filesystem.getTemporaryDirectory("patcher_run_");
    String patchFilePath = options.get("file");
    if (isNullOrEmpty(patchFilePath)) {
      return input;
    } else {
      File patchFile = new File(patchFilePath);
      if (!filesystem.isReadable(patchFile)) {
        throw new MoeProblem("cannot read file %s", patchFilePath);
      }
      try {
        filesystem.copyDirectory(input.path(), tempDir);
      } catch (IOException e) {
        throw new MoeProblem(e.getMessage());
      }
      try {
        cmd.runCommand(
            "patch",
            ImmutableList.of("-p0", "--input=" + patchFilePath),
            tempDir.getAbsolutePath());
      } catch (CommandRunner.CommandException e) {
        throw new MoeProblem(e.getMessage());
      }
      return Codebase.create(tempDir, input.projectSpace(), input.expression());
    }
  }
}
