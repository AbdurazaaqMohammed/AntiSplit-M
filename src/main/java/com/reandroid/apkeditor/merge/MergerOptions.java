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
package com.reandroid.apkeditor.merge;

import com.reandroid.apkeditor.Options;
import com.reandroid.jcommand.annotations.ChoiceArg;
import com.reandroid.jcommand.annotations.CommandOptions;
import com.reandroid.jcommand.annotations.OptionArg;

import java.io.File;


@CommandOptions(
        name = "m",
        alternates = {"merge"},
        description = "merge_description",
        examples = {
                "merge_example_1"
        })
public class MergerOptions extends Options {


    @ChoiceArg(
            name = "-extractNativeLibs",
            values = {
                    "manifest",
                    "none",
                    "false",
                    "true"
            },
            description = "extract_native_libs")
    public String extractNativeLibs;

    @OptionArg(name = "-vrd", flag = true, description = "validate_resources_dir")
    public boolean validateResDir;

    @OptionArg(name = "-clean-meta", flag = true, description = "clean_meta")
    public boolean cleanMeta;

    @OptionArg(name = "-validate-modules", flag = true, description = "validate_modules")
    public boolean validateModules;

    @OptionArg(name = "-res-dir", description = "res_dir_name")
    public String resDirName;

    public MergerOptions(){
        super();
    }

    @Override
    public Merger newCommandExecutor() {
        return new Merger(this);
    }

    @Override
    public void validateInput(boolean isFile, boolean isDirectory) {
        super.validateInput(true, true);
    }

    @Override
    public File generateOutputFromInput(File input) {
        return generateOutputFromInput(input, "_merged.apk");
    }

    public String getExtractNativeLibs() {
        String extractNativeLibs = this.extractNativeLibs;
        if (extractNativeLibs == null) {
            extractNativeLibs = "manifest";
        }
        return extractNativeLibs;
    }
}
