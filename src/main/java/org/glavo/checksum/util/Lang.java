package org.glavo.checksum.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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

    private final String[] messageTable = null;

    public String getHelpMessage() {
        if (this == CHINESE) {
            return "用法: \n" +
                    "    gchecksum c(reate) [选项]     : 创建校验文件\n" +
                    "    gchecksum v(erify) [选项]     : 使用校验文件对文件进行验证\n" +
                    "    gchecksum u(pdate) [选项]     : 更新已存在的校验文件，打印目录发生的变更\n" +
                    "\n" +
                    "选项：\n" +
                    "    -h -? --help            打印本帮助信息\n" +
                    "    -v --version            打印程序版本信息\n" +
                    "    -f <checksums file>     指定校验文件路径 (默认为 checksums.txt, 使用 '-' 指定为标准输入/输出流，)\n" +
                    "    -d <directory>          指定要验证的文件夹 (默认值为当前工作路径)\n" +
                    "    -y --yes --assume-yes   静默覆盖已存在的 checksums 文件\n" +
                    //"                          (与 -i 选项互斥)\n" +
                    //"    -i <files>            指定要验证的文件列表\n" +
                    //"                          (与 -d 选项互斥)\n" +
                    "    -a --algorithm   <algorithm>\n" +
                    "                            指定将使用的哈希算法 (创建和更新模式下默认为 SHA-256, 校验模式下默认根据哈希值长度自动选择)\n" +
                    "    -n --num-threads <num threads>\n" +
                    "                            指定计算哈希值的并发线程数 (默认为当前逻辑处理器数的一半)";
        } else {
            return "Usage: \n" +
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
                    //"                          (don't use both -d and -i)\n" +
                    //"    -i <files>            Specify the files that will be validated \n" +
                    //"                          (don't use both -d and -i)\n" +
                    "    -a --algorithm <algorithm>\n" +
                    "                            Specify the hash algorithm to be used [default=SHA-256]\n" +
                    "    -n --num-threads <num threads>\n" +
                    "                            Specify the number of threads used for validation";
        }
    }

    public String getVersionInformation() {
        //noinspection ConstantConditions
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Lang.class.getResourceAsStream("Version.txt")))) {
            return reader.readLine();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    // error messages

    public String getUnknownModeMessage() {
        if (this == CHINESE) return "错误：未知模式：%s";
        else return "error: unknown mode: %s";
    }

    public String getMissArgMessage() {
        if (this == CHINESE) return "错误：%s 需要参数";
        else return "error: %s requires an argument";
    }

    public String getParamRespecifiedMessage() {
        if (this == CHINESE) return "错误：选项 %s 被指定多次";
        else return "error: option %s is specified more than once";
    }

    public String getOptionMixedMessage() {
        if (this == CHINESE) return "错误：不能混用选项 %s 和 %s";
        else return "error: can't mix options %s and %s";
    }

    public String getUnsupportedAlgorithmMessage() {
        if (this == CHINESE) return "错误：不支持的哈希算法：%s";
        else return "error: unsupported hash algorithm: %s";
    }

    public String getInvalidOptionValueMessage() {
        if (this == CHINESE) return "错误：无效参数值：%s";
        else return "error: invalid option value: %s";
    }

    public String getPathNotExistMessage() {
        if (this == CHINESE) return "错误：路径 '%s' 不存在";
        else return "error: path '%s' does not exist";
    }

    public String getPathIsAFileMessage() {
        if (this == CHINESE) return "错误：路径 '%s' 是一个文件";
        else return "error: path '%s' is a file";
    }

    public String getFileNotExistMessage() {
        if (this == CHINESE) return "错误：文件 '%s' 不存在";
        else return "error: file '%s' does not exist";
    }

    public String getFileCannotBeReadMessage() {
        if (this == CHINESE) return "错误：无法读取文件 '%s'";
        else return "error: file '%s' cannot be read";
    }

    public String getNoMatchHasher() {
        if (this == CHINESE) return "错误：无法自动选择哈希算法，请使用 --algorithm 选项指定算法";
        else return "error: unable to automatically select hash algorithm, please use the --algorithm option to specify the algorithm";
    }

    public String getInvalidHashRecordMessage() {
        if (this == CHINESE) return "错误：无效哈希记录 '%s'";
        else return "error: invalid hash record '%s'";
    }

    public String getPathIsDirMessage() {
        if (this == CHINESE) return "错误：路径 '%s' 是一个目录";
        else return "error: path '%s' is a directory";
    }

    public String getHashNotMatchMessage() {
        if (this == CHINESE) return "错误：文件 '%s' 的哈希值（%s）不匹配记录（%s）";
        else return "error: hash value of file '%s'(%s) does not match the value in the record(%s)";
    }

    public String getInvalidOptionMessage() {
        if (this == CHINESE) return "错误：无效参数：%s";
        else return "error: invalid option: %s";
    }

    // messages

    public String getVerificationCompletedMessage() {
        if (this == CHINESE) return "校验完毕：%d 个成功，%d 个失败";
        else return "Verification completed: %d success, %d failure";
    }

    public String getDoneMessage() {
        if (this == CHINESE) return "完成";
        else return "Done";
    }

    public String getOverwriteFileMessage() {
        if (this == CHINESE) return "已存在的文件 '%s' 将被覆盖，是否继续？[y/n]";
        else return "The existing file '%s' will be overwritten, do you want to continue? [y/n]";
    }

    public String getCreateFileMessage() {
        if (this == CHINESE) return "文件 '%s' 尚不存在，是否想要创建它？[y/n]";
        else return "The file '%s' not exist, do you want to create it? [y/n]";
    }

    public String getNewFileBeRecordedMessage() {
        if (this == CHINESE) return "变化：新的文件 '%s' 将被记录";
        else return "change: The new file '%s' will be recorded";
    }

    public String getFileHashUpdatedMessage() {
        if (this == CHINESE) return "变化：文件 '%s' 在记录中的哈希值（%s）被更新为 '%s'";
        else return "change: The hash value of file '%s' (%s) will be updated to '%s'";
    }

    public String getFileRecordBeRemoved() {
        if (this == CHINESE) return "变化：文件 '%s' 的记录将被删除";
        else return "change: The record of file '%s' will be deleted";
    }
}
