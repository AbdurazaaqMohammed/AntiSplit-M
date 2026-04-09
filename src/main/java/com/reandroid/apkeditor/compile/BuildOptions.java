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
package com.reandroid.apkeditor.compile;

import com.reandroid.apkeditor.OptionsWithFramework;
import com.reandroid.app.AndroidManifest;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.jcommand.annotations.ChoiceArg;
import com.reandroid.jcommand.annotations.CommandOptions;
import com.reandroid.jcommand.annotations.OptionArg;
import com.reandroid.jcommand.exceptions.CommandException;

import java.io.File;


@CommandOptions(
        name = "b",
        alternates = {"build"},
        description = "build_description",
        examples = {
                "build_example_1",
                "build_example_2",
                "build_example_3",
                "build_example_4"
        })
public class BuildOptions extends OptionsWithFramework {

    @ChoiceArg(
            name = "-t",
            values = {
                    TYPE_XML,
                    TYPE_JSON,
                    TYPE_RAW, TYPE_SIG
            },
            description = "build_types")
    public String type;

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

    @OptionArg(name = "-vrd", description = "validate_resources_dir", flag = true)
    public boolean validateResDir;

    @OptionArg(name = "-res-dir", description = "res_dir_name")
    public String resDirName;

    @OptionArg(name = "-no-cache", description = "build_no_cache", flag = true)
    public boolean noCache;

    @ChoiceArg(name = "-dex-lib",
            values = {
                    DEX_LIB_INTERNAL,
                    DEX_LIB_JF
            },
            description = "dex_lib"
    )
    public String dexLib = DEX_LIB_INTERNAL;

    @OptionArg(name = "-sig", description = "signatures_path")
    public File signaturesDirectory;

    @OptionArg(name = "-dex-profile", flag = true, description = "encode_dex_profile")
    public boolean dexProfile;

    public BuildOptions() {
        super();
    }

    @Override
    public Builder newCommandExecutor() {
        return new Builder(this);
    }

    @Override
    public void validateInput(boolean isFile, boolean isDirectory) {
        isFile = TYPE_SIG.equals(type);
        super.validateInput(isFile, !isFile);
        evaluateInputDirectoryType();
        validateSignaturesDirectory();
    }

    private void evaluateInputDirectoryType() {
        String type = this.type;
        if (type != null) {
            return;
        }
        File file = inputFile;
        if(isRawInputDirectory(file)) {
            type = TYPE_RAW;
        } else if(isJsonInputDirectory(file)) {
            type = TYPE_JSON;
            this.inputFile = getJsonInDir(this.inputFile);
        } else if (isXmlInputDirectory(file)) {
            type = TYPE_XML;
        } else if(signaturesDirectory != null){
            type = TYPE_SIG;
        } else {
            throw new CommandException("unknown_build_directory", file);
        }
        this.type = type;
    }

    @Override
    public void validateOutput(boolean isFile) {
        super.validateOutput(true);
    }

    private void validateSignaturesDirectory() {
        if (TYPE_SIG.equals(type)) {
            File file = this.signaturesDirectory;
            if(file == null) {
                throw new CommandException("missing_sig_directory");
            }
            validateInputFile(file, false, true);
        } else if(this.signaturesDirectory != null) {
            throw new CommandException("invalid_sig_parameter_combination");
        }
    }
    @Override
    public File generateOutputFromInput(File file) {
        return generateOutputFromInput(file, "_out.apk");
    }

    public String getExtractNativeLibs() {
        String extractNativeLibs = this.extractNativeLibs;
        if (extractNativeLibs == null) {
            extractNativeLibs = "manifest";
        }
        return extractNativeLibs;
    }
    private static boolean isRawInputDirectory(File dir){
        File file=new File(dir, AndroidManifest.FILE_NAME_BIN);
        if(!file.isFile()) {
            file = new File(dir, TableBlock.FILE_NAME);
        }
        return file.isFile();
    }
    private static boolean isXmlInputDirectory(File dir) {
        File manifest = new File(dir, AndroidManifest.FILE_NAME);
        return manifest.isFile();
    }
    private static boolean isJsonInputDirectory(File dir) {
        if (isModuleDir(dir)) {
            return true;
        }
        File[] files = dir.listFiles();
        if(files == null) {
            return false;
        }
        for(File file:files) {
            if(isModuleDir(file)){
                return true;
            }
        }
        return false;
    }
    private static boolean isModuleDir(File dir){
        if(!dir.isDirectory()){
            return false;
        }
        File file = new File(dir, AndroidManifest.FILE_NAME_JSON);
        return file.isFile();
    }
    private static File getJsonInDir(File dir) {
        if(isModuleDir(dir)){
            return dir;
        }
        File[] files = dir.listFiles();
        if(files == null || files.length == 0){
            throw new CommandException("Empty directory: %s", dir);
        }
        for(File file:files){
            if(isModuleDir(file)){
                return file;
            }
        }
        throw new CommandException("Invalid directory: '%s', missing file \"uncompressed-files.json\"", dir);
    }
}
