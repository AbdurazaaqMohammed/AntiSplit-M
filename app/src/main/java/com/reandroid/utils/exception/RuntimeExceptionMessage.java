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
package com.reandroid.utils.exception;

public class RuntimeExceptionMessage extends RuntimeException implements ExceptionMessage {

    public RuntimeExceptionMessage(Throwable cause) {
        super(cause);
    }

    public ExceptionMessage getExceptionMessage() {
        Throwable cause = getCause();
        while (cause != null && cause != this) {
            if (cause instanceof ExceptionMessage) {
                return (ExceptionMessage) cause;
            }
            cause = cause.getCause();
        }
        return null;
    }

    @Override
    public String getMessage() {
        ExceptionMessage exceptionMessage = getExceptionMessage();
        if (exceptionMessage != null) {
            return exceptionMessage.getMessage();
        }
        String message = super.getMessage();
        if (message == null) {
            Throwable cause = getCause();
            if (cause != null && cause != this) {
                message = cause.getMessage();
            }
        }
        return message;
    }
}
