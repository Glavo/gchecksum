# gchecksum

一个简单的文件夹校验工具，用于为文件夹下所有文件生成哈希码并保存到文件，
以及使用保存的哈希码对文件夹内容进行校验。

支持并默认使用并发校验。

简单用法：
```
# 创建校验码
gchecksum create # 或者 gchecksum c

# 校验文件
gchecksum verify # 或者 gchecksum v
```



帮助（可以使用 `gchecksum --help` 查看）：
```
用法:
    gchecksum c(reate) [选项]     : 创建校验文件
    gchecksum v(erify) [选项]     : 使用校验文件对文件进行验证

Options:
    -v --version            打印程序版本信息
    -h -? --help            打印本帮助信息
    -f <checksums file>     指定校验文件路径（默认值为 checksums.txt）
    -x                      将校验值输出至标准输出流，或从标准输入流读取校验值（与 -f 选项互斥）
    -d <directory>          指定要验证的文件夹（默认值为当前工作路径）
    -a --algorithm          指定将使用的哈希算法（create 模式下默认为 SHA-256，verify 模式下默认根据哈希值长度自动选择）
    -n --num-threads        指定计算哈希值的并发线程数（默认为当前逻辑处理器数的一半）
```

gchecksum 有两种模式：创建（create）模式，校验（verify）模式。
通过将 `create`（缩写为 `c`）或 `verify`（缩写为 `v`） 作为第一个参数传递指定。
不指定的情况下，默认为校验模式。

`-f` 选项用于指定 checksums 文件的路径，默认为当前工作路径下的 `checksums.txt` 文件。

可以用 `-x` 选项指定使用标准输入/输出流代替 checksums 文件，该选项与 `-f` 选项互斥。

`-d` 选项用于指定处理的根路径，默认为当前工作路径。

`-a` 选项用于指定使用的哈希算法。
未指定时，创建模式会默认选择 SHA-256 算法生成校验文件，而校验模式会根据校验文件第一行中哈希码的长度来自动判断算法。

当前支持的哈希算法有（需要运行时 Java 环境支持对应算法）：

* MD5
* SHA-1
* SHA-224
* SHA-256
* SHA-384
* SHA-512

**注意：** 校验模式自动选择算法通常很准确（因为当前哈希码位数与算法一一对应），
但未来可能会引入其他冲突的算法（例如，SHA-512/256 算法的哈希码长度与 SHA-256 相同），这时就必须显式指定、

`--num-threads`（`-n`） 选项用于指定并发计算哈希值的线程数，必须为正整数。
默认值为运行平台逻辑处理器数的一半（`Runtime.getRuntime().availableProcessors() / 2`）

