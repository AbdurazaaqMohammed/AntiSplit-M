/*
  *  Copyright (C) 2022 github.com/REAndroid
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package com.reandroid.apkeditor.protect;

import com.reandroid.apkeditor.Options;
import com.reandroid.jcommand.annotations.CommandOptions;
import com.reandroid.jcommand.annotations.OptionArg;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.io.FileUtil;
import com.reandroid.utils.io.IOUtil;

import org.apache.commons.collections4.Predicate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@CommandOptions(
        name = "p",
        alternates = {"protect"},
        description = "protect_description",
        examples = {
                "protect_example_1"
        })
public class ProtectorOptions extends Options {

    @OptionArg(name = "-skip-manifest", flag = true, description = "protect_skip_manifest")
    public boolean skipManifest;

    @OptionArg(name = "-confuse-zip", flag = true, description = "protect_confuse_zip")
    public boolean confuse_zip;

    @OptionArg(name = "-keep-type", description = "protect_keep_type")
    public final Set<String> keepTypes = new HashSet<>();

    @OptionArg(name = "-dic-dir-names", flag = true, description = "protect_dic_dir_name")
    public File dic_dir_name;

    @OptionArg(name = "-dic-file-names", flag = true, description = "protect_dic_file_name")
    public File dic_file_name;

    public ProtectorOptions() {
        super();
    }

    @Override
    public void validateValues() {
        super.validateValues();
        addDefaultKeepTypes();
    }
    private void addDefaultKeepTypes() {
        Set<String> keepTypes = this.keepTypes;
        if (!keepTypes.isEmpty()) {
            return;
        }
        keepTypes.add("font");
    }

    @Override
    public Protector newCommandExecutor() {
        return new Protector(this);
    }

    @Override
    public void validateInput(boolean isFile, boolean isDirectory) {
        super.validateInput(true, false);
    }

    @Override
    public void validateOutput(boolean isFile) {
        super.validateOutput(true);
    }

    @Override
    public File generateOutputFromInput(File input) {
        return generateOutputFromInput(input, "_protected.apk");
    }

    public String[] loadDirectoryNameDictionary() {
        return loadDictionary(dic_dir_name, "/protect_dic_dir_name.txt");
    }
    public String[] loadFileNameDictionary() {
        return loadDictionary(dic_file_name, "/protect_dic_file_name.txt");
    }

    private String[] loadDictionary(File file, String resource) {
        InputStream inputStream;
        if (file != null) {
            try {
                inputStream = FileUtil.inputStream(file);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        } else {
            inputStream = ProtectorOptions.class.getResourceAsStream(resource);
        }
        String full;
        try {
            full = IOUtil.readUtf8(inputStream);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        ArrayCollection<String> results = new ArrayCollection<>(
                StringsUtil.split(full, '\n', true));
        results.removeIf(new Predicate<String>() {
            @Override
            public boolean evaluate(String text) {
                return StringsUtil.isEmpty(text);
            }
        });
        return results.toArray(new String[results.size()]);
    }
    public boolean isKeepType(String type) {
        Set<String> keepTypes = this.keepTypes;
        return keepTypes.contains(type) ||
                keepTypes.contains(KEEP_ALL_TYPES);
    }
    public boolean isKeepAllTypes() {
        return keepTypes.contains(KEEP_ALL_TYPES);
    }

    private static final String KEEP_ALL_TYPES = "all-types";
}
