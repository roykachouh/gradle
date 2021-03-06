/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.plugins;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import org.gradle.util.TextUtil;

import java.io.File;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Map;

public class WindowsStartScriptGenerator extends AbstractTemplateBasedStartScriptGenerator {
    String getLineSeparator() {
        return TextUtil.getWindowsLineSeparator();
    }

    File getTemplate() {
        return new File("windowsStartScript.txt");
    }

    Map<String, String> createBinding(StartScriptGenerationDetails details) {
        Map<String, String> binding = new HashMap<String, String>();
        binding.put("applicationName", details.getApplicationName());
        binding.put("optsEnvironmentVar", details.getOptsEnvironmentVar());
        binding.put("exitEnvironmentVar", details.getExitEnvironmentVar());
        binding.put("mainClassName", details.getMainClassName());
        binding.put("defaultJvmOpts", createJoinedDefaultJvmOpts(details.getDefaultJvmOpts()));
        binding.put("appNameSystemProperty", details.getAppNameSystemProperty());
        binding.put("appHomeRelativePath", createJoinedAppHomeRelativePath(details.getScriptRelPath()).replace('/', '\\'));
        binding.put("classpath", createJoinedClasspath(details.getClasspath()));
        return binding;
    }

    private String createJoinedClasspath(Iterable<String> classpath) {
        Joiner semicolonJoiner = Joiner.on(";");
        return semicolonJoiner.join(Iterables.transform(classpath, new Function<String, String>() {
            public String apply(String input) {
                StringBuilder classpath = new StringBuilder();
                classpath.append("%APP_HOME%\\");
                classpath.append(input.replace("/", "\\"));
                return classpath.toString();
            }
        }));
    }

    private String createJoinedDefaultJvmOpts(Iterable<String> defaultJvmOpts) {
        Iterable<String> quotedDefaultJvmOpts = Iterables.transform(defaultJvmOpts, new Function<String, String>() {
            public String apply(String jvmOpt) {
                StringBuilder quotedDefaultJvmOpt = new StringBuilder();
                quotedDefaultJvmOpt.append("\"");
                quotedDefaultJvmOpt.append(escapeJvmOpt(jvmOpt));
                quotedDefaultJvmOpt.append("\"");
                return quotedDefaultJvmOpt.toString();
            }
        });

        Joiner spaceJoiner = Joiner.on(" ");
        return spaceJoiner.join(quotedDefaultJvmOpts);
    }

    private String escapeJvmOpt(String jvmOpts) {
        boolean wasOnBackslash = false;
        StringBuilder escapedJvmOpt = new StringBuilder();
        CharacterIterator it = new StringCharacterIterator(jvmOpts);

        //argument quoting:
        // - " must be encoded as \"
        // - % must be encoded as %%
        // - pathological case: \" must be encoded as \\\", but other than that, \ MUST NOT be quoted
        // - other characters (including ') will not be quoted
        // - use a state machine rather than regexps
        for(char ch = it.first(); ch != CharacterIterator.DONE; ch = it.next()) {
            String repl = Character.toString(ch);

            if (ch == '%') {
                repl = "%%";
            } else if (ch == '"') {
                repl = (wasOnBackslash ? '\\' : "") + "\\\"";
            }
            wasOnBackslash = ch == '\\';
            escapedJvmOpt.append(repl);
        }

        return escapedJvmOpt.toString();
    }
}
