# gchecksum

[![License](license.svg)](LICENSE)

**Chinese** | English (TODO)

English documents are not available, welcome to contribute.

(English help is available, please execute `gchecksum --help` to view)

一个高效的文件夹校验工具，用于为文件夹下所有文件生成哈希码并保存到文件，
以及使用保存的哈希码对文件夹内容进行校验。

默认使用并发校验，对于 SSD 硬盘有明显提速效果，而机械硬盘请使用参数 `-n 1` 限制为单线程。

简单用法：
```
# 创建校验文件
gchecksum create # 或者 gchecksum c

# 校验文件
gchecksum verify # 或者 gchecksum v

# 更新已存在的校验文件
gchecksum update # 或者 gchecksum u
```

帮助（可以使用 `gchecksum --help` 查看）：
```
用法：
    gchecksum c(reate) [选项]     : 创建校验文件
    gchecksum v(erify) [选项]     : 使用校验文件对文件进行验证
    gchecksum u(pdate) [选项]     : 更新已存在的校验文件，打印目录发生的变更

选项：
    -v --version            打印程序版本信息
    -h -? --help            打印本帮助信息
    -f <checksums file>     指定校验文件路径（默认值为 checksums.txt，使用 '-' 指定为标准输入/输出流）
    -y --yes --assume-yes   静默覆盖已存在的 checksums 文件
    -d <directory>          指定要验证的文件夹（默认值为当前工作路径）
    -a --algorithm          指定将使用的哈希算法（create 模式下默认为 SHA-256，verify 模式下默认根据哈希值长度自动选择）
    -n --num-threads        指定计算哈希值的并发线程数（默认为当前逻辑处理器数的一半）
```

## 安装方法

gchecksum 工具使用 Java 编写，可以在任意拥有 JRE 8 或更高版本的环境下运行。

gchecksum 工具为 Linux 平台提供带 bash 头的特殊 JAR 分发：它们可以普通地用 `java -jar` 执行，也可以在 Linux 上如同脚本般直接运行！

gchecksum 同时会为常见平台提供 Native Image 构建，无需 JRE 环境，但性能会降低。目前 gchecksum 为这些平台提供 Native Image 构建：

* Windows x86-64
* Linux x86-64

### 通用

