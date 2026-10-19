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
import com.reandroid.utils.exception.ExceptionMessage;

import java.io.IOException;

public class SmaliParseException extends IOException implements ExceptionMessage {

    private final SmaliReader reader;

    public SmaliParseException(String message, SmaliReader reader) {
        super(message);
        this.reader = reader;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        SmaliReader reader = this.reader;
        if (reader == null) {
            return message;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(message);
        Origin origin = reader.getCurrentOrigin();
        if (origin != null) {
            builder.append("\nat ");
            builder.append(origin);
        };
        return builder.toString();
    }

    public static void expect(SmaliReader reader, SmaliDirective expect) throws IOException {
        expect(reader, expect, false);
    }
    public static void expect(SmaliReader reader, SmaliDirective expect, boolean end) throws IOException {
        int position = reader.position();
        boolean error = false;
        if (end) {
            if (!expect.isEnd(reader)) {
                error = true;
            }
        }
        if (!error) {
            if (SmaliDirective.parse(reader) != expect) {
                error = true;
            }
        }
        if (!error) {
            return;
        }
        reader.position(position);
        String message = "expecting '" + expect.toString(end) + "'";
        SmaliParseException exception = new SmaliParseException(message, reader);
        sanitizeStackTrace(exception);
        throw exception;
    }
    public static void expect(SmaliReader reader, char ch) throws IOException {
        if (reader.skipIfChar(ch)) {
            return;
        }
        String message = "expecting '" + ch + "'";
        SmaliParseException exception = new SmaliParseException(message, reader);
        sanitizeStackTrace(exception);
        throw exception;
    }
    public static char expect(SmaliReader reader, char ch1, char ch2) throws IOException {
        if (!reader.finished()) {
            char ch = reader.readASCII();
            if (ch == ch1 || ch == ch2) {
                return ch;
            }
            reader.skip(-1);
        }
        String message = "expecting '" + ch1 + "', or '" + ch2 + "'";
        SmaliParseException exception = new SmaliParseException(message, reader);
        sanitizeStackTrace(exception);
        throw exception;
    }
    private static void sanitizeStackTrace(Exception exception) {
        StackTraceElement[] elements = exception.getStackTrace();
        int count = 0;
        for(int i = 0; i < elements.length; i++) {
            StackTraceElement element = elements[i];
            if (remove(element)) {
                elements[i] = null;
            } else {
                count ++;
            }
        }
        StackTraceElement[] result = new StackTraceElement[count];
        count = 0;
        for(StackTraceElement element : elements) {
            if (element != null) {
                result[count] = element;
                count ++;
            }
        }
        exception.setStackTrace(result);
    }
    private static boolean remove(StackTraceElement element) {
        if (element == null) {
            return true;
        }
        String name = element.getClassName();
        return name.equals(SmaliParseException.class.getName());
    }
}
