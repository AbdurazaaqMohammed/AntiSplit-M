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

import com.reandroid.apk.xmlencoder.EncodeException;
import com.reandroid.apkeditor.compile.BuildOptions;
import com.reandroid.apkeditor.decompile.DecompileOptions;
import com.reandroid.apkeditor.info.InfoOptions;
import com.reandroid.apkeditor.merge.MergerOptions;
import com.reandroid.apkeditor.protect.ProtectorOptions;
import com.reandroid.apkeditor.refactor.RefactorOptions;
import com.reandroid.arsc.ARSCLib;
import com.reandroid.arsc.coder.xml.XmlEncodeException;
import com.reandroid.jcommand.CommandHelpBuilder;
import com.reandroid.jcommand.CommandParser;
import com.reandroid.jcommand.annotations.MainCommand;
import com.reandroid.jcommand.annotations.OnOptionSelected;
import com.reandroid.jcommand.annotations.OtherOption;
import com.reandroid.jcommand.exceptions.CommandException;


@SuppressWarnings("unused")
@MainCommand(
        headers = {"title_app_name_and_version", "title_app_repo", "title_app_description"},
        options = {
                DecompileOptions.class,
                BuildOptions.class,
                MergerOptions.class,
                RefactorOptions.class,
                ProtectorOptions.class,
                InfoOptions.class
        }
)
public class Main {

    private boolean mEmptyOption;
    private Options mOptions;
    private int mExitCode;

    private Main() {

    }
    public static void main(String[] args) {
        int result = execute(args);
        System.exit(result);
    }

    /**
     * If you are running inside java application, use this method to
     * avoid unwanted System.exit()
     *
     * Returns 0 - executed successfully
     * Returns 1 - error
     * Returns 2 - non executing commands like help, version
     *
     * */
    public static int execute(String[] args) {
        Main main = new Main();
        return main.run(args);
    }

    @OtherOption(
            names = {"-h", "-help"}, alternates = {"--help"},
            description = "app_help"
    )
    void onMainHelp() {
        mExitCode = 2;
        CommandHelpBuilder builder = new CommandHelpBuilder(
                ResourceStrings.INSTANCE, Main.class);
        builder.setFooters("", "help_main_footer", "<command> -h", "");
        System.err.println(builder.build());
    }
    @OtherOption(
            names = {"-v", "-version"}, alternates = {"--version"},
            description = "app_version"
    )
    void onPrintVersion() {
        mExitCode = 2;
        System.err.println(APKEditor.getName() +
                " version " + APKEditor.getVersion() +
                ", " + ARSCLib.getName() +
                " version " + ARSCLib.getVersion());
    }
    @OnOptionSelected
    void onOption(Object option, boolean emptyArgs) {
        this.mOptions = (Options) option;
        this.mEmptyOption = emptyArgs;
    }

    private int run(String[] args) {
        mOptions = null;
        mEmptyOption = false;
        mExitCode = 2;
        CommandParser parser = new CommandParser(Main.class);
        try {
            parser.parse(this, args);
        } catch (CommandException e) {
            System.err.flush();
            System.err.println(e.getMessage(ResourceStrings.INSTANCE));
            return mExitCode;
        }
        if(mOptions == null) {
            return mExitCode;
        }
        if(mEmptyOption) {
            System.err.println(ResourceStrings.INSTANCE.getString(
                    "empty_command_option_exception"));
            return mExitCode;
        }
        try {
            mOptions.validate();
        } catch (CommandException e) {
            System.err.flush();
            System.err.println(e.getMessage(ResourceStrings.INSTANCE));
            return mExitCode;
        }
        if(mOptions.help) {
            System.err.println(mOptions.getHelp());
            return mExitCode;
        }
        mExitCode = 1;
        try {
            mOptions.runCommand();
            mExitCode = 0;
        }  catch (CommandException ex1) {
            System.err.flush();
            System.err.println(ex1.getMessage(ResourceStrings.INSTANCE));
        } catch (EncodeException | XmlEncodeException ex) {
            System.err.flush();
            System.err.println("\nERROR:\n" + ex.getMessage());
        } catch (Exception exception) {
            System.err.flush();
            System.err.println("\nERROR:");
            exception.printStackTrace(System.err);
        }
        return mExitCode;
    }
}