访问 [GitHub Release 页](https://github.com/Glavo/gchecksum/releases)下载 JAR 文件，并使用 `java -jar` 执行。

### Linux

在 Linux 上安装 Java 版本（需要 JRE 8 或更高版本）：

```shell
sudo sh -c '(echo "#!/usr/bin/env sh" && curl -L https://github.com/Glavo/gchecksum/releases/download/0.11.0/gchecksum-0.11.0) > /usr/local/bin/gchecksum && chmod +x /usr/local/bin/gchecksum'
```

中国大陆用户如果访问 GitHub 缓慢，可以使用 [FastGit](https://doc.fastgit.org/) 加速：

```shell
sudo sh -c '(echo "#!/usr/bin/env sh" && curl -L https://download.fastgit.org/Glavo/gchecksum/releases/download/0.11.0/gchecksum-0.11.0) > /usr/local/bin/gchecksum && chmod +x /usr/local/bin/gchecksum'
```

（备选）安装 Native Image 版本（**无需 JRE 环境，但性能差于 Java 版本**）：

```shell
sudo sh -c '(echo "#!/usr/bin/env sh" && curl -L https://github.com/Glavo/gchecksum/releases/download/0.11.0/gchecksum-0.11.0-native-image) > /usr/local/bin/gchecksum && chmod +x /usr/local/bin/gchecksum'
```

使用 [FastGit](http://fastgit.org/) 镜像：

```shell
sudo sh -c '(echo "#!/usr/bin/env sh" && curl -L https://download.fastgit.org/Glavo/gchecksum/releases/download/0.11.0/gchecksum-0.11.0-native-image) > /usr/local/bin/gchecksum && chmod +x /usr/local/bin/gchecksum'
```

### Windows

下载为 Windows 生成的 Native Image 镜像：

* [gchecksum-0.11.0-native-image.exe](https://github.com/Glavo/gchecksum/releases/download/0.11.0/gchecksum-0.11.0-native-image.exe)
* [gchecksum-0.11.0-native-image.exe](https://download.fastgit.org/Glavo/gchecksum/releases/download/0.11.0/gchecksum-0.11.0-native-image.exe)（FastGit 镜像链接）

## 介绍

gchecksum 有三种模式：

* 创建（create）模式：创建记录文件与其哈希值的校验文件。
* 校验（verify）模式：校验文件是否与记录中的哈希值所匹配。
* 更新（update）模式：更新已存在的校验文件，打印发生变更的文件。

通过将 `create`（缩写为 `c`）、 `verify`（缩写为 `v`）或 `update`（缩写为 `u`） 作为第一个参数传递指定。
不指定的情况下，默认为校验模式。

默认情况下，创建模式覆盖已存在的校验文件前会询问用户是否想要覆盖。传入 `-y` 选项让工具静默覆盖已有校验文件。

`-f` 选项用于指定 checksums 文件的路径，默认为当前工作路径下的 `checksums.txt` 文件。

可以用 `-` 代替文件名，指定工具使用标准输入/输出流代替 checksums 文件。

`-d` 选项用于指定处理的根路径，默认为当前工作路径。

`-a` 选项用于指定使用的哈希算法。
未指定时，创建模式和更新模式会默认选择 SHA-256 算法生成校验文件，而校验模式会根据校验文件第一行中哈希码的长度来自动判断算法。

当前支持的哈希算法有（需要运行时 Java 环境支持对应算法）：

* MD5
* SHA-1
* SHA-224
* SHA-256
* SHA-384
* SHA-512

**注意：** 校验模式自动选择算法通常很准确（因为当前哈希码位数与算法一一对应），
但未来可能会引入其他冲突的算法（例如，SHA-512/256 算法的哈希码长度与 SHA-256 相同），这时就必须显式指定。

`--num-threads`（`-n`） 选项用于指定并发计算哈希值的线程数，必须为正整数。
默认值为运行平台逻辑处理器数的一半（`Runtime.getRuntime().availableProcessors() / 2`）

## checksums 文件

checksums 文件内容形式如下：
```
862b930590e9abbc9595179a62b3e640a4ecfd22b324f09843375412b9934cc5  Config.json
5d7090789c8956083887f10bea8628a58c179b3422c7d53bff315e150a812b25  libs/aliyun-java-sdk-alidns-2.6.29.jar
d9ff177868630668f2da1e4c8b30d215440e4bbaa953d9ccafaaba200a2f7ffc  libs/aliyun-java-sdk-core-4.5.20.jar
12ff01eeaf0c09c6a68f2ec024b3bf9fa4cad6e68b74b968bf62c7f759047032  libs/annotations-19.0.0.jar
1f58b77470d8d147a0538d515347dd322f49a83b9e884b8970051160464b65b3  libs/apiguardian-api-1.0.0.jar
d68131283c01f81cc1532ae26aebaf760f6e0b92675a0e13816d45e7f28a7f58  libs/atomicfu-common-0.14.1.jar
e73c935ed4ecb62de04b56fdf2d0256e7757b47887551a28a34cd5eafa465f3b  libs/atomicfu-jvm-0.15.1.jar
a4f463ce552b908a722fa198ef4892a226b3225e453f8df10d5c0a5bfe5db6b6  libs/bcprov-jdk15on-1.64.jar
e599d5318e97aa48f42136a2927e6dfa4e8881dff0e6c8e3109ddbbff51d7b7d  libs/commons-codec-1.11.jar
daddea1ea0be0f56978ab3006b8ac92834afeefbd9b7e4e6316fca57df0fa636  libs/commons-logging-1.2.jar
c8fb4839054d280b3033f800d1f5a97de2f028eb8ba2eb458ad287e536f3f25f  libs/gson-2.8.6.jar
6fe9026a566c6a5001608cf3fc32196641f6c1e5e1986d1037ccdbd5f31ef743  libs/httpclient-4.5.13.jar
f956209e450cb1d0c51776dfbd23e53e9dd8db9a1298ed62b70bf0944ba63b28  libs/httpcore-4.4.14.jar
aad60635eee567254ed29f18fb18c0f9e4c4dacf51c8229128203183bb35e2dd  libs/ini4j-0.5.4.jar
43fdef0b5b6ceb31b0424b208b930c74ab58fac2ceeb7b3f6fd3aeb8b5ca4393  libs/javax.activation-api-1.2.0.jar
2f8e3b5c3c0e3eddd11ed025d3937085d9b7a8f6330ccc9e1497dd2f02297875  logs/2021-03-10_045632.log
9a728db7640fb6d4b0f257ad94d0185dd76e6ccd650896acee7d80dd835d8f64  logs/2021-03-10_045852.log
738c3a5d41a582929be1be1374452b53c098a3678f896727a3916155dc137ee6  logs/2021-03-10_050400.log
0d60e31e04ad4918a25273ad082bcf5b2064792dc5fbfe27c28a39cd3cefa4eb  logs/2021-03-11_120522.log
520c311f7684a81a6d8acdd92f416e8370700c23f1b669f8a7dfce60003f0119  logs/2021-03-11_120659.log
8f9a12d9bee054d28fe40ae73e5cce128d8cd4c108ca75e7066d1f7f1edd981e  logs/2021-03-12_203327.log
```

每行的内容为 哈希码-空格-文件相对路径。

文件所有哈希码所用算法必须一致。文件不存储哈希码使用的算法。

gchecksum 生成时会按路径排序，但校验时不要求顺序。

哈希码与文件路径之间可以间隔任意个空格，而生成模式下默认生成为 BSD 风格的两空格，与 Linux 下 `shasum` 系工具兼容，
可以使用 `shasum -c checksums.txt` 进行校验。

## 性能

基于 0.6.0 测试，使用 OpenJDK 默认 SHA-256 实现。

使用 [Amazon Corretto Crypto Provider](https://github.com/corretto/amazon-corretto-crypto-provider) 替换 JDK 内置哈希算法实现
可以获得轻微性能提升，但主要瓶颈应该处于文件 io 上。

gchecksum 当前实现较为简略，未来会考虑更细致地优化性能。

测试平台：

* CPU：AMD Ryzen 7 3700X 8-Core @ 16x 4.1GHz
* 内存：DDR4 双通道 32G 2666MHz
* 系统：Ubuntu 20.04
* Java：OpenJDK 16.0.1
* 硬盘：SN750 500G

### 测试1
47 个压缩文件，共 188G。

默认参数（8 线程）：

* 生成：1m0.787s
* 验证：1m2.424s

参数 `-n 1`（单线程）：

* 生成：3m27.069s
* 验证：3m27.096s

（参照组）使用 Linux 的 sha256sum 工具进行校验，命令 `sha256sum --quiet -c checksums.txt`：

* 验证：11m53.429s

### 测试2
Minecraft 服务器文件夹，5604 个文件，7.2G。

默认参数（8 线程）：

* 生成：0.859s
* 验证：0.908s

参数 `-n 1`（单线程）：

* 生成：4.995s
* 验证：4.947s

（参照组）使用 Linux 的 sha256sum 工具进行校验，命令 `sha256sum --quiet -c checksums.txt`：

* 验证：26.187s
