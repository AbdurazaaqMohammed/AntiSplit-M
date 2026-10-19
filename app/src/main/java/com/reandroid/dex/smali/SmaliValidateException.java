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
package com.reandroid.dex.smali;

import com.reandroid.common.Origin;
import com.reandroid.dex.smali.model.Smali;
import com.reandroid.dex.smali.model.SmaliDef;
import com.reandroid.utils.exception.ExceptionMessage;

import java.io.IOException;

public class SmaliValidateException extends IOException implements ExceptionMessage {

    private final Smali smali;

    public SmaliValidateException(String message, Smali smali) {
        super(message);
        this.smali = smali;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        Smali smali = this.smali;
        if (smali == null) {
            return message;
        }
        Origin origin = smali.getOrigin();
        if (origin != null) {
            return message + "\nat " + origin;
        }
        Smali debug;
        if (smali instanceof SmaliDef) {
            debug = smali;
        } else {
            debug = smali.getParentInstance(SmaliDef.class);
        }
        if (debug == null) {
            return message;
        }
        return message + "\n at " + debug.toDebugString();
    }
}
