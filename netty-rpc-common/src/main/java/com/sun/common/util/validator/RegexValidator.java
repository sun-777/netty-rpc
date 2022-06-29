/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sun.common.util.validator;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexValidator implements Serializable{

    private static final long serialVersionUID = 8193776869934163015L;
    private final Pattern[] patterns;

    public RegexValidator(String regex) {
        this(regex, true);
    }

    public RegexValidator(String regex, boolean caseSensitive) {
        this(new String[] {regex}, caseSensitive);
    }

    public RegexValidator(String[] regexs) {
        this(regexs, true);
    }

    public RegexValidator(String[] regexs, boolean caseSensitive) {
        if (regexs == null || regexs.length == 0) {
            throw new IllegalArgumentException("Regular expressions are missing");
        }
        patterns = new Pattern[regexs.length];
        int flags =  (caseSensitive ? 0: Pattern.CASE_INSENSITIVE);
        for (int i = 0; i < regexs.length; i++) {
            if (regexs[i] == null || regexs[i].length() == 0) {
                throw new IllegalArgumentException("Regular expression[" + i + "] is missing");
            }
            patterns[i] =  Pattern.compile(regexs[i], flags);
        }
    }


    /*
     * find()：是部分匹配，是查找输入串中与模式匹配的子串。
     * matches()：是全部匹配，是将整个输入串与模式匹配。
     * matches()更新了最后匹配位置，如果需要，可以使用find(0)重置匹配器。
     */
    public boolean isFind(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i].matcher(value).find()) {
                return true;
            }
        }
        return false;
    }

    public boolean isMatch(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i].matcher(value).matches()) {
                return true;
            }
        }
        return false;
    }


    public String[] match(String value) {
        if (value == null) {
            return null;
        }
        for (int i = 0; i < patterns.length; i++) {
            Matcher matcher = patterns[i].matcher(value);
            if (matcher.matches()) {
                int count = matcher.groupCount();
                String[] groups = new String[count];
                for (int j = 0; j < count; j++) {
                    groups[j] = matcher.group(j+1);
                }
                return groups;
            }
        }
        return null;
    }


    public String validate(String value) {
        if (value == null) {
            return null;
        }
        for (int i = 0; i < patterns.length; i++) {
            Matcher matcher = patterns[i].matcher(value);
            if (matcher.matches()) {
                int count = matcher.groupCount();
                if (count == 1) {
                    return matcher.group(1);
                }
                StringBuilder buffer = new StringBuilder();
                for (int j = 0; j < count; j++) {
                    String component = matcher.group(j+1);
                    if (component != null) {
                        buffer.append(component);
                    }
                }
                return buffer.toString();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("RegexValidator{");
        for (int i = 0; i < patterns.length; i++) {
            if (i > 0) {
                buffer.append(",");
            }
            buffer.append(patterns[i].pattern());
        }
        buffer.append("}");
        return buffer.toString();
    }
}