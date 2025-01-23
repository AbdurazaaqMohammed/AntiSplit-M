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
package com.reandroid.apkeditor;

import com.reandroid.arsc.ARSCLib;
import com.reandroid.jcommand.CommandHelpBuilder;
import com.reandroid.jcommand.OptionStringBuilder;
import com.reandroid.jcommand.SubCommandHelpBuilder;
import com.reandroid.jcommand.SubCommandParser;
import com.reandroid.jcommand.annotations.OptionArg;
import com.reandroid.jcommand.exceptions.CommandException;

import java.io.File;
import java.io.IOException;

public class Options {

    @OptionArg(name = "-i", description = "input_path")
    public File inputFile;
    @OptionArg(name = "-o", description = "output_path")
    public File outputFile;
    @OptionArg(name = "-f", flag = true, description = "force_delete")
    public boolean force;
    public String type;
    @OptionArg(name = "-h", alternates = {"-help", "--help"}, description = "help_description", flag = true)
    public boolean help = false;

    private boolean mValidated;

    public Options() {
    }

    public String getHelp() {
        SubCommandHelpBuilder builder = new SubCommandHelpBuilder(ResourceStrings.INSTANCE, this.getClass());
        builder.setMaxWidth(Options.PRINT_WIDTH);
        builder.setColumnSeparator("   ");
        return builder.build();
    }
    public void parse(String[] args) {
        SubCommandParser.parse(this, args);
        validate();
    }
    public void runCommand() throws IOException {
        CommandExecutor<?> executor = newCommandExecutor();
        executor.logMessage(this.toString());
        executor.runCommand();
    }
    public CommandExecutor<?> newCommandExecutor() {
        throw new RuntimeException("Method not implemented");
    }
    public void validate() {
        if (!help && !mValidated) {
            mValidated = true;
            validateValues();
        }
    }
    public void validateValues() {
        validateInput(true, false);
        validateOutput(true);
    }
    public void validateInput(boolean isFile, boolean isDirectory) {
        File file = this.inputFile;
        if (file == null) {
            throw new CommandException("missing_input_file");
        }
        validateInputFile(file, isFile, isDirectory);
    }
    public void validateInputFile(File file, boolean isFile, boolean isDirectory) {
        if (isFile) {
            if(file.isFile()) {
                return;
            }
            if(!isDirectory) {
                throw new CommandException("no_such_file", file);
            }
        }
        if (isDirectory) {
            if(file.isDirectory()) {
                return;
            }
            throw new CommandException("no_such_directory", file);
        }
        if (!file.exists()) {
            throw new CommandException("no_such_file_or_directory", file);
        }
    }

    public void validateOutput(boolean isFile) {
        File file = this.outputFile;
        if (file == null) {
            file = generateOutputFromInput(this.inputFile);
            this.outputFile = file;
        }
        if (file == null || !file.exists()) {
            return;
        }
        if (isFile != file.isFile()) {
            if (file.isFile()) {
                throw new CommandException("path_is_file_expect_directory", file);
            }
            throw new CommandException("path_is_directory_expect_file", file);
        }
        if(!force) {
            throw new CommandException("path_already_exists", file);
        }
    }
    public File generateOutputFromInput(File input) {
        return null;
    }
    public File generateOutputFromInput(File file, String suffix) {
        String name = file.getName();
        if (file.isFile()) {
            int i = name.lastIndexOf('.');
            if(i > 0){
                name = name.substring(0, i);
            }
        }
        name = name + suffix;
        File dir = file.getParentFile();
        if (dir == null) {
            return new File(name);
        }
        return new File(dir, name);
    }

    @Override
    public String toString() {
        OptionStringBuilder builder = new OptionStringBuilder(this);
        builder.setMaxWidth(Options.PRINT_WIDTH);
        builder.setTab2("      ");
        return "Using: " + APKEditor.getName() + " version " + APKEditor.getVersion() +
                ", " + ARSCLib.getName() + " version " + ARSCLib.getVersion() +
                "\n" + builder.buildTable();
    }

    public static String getHelp(Class<?> optionClass){
        CommandHelpBuilder builder = new CommandHelpBuilder(ResourceStrings.INSTANCE, optionClass);
        builder.setMaxWidth(Options.PRINT_WIDTH);
        return builder.build();
    }

    public static final int PRINT_WIDTH = 80;


    public static final String TYPE_SIG = "sig";
    public static final String TYPE_JSON = "json";
    public static final String TYPE_RAW = "raw";
    public static final String TYPE_XML = "xml";
    public static final String TYPE_TEXT = "text";

    public static final String DEX_LIB_INTERNAL = "internal";
    public static final String DEX_LIB_JF = "jf";
}
