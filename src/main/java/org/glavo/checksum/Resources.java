package org.glavo.checksum;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class Resources {
    private static final String HELP_MESSAGE_EN =
            "Usage: \n" +
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
                    //"                            (don't use both -d and -i)\n" +
                    //"    -i <files>              Specify the files that will be validated \n" +
                    //"                            (don't use both -d and -i)\n" +
                    "    -a --algorithm <algorithm>\n" +
                    "                            Specify the hash algorithm to be used [default=SHA-256]\n" +
                    "    -n --num-threads <num threads>\n" +
                    "                            Specify the number of threads used for validation";

    private static final String HELP_MESSAGE_ZH =
            "用法: \n" +
                    "    gchecksum c(reate) [选项]     : 创建校验文件\n" +
                    "    gchecksum v(erify) [选项]     : 使用校验文件对文件进行验证\n" +
                    "    gchecksum u(pdate) [选项]     : 更新已存在的校验文件，打印目录发生的变更\n" +
                    "\n" +
                    "选项：\n" +
                    "    -h -? --help            打印本帮助信息\n" +
                    "    -v --version            打印程序版本信息\n" +
                    "    -f <checksums file>     指定校验文件路径（使用 '-' 指定为标准输入/输出流，默认为 checksums.txt）\n" +
                    "    -d <directory>          指定要验证的文件夹（默认值为当前工作路径）\n" +
                    "    -y --yes --assume-yes   静默覆盖已存在的 checksums 文件\n" +
                    //"                            （与 -i 选项互斥）\n" +
                    //"    -i <files>              指定要验证的文件列表\n" +
                    //"                            （与 -d 选项互斥）\n" +
                    "    -a --algorithm   <algorithm>\n" +
                    "                            指定将使用的哈希算法（create 模式下默认为 SHA-256，verify 模式下默认根据哈希值长度自动选择）\n" +
                    "    -n --num-threads <num threads>\n" +
                    "                            指定计算哈希值的并发线程数（默认为当前逻辑处理器数的一半）";

    private static final String[] ERROR_TABLE_EN = {
            "error: unknown mode: %s",
            "error: %s requires an argument",
            "error: option %s is specified more than once",
            "error: can't mix options %s and %s",
            "error: unsupported hash algorithm: %s",
            "error: invalid option value: %s",
            "error: path '%s' does not exist",
            "error: path '%s' is a file",
            "error: file '%s' does not exist",
            "error: file '%s' cannot be read",
            "error: invalid hash record '%s'",
            "error: path '%s' is a directory",
            "error: hash value of file '%s'(%s) does not match the value in the record(%s)",
            "error: invalid option: %s"
    };

    private static final String[] ERROR_TABLE_ZH = {
            "错误：未知模式：%s",
            "错误：%s 需要参数",
            "错误：选项 %s 被指定多次",
            "错误：不能混用选项 %s 和 %s",
            "错误：不支持的哈希算法：%s",
            "错误：无效参数值：%s",
            "错误：路径 '%s' 不存在",
            "错误：路径 '%s' 是一个文件",
            "错误：文件 '%s' 不存在",
            "错误：文件 '%s' 无法读取",
            "错误：无效哈希记录 '%s'",
            "错误：路径 '%s' 是一个目录",
            "错误：文件 '%s' 的哈希值（%s）不匹配记录（%s）",
            "错误：无效参数：%s"
    };

    private static final String[] MESSAGE_TABLE_EN = {
            "Verification completed: %d success, %d failure",
            "Done",
            "The existing file '%s' will be overwritten, do you want to continue? [y/n]",
            "The file '%s' not exist, do you want to create it? [y/n]",
            "change: The new file '%s' be recorded",
            "change: The hash value of file '%s' (%s) will be updated to '%s'",
            "change: The record of file '%s' will be deleted"
    };

    private static final String[] MESSAGE_TABLE_ZH = {
            "校验完毕：%d 个成功，%d 个失败",
            "完成",
            "已存在的文件 '%s' 将被覆盖，是否继续？[y/n]",
            "文件 '%s' 尚不存在，是否想要创建它？[y/n]",
            "变化：新的文件 '%s' 将被记录",
            "变化：文件 '%s' 在记录中的哈希值（%s）被更新为 '%s'",
            "变化：文件 '%s' 的记录将被删除"
    };

    private static final Resources INSTANCE;

    static {
        String lang = System.getProperty("user.language");
        if (lang == null) {
            String l = System.getenv("LANG");
            if (l != null && l.startsWith("zh_")) {
                lang = "zh";
            }
        }

        if ("zh".equalsIgnoreCase(lang)) {
            INSTANCE = new Resources(HELP_MESSAGE_ZH, ERROR_TABLE_ZH, MESSAGE_TABLE_ZH);
        } else {
            INSTANCE = new Resources(HELP_MESSAGE_EN, ERROR_TABLE_EN, MESSAGE_TABLE_EN);
        }
    }

    public static Resources getInstance() {
        return INSTANCE;
    }

    private final String helpMessage;
    private final String[] errorTable;
    private final String[] messageTable;

    private Resources(String helpMessage, String[] errorTable, String[] messageTable) {
        this.helpMessage = helpMessage;
        this.errorTable = errorTable;
        this.messageTable = messageTable;
    }

    public final String getHelpMessage() {
        return helpMessage;
    }

    public final String getVersionInformation() {
        //noinspection ConstantConditions
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Resources.class.getResourceAsStream("Version.txt")))) {
            return reader.readLine();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    public final String getUnknownModeMessage() {
        return errorTable[0];
    }

    public final String getMissArgMessage() {
        return errorTable[1];
    }

    public final String getParamRespecifiedMessage() {
        return errorTable[2];
    }

    public final String getOptionMixedMessage() {
        return errorTable[3];
    }

    public final String getUnsupportedAlgorithmMessage() {
        return errorTable[4];
    }

    public final String getInvalidOptionValueMessage() {
        return errorTable[5];
    }

    public final String getPathNotExistMessage() {
        return errorTable[6];
    }

    public final String getPathIsAFileMessage() {
        return errorTable[7];
    }

    public final String getFileNotExistMessage() {
        return errorTable[8];
    }

    public final String getFileCannotBeReadMessage() {
        return errorTable[9];
    }

    public final String getInvalidHashRecordMessage() {
        return errorTable[10];
    }

    public final String getPathIsDirMessage() {
        return errorTable[11];
    }

    public final String getHashNotMatchMessage() {
        return errorTable[12];
    }

    public final String getInvalidOptionMessage() {
        return errorTable[13];
    }

    public final String getVerificationCompletedMessage() {
        return messageTable[0];
    }

    public final String getDoneMessage() {
        return messageTable[1];
    }

    public final String getOverwriteFileMessage() {
        return messageTable[2];
    }

    public final String getCreateFileMessage() {
        return messageTable[3];
    }

    public final String getNewFileBeRecordedMessage() {
        return messageTable[4];
    }

    public final String getFileHashUpdatedMessage() {
        return messageTable[5];
    }

    public final String getFileRecordBeRemoved() {
        return messageTable[6];
    }
}
