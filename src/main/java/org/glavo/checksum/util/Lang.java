/*
 * Copyright 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glavo.checksum.util;

import org.glavo.checksum.Main;
import org.glavo.checksum.hash.Hasher;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public enum Lang {
    CHINESE,
    OTHER;

    private static final Lang INSTANCE;

    static {
        String lang = System.getProperty("user.language");
        if (lang == null) {
            String l = System.getenv("LANG");
            if (l != null && l.startsWith("zh_")) {
                lang = "zh";
            }
        }

        if ("zh".equalsIgnoreCase(lang)) {
            INSTANCE = CHINESE;
        } else {
            INSTANCE = OTHER;
        }
    }

    public static Lang getInstance() {
        return INSTANCE;
    }

    private String getHelpMessageBase() {
        if (this == CHINESE) {
            return "用法:\n" +
                   "    gchecksum c(reate) [选项]     : 创建校验文件\n" +
                   "    gchecksum v(erify) [选项]     : 使用校验文件对文件进行验证\n" +
                   "    gchecksum u(pdate) [选项]     : 更新已存在的校验文件, 打印目录发生的变更\n" +
                   "\n" +
                   "选项:\n" +
                   "    -h -? --help            打印本帮助信息\n" +
                   "    -v --version            打印程序版本信息\n" +
                   "    -f <checksums file>     指定校验文件路径 (默认为 checksums.txt, 使用 '-' 指定为标准输入/输出流)\n" +
                   "    -d <directory>          指定要验证的文件夹 (默认值为当前工作路径)\n" +
                   "    -y --yes --assume-yes   静默覆盖已存在的 checksums 文件\n" +
                   "    -a --algorithm   <algorithm>\n" +
                   "                            指定将使用的哈希算法 (创建和更新模式下默认为 SHA-256, 校验模式下默认根据哈希值长度自动选择)\n" +
                   "    -n --num-threads <num threads>\n" +
                   "                            指定计算哈希值的并发线程数 (默认值为 4)";
        } else {
            return "Usage:\n" +
                   "    gchecksum c(reate) [options]    : Create checksums file\n" +
                   "    gchecksum v(erify) [options]    : Verify files using checksums file\n" +
                   "    gchecksum u(pdate) [options]    : Update the existing checksums file and print the changes\n" +
                   "\n" +
                   "Options:\n" +
                   "    -h -? --help            Print this help message\n" +
                   "    -v --version            Print version information\n" +
                   "    -f <checksums file>     Specify the checksums file [default=checksums.txt]\n" +
                   "    -d <directory>          Specify the directory that will be validated [default=.] \n" +
                   "    -y --yes --assume-yes   Overwrite the existing checksums file silently\n" +
                   "    -a --algorithm <algorithm>\n" +
                   "                            Specify the hash algorithm to be used [default=SHA-256]\n" +
                   "    -n --num-threads <num threads>\n" +
                   "                            Specify the number of threads used for validation [default=4]";
        }
    }

    private String getHelpMessageAlgorithms() {
        if (this == CHINESE) return "可用哈希算法:";
        else return "Available Hash Algorithms:";
    }

    private String getHelpMessageExperimentalAlgorithms() {
        if (this == CHINESE) return "可用哈希算法 (实验性):";
        else return "Available Hash Algorithms (Experimental):";
    }

    private static void formatAlgorithms(StringBuilder builder, List<String> algorithms) {
        final int maxLength = 16;

        int index = 0;
        for (String algorithm : algorithms) {
            if (index > 0 && index % 4 == 0) {
                builder.append('\n');
            }

            builder.append(algorithm);

            int trailingSpace = maxLength - algorithm.length();

            for (int i = 0; i < trailingSpace; i++) {
                builder.append(' ');
            }

            index++;
        }
    }

    public String getHelpMessage() {
        List<String> algorithms = Hasher.getAlgorithms();
        List<String> experimentalAlgorithms = Hasher.getExperimentalAlgorithms();

        StringBuilder builder = new StringBuilder();
        builder.append(getVersionInformation()).append("\n\n");
        builder.append(getHelpMessageBase()).append("\n\n");

        builder.append(getHelpMessageAlgorithms()).append("\n\n");
        formatAlgorithms(builder, algorithms);
        builder.append("\n");

        if (!experimentalAlgorithms.isEmpty()) {
            builder.append("\n");
            builder.append(getHelpMessageExperimentalAlgorithms()).append("\n\n");
            formatAlgorithms(builder, experimentalAlgorithms);
            builder.append("\n");
        }

        return builder.toString();
    }

    public String getVersionInformation() {
        String version = null;
        String provider = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("version.txt"), StandardCharsets.US_ASCII))) {
            version = reader.readLine();
        } catch (Throwable ignored) {
        }
        try {
            provider = Cipher.getInstance("AES/GCM/NoPadding").getProvider().toString();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | NullPointerException ignored) {
        }

        if (version == null) version = "unknown";

        StringBuilder builder = new StringBuilder();
        builder.append("gchecksum ").append(version).append(" by Glavo");
        if (provider != null) {
            builder.append(" (").append(provider).append(")");
        }
        return builder.toString();
    }

    // error messages

    public String getUnknownModeMessage(String mode) {
        if (this == CHINESE) return "错误: 未知模式: " + mode;
        else return "error: unknown mode: " + mode;
    }

    public String getMissArgMessage(String option) {
        if (this == CHINESE) return "错误: " + option + " 需要参数";
        else return "error: " + option + " requires an argument";
    }

    public String getParamRespecifiedMessage(String option) {
        if (this == CHINESE) return "错误: 选项 " + option + " 被指定多次";
        else return "error: option " + option + " is specified more than once";
    }

    public String getOptionMixedMessage(String option1, String option2) {
        if (this == CHINESE) return "错误: 不能混用选项 " + option1 + " 和 " + option2;
        else return "error: can't mix options " + option1 + " and " + option2;
    }

    public String getUnsupportedAlgorithmMessage(String algo) {
        if (this == CHINESE) return "错误: 不支持的哈希算法: " + algo;
        else return "error: unsupported hash algorithm: " + algo;
    }

    public String getInvalidOptionValueMessage(String option, String value) {
        if (this == CHINESE) return "错误: 选项 " + option + " 的参数值 " + value + " 无效";
        else return "error: invalid value " + value + "  for option " + option;
    }

    public String getPathNotExistMessage(Path path) {
        if (this == CHINESE) return "错误: 路径 '" + path + "' 不存在";
        else return "error: path '" + path + "' does not exist";
    }

    public String getPathIsAFileMessage(Path path) {
        if (this == CHINESE) return "错误: 路径 '" + path + "' 是一个文件";
        else return "error: path '" + path + "' is a file";
    }

    public String getFileNotExistMessage(Path file) {
        if (this == CHINESE) return "错误: 文件 '" + file + "' 不存在";
        else return "error: file '" + file + "' does not exist";
    }

    public String getFileCannotBeReadMessage(Path file) {
        if (this == CHINESE) return "错误: 无法读取文件 '" + file + "'";
        else return "error: file '" + file + "' cannot be read";
    }

    public String getNoMatchHasher() {
        if (this == CHINESE) return "错误: 无法自动选择哈希算法, 请使用 --algorithm 选项指定算法";
        else
            return "error: unable to automatically select hash algorithm, please use the --algorithm option to specify the algorithm";
    }

    public String getInvalidHashRecordMessage(String record) {
        if (this == CHINESE) return "错误: 无效哈希记录 '" + record + "'";
        else return "error: invalid hash record '" + record + "'";
    }

    public String getPathIsDirMessage(Path path) {
        if (this == CHINESE) return "错误: 路径 '" + path + "' 是一个目录";
        else return "error: path '" + path + "' is a directory";
    }

    public String getHashNotMatchMessage(Path file, String hash, String oldHash) {
        if (this == CHINESE) return String.format("错误: 文件 '%s' 的哈希值(%s) 不匹配记录(%s)", file, hash, oldHash);
        else
            return String.format("error: hash value of file '%s'(%s) does not match the value in the record(%s)", file, hash, oldHash);
    }

    public String getInvalidOptionMessage(String option) {
        if (this == CHINESE) return "错误: 无效参数: " + option;
        else return "error: invalid option: " + option;
    }

    public String getErrorOccurredMessage(Path file) {
        if (this == CHINESE) return "错误: 处理文件 '" + file + "' 时发生异常";
        else return "error: an error occurred while processing the file '" + file + "'";
    }

    // messages

    public String getVerificationCompletedMessage(long success, long failure) {
        if (this == CHINESE) return "校验完毕: " + success + " 个成功, " + failure + " 个失败";
        else return "Verification completed: " + success + " success, " + failure + " failure";
    }

    public String getDoneMessage() {
        if (this == CHINESE) return "完成";
        else return "Done";
    }

    public String getOverwriteFileMessage(Path file) {
        if (this == CHINESE) return "已存在的文件 '" + file + "' 将被覆盖, 是否继续?[y/n]";
        else return "The existing file '" + file + "' will be overwritten, do you want to continue? [y/n]";
    }

    public String getCreateFileMessage(Path file) {
        if (this == CHINESE) return "文件 '" + file + "' 尚不存在, 是否想要创建它?[y/n]";
        else return "The file '" + file + "' not exist, do you want to create it? [y/n]";
    }

    public String getNewFileBeRecordedMessage(String file) {
        if (this == CHINESE) return "变化: 新的文件 '" + file + "' 将被记录";
        else return "change: The new file '" + file + "' will be recorded";
    }

    public String getFileHashUpdatedMessage(String file, String hash, String oldHash) {
        if (this == CHINESE)
            return String.format("变化: 文件 '%s' 在记录中的哈希值(%s)被更新为 '%s'", file, hash, oldHash);
        else
            return String.format("change: The hash value of file '%s' (%s) will be updated to '%s'", file, hash, oldHash);
    }

    public String getFileRecordBeRemoved(String file) {
        if (this == CHINESE) return "变化: 文件 '" + file + "' 的记录将被删除";
        else return "change: The record of file '" + file + "' will be deleted";
    }
}
