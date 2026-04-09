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
package com.reandroid.apkeditor.decompile;

import com.reandroid.apkeditor.OptionsWithFramework;
import com.reandroid.jcommand.annotations.ChoiceArg;
import com.reandroid.jcommand.annotations.CommandOptions;
import com.reandroid.jcommand.annotations.OptionArg;
import com.reandroid.utils.StringsUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommandOptions(
        name = "d",
        alternates = {"decode"},
        description = "decode_description",
        usage = "decode_usage",
        examples = {
                "decode_example_1",
                "decode_example_2",
                "decode_example_3",
                "decode_example_4",
                "decode_example_5"
        },
        notes = {
                "decode_note_1",
                "decode_note_2"
        })
public class DecompileOptions extends OptionsWithFramework {

    @ChoiceArg(name = "-t",
            values = {
                    TYPE_XML,
                    TYPE_JSON,
                    TYPE_RAW,
                    TYPE_SIG
            },
            description = "decode_types"
    )
    public String type = TYPE_XML;

    @OptionArg(name = "-split-json", flag = true, description = "split_json")
    public boolean splitJson;

    @OptionArg(name = "-vrd", flag = true, description = "validate_resources_dir")
    public boolean validateResDir;

    @OptionArg(name = "-res-dir", description = "res_dir_name")
    public String resDirName;

    @OptionArg(name = "-keep-res-path", flag = true, description = "keep_original_res")
    public boolean keepResPath;

    @OptionArg(name = "-dex", flag = true, description = "raw_dex")
    public boolean dex;

    @OptionArg(name = "-no-cache", description = "decode_no_cache", flag = true)
    public boolean noCache;

    @OptionArg(name = "-no-dex-debug", flag = true, description = "no_dex_debug")
    public boolean noDexDebug;

    @OptionArg(name = "-dex-markers", flag = true, description = "dump_dex_markers")
    public boolean dexMarkers;

    @OptionArg(name = "-load-dex", description = "decode_load_dex")
    public int loadDex = 3;

    @ChoiceArg(name = "-dex-lib",
            values = {
                    DEX_LIB_INTERNAL,
                    DEX_LIB_JF
            },
            description = "dex_lib"
    )
    public String dexLib = DEX_LIB_INTERNAL;

    @OptionArg(name = "-smali-registers", flag = true, description = "smali_registers")
    public boolean smaliRegisters;

    @ChoiceArg(name = "-comment-level",
            values = {
                    COMMENT_LEVEL_OFF,
                    COMMENT_LEVEL_BASIC,
                    COMMENT_LEVEL_DETAIL,
                    COMMENT_LEVEL_DETAIL2,
                    COMMENT_LEVEL_FULL
            },
            description = "comment_level"
    )
    public String commentLevel = COMMENT_LEVEL_BASIC;

    @OptionArg(name = "-sig", description = "signatures_path")
    public File signaturesDirectory;

    @OptionArg(name = "-dex-profile", flag = true, description = "decode_dex_profile")
    public boolean dexProfile;

    @OptionArg(name = "-remove-annotation", description = "remove_annotation")
    public final List<String> removeAnnotations = new ArrayList<>();

    public DecompileOptions() {
    }

    @Override
    public Decompiler newCommandExecutor() {
        return new Decompiler(this);
    }

    @Override
    public void validateInput(boolean isFile, boolean isDirectory) {
        super.validateInput(true, false);
    }
    @Override
    public void validateOutput(boolean isFile) {
        super.validateOutput(false);
    }
    public boolean containsCommentLevel(String level) {
        String commentLevel = this.commentLevel;
        if (StringsUtil.isEmpty(level)) {
            return COMMENT_LEVEL_OFF.equals(commentLevel);
        }
        if (COMMENT_LEVEL_OFF.equals(level)) {
            return commentLevel.equals(level);
        }
        if (COMMENT_LEVEL_BASIC.equals(level)) {
            return commentLevel.equals(level) ||
                    COMMENT_LEVEL_DETAIL.equals(commentLevel) ||
                    COMMENT_LEVEL_DETAIL2.equals(commentLevel) ||
                    COMMENT_LEVEL_FULL.equals(commentLevel);
        }
        if (COMMENT_LEVEL_DETAIL.equals(level)) {
            return commentLevel.equals(level) ||
                    COMMENT_LEVEL_DETAIL2.equals(commentLevel) ||
                    COMMENT_LEVEL_FULL.equals(commentLevel);
        }
        if (COMMENT_LEVEL_DETAIL2.equals(level)) {
            return commentLevel.equals(level) ||
                    COMMENT_LEVEL_FULL.equals(commentLevel);
        }
        if (COMMENT_LEVEL_FULL.equals(level)) {
            return commentLevel.equals(level);
        }
        return false;
    }

    @Override
    public File generateOutputFromInput(File input) {
        return generateOutputFromInput(input, "_decompile_" + type);
    }

    public static final String COMMENT_LEVEL_OFF = "off";
    public static final String COMMENT_LEVEL_BASIC = "basic";
    public static final String COMMENT_LEVEL_DETAIL = "detail";
    public static final String COMMENT_LEVEL_DETAIL2 = "detail2";
    public static final String COMMENT_LEVEL_FULL = "full";
}
