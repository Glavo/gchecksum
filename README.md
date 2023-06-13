# gchecksum

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

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
    -n --num-threads        指定计算哈希值的并发线程数（默认值为 4）
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
sudo sh -c '(echo "#!/usr/bin/env sh" && curl -L https://github.com/Glavo/gchecksum/releases/download/0.13.0/gchecksum-0.13.0.sh) > /usr/local/bin/gchecksum && chmod +x /usr/local/bin/gchecksum'
```

（备选）安装 Native Image 版本（**无需 JRE 环境，但性能差于 Java 版本**）：

```shell
sudo sh -c '(echo "#!/usr/bin/env sh" && curl -L https://github.com/Glavo/gchecksum/releases/download/0.13.0/gchecksum-0.13.0) > /usr/local/bin/gchecksum && chmod +x /usr/local/bin/gchecksum'
```

### Windows

下载为 Windows 生成的 Native Image 镜像：

* [gchecksum-0.13.0-native-image.exe](https://github.com/Glavo/gchecksum/releases/download/0.13.0/gchecksum-0.13.0.exe)

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

* CRC32
* CRC32C (Java 9+)
* Adler32
* MD5
* SHA-1
* SHA-2 
  * SHA-224
  * SHA-256
  * SHA-384
  * SHA-512
* SHA-3 (Java 9+)
  * SHA3-224
  * SHA3-256
  * SHA3-512
* xxHash64 (Experimental)
* xxHash128 (Experimental)

校验模式下会自动检测的算法有：

* MD5
* SHA-1
* SHA-224
* SHA-256
* SHA-384
* SHA-512

如果 `checksums.txt` 文件使用了不支持自动检测的算法，请使用 `-a` 选项显式指定要用的算法。

`--num-threads`（`-n`） 选项用于指定并发计算哈希值的线程数，必须为正整数。默认值为 4。

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

## Performance Benchmarking

Test Platform:

* CPU: AMD Ryzen 7 3700X 8-Core @ 16x 4.0GHz
* Memory: DDR4 3200AA 32GiB
* SATA SSD: ZHITAI SC001 256G
* NVMe SSD: KIOXIA EXCERIA G2

Environment:

* System: Ubuntu 23.04 (Linux 6.2.0-20-generic)
* Java: LibericaJDK 20.0.1
* gchecksum: 0.14.0
* xxhsum: 0.8.1
* shasum: 6.0.2

All test samples are randomly generated by [RandomFileGenerator 0.2.0](https://github.com/Glavo/RandomFileGenerator) (Options: `-e 0 -n <number of files> -s <size of file> -o file-%d.bin`).

* Small Files:  1048576 * 1KiB (=  1GiB)
* Medium Files:   10240 * 1MiB (= 10GiB)
* Large Files:        8 * 8GiB (= 64GiB)

I ran the benchmark on RAM (tmpfs) and SSD.
When running on an SSD, I perform `sync && sudo bash -c "echo 3 > /proc/sys/vm/drop_caches"` to clean the page cache to simulate reading cold data before each benchmark run.

### SHA-256

#### On tmpfs

* Small Files
  * sha256sum :             7.36 secs
  * gchecksum (`-n 1`):     7.33 secs
  * gchecksum (`-n 4`):     3.20 secs
  * gchecksum (`-n 8`):     2.23 secs

* Medium Files
  * sha256sum :            36.84 secs
  * gchecksum (`-n 1`):     6.91 secs
  * gchecksum (`-n 4`):     1.83 secs
  * gchecksum (`-n 8`):     1.01 secs

#### On SATA SSD

* Small Files
  * sha256sum :            79.89 secs 
  * gchecksum (`-n 1`):    81.69 secs
  * gchecksum (`-n 4`):    29.75 secs
  * gchecksum (`-n 8`):    20.85 secs

* Medium Files
  * sha256sum :            44.57 secs
  * gchecksum (`-n 1`):    29.25 secs
  * gchecksum (`-n 4`):    19.74 secs 
  * gchecksum (`-n 8`):    19.75 secs 

* Large Files
  * sha256sum :           247.34 secs
  * gchecksum (`-n 1`):   193.12 secs
  * gchecksum (`-n 4`):   126.37 secs
  * gchecksum (`-n 8`):   126.36 secs

#### On NVMe SSD

* Small Files
  * sha256sum :            41.70 secs
  * gchecksum (`-n 1`):    42.12 secs
  * gchecksum (`-n 4`):    20.77 secs
  * gchecksum (`-n 8`):    30.75 secs

* Medium Files
  * sha256sum :            39.60 secs
  * gchecksum (`-n 1`):    11.95 secs
  * gchecksum (`-n 4`):     5.79 secs
  * gchecksum (`-n 8`):     5.31 secs

* Large Files
  * sha256sum :           246.47 secs
  * gchecksum (`-n 1`):    61.96 secs
  * gchecksum (`-n 4`):    34.54 secs
  * gchecksum (`-n 8`):    33.70 secs

### SHA-512

#### On tmpfs

* Small Files
  * sha512sum :            6.41 secs
  * gchecksum (`-n 1`):    8.52 secs
  * gchecksum (`-n 4`):    3.46 secs
  * gchecksum (`-n 8`):    2.37 secs

* Medium Files
  * sha512sum :           25.48 secs
  * gchecksum (`-n 1`):   15.68 secs
  * gchecksum (`-n 4`):    4.05 secs
  * gchecksum (`-n 8`):    2.13 secs

#### On SATA SSD

* Small Files
  * sha512sum :           79.87 secs
  * gchecksum (`-n 1`):   83.46 secs
  * gchecksum (`-n 4`):   30.13 secs 
  * gchecksum (`-n 8`):   21.02 secs

* Medium Files
  * sha512sum :           36.03 secs
  * gchecksum (`-n 1`):   33.94 secs
  * gchecksum (`-n 4`):   20.01 secs
  * gchecksum (`-n 8`):   19.83 secs

* Large Files
  * sha512sum :          197.97 secs
  * gchecksum (`-n 1`):  213.30 secs
  * gchecksum (`-n 4`):  130.46 secs
  * gchecksum (`-n 8`):  131.00 secs  

#### On NVMe SSD

* Small Files
  * sha512sum :           41.57 secs
  * gchecksum (`-n 1`):   44.34 secs
  * gchecksum (`-n 4`):   25.73 secs
  * gchecksum (`-n 8`):   29.18 secs

* Medium Files
  * sha512sum :           28.28 secs
  * gchecksum (`-n 1`):   20.22 secs
  * gchecksum (`-n 4`):    7.12 secs
  * gchecksum (`-n 8`):    5.38 secs

* Large Files
  * sha512sum :          174.12 secs
  * gchecksum (`-n 1`):  118.13 secs
  * gchecksum (`-n 4`):   40.20 secs
  * gchecksum (`-n 8`):   34.13 secs

### XXH64

#### On tmpfs

* Small Files
  * xxh64sum:              3.53 secs      
  * gchecksum (`-n 1`):    6.65 secs
  * gchecksum (`-n 4`):    3.05 secs
  * gchecksum (`-n 8`):    2.15 secs

* Medium Files
  * xxh64sum:              1.50 secs    
  * gchecksum (`-n 1`):    2.27 secs
  * gchecksum (`-n 4`):    0.73 secs
  * gchecksum (`-n 8`):    0.60 secs

#### On SATA SSD

* Small Files
  * xxh64sum:             76.14 secs
  * gchecksum (`-n 1`):   79.76 secs
  * gchecksum (`-n 4`):   29.49 secs
  * gchecksum (`-n 8`):   20.53 secs

* Medium Files
  * xxh64sum:             30.53 secs
  * gchecksum (`-n 1`):   28.81 secs
  * gchecksum (`-n 4`):   19.75 secs
  * gchecksum (`-n 8`):   19.72 secs

* Large Files
  * xxh64sum:            212.45 secs
  * gchecksum (`-n 1`):  192.59 secs 
  * gchecksum (`-n 4`):  126.89 secs
  * gchecksum (`-n 8`):  126.45 secs 

#### On NVMe SSD

* Small Files
  * xxh64sum :           38.60 secs
  * gchecksum (`-n 1`):  42.50 secs
  * gchecksum (`-n 4`):  22.81 secs
  * gchecksum (`-n 8`):  29.99 secs

* Medium Files
  * xxh64sum :            9.00 secs
  * gchecksum (`-n 1`):   8.76 secs
  * gchecksum (`-n 4`):   5.63 secs
  * gchecksum (`-n 8`):   5.30 secs

* Large Files
  * xxh64sum :           51.87 secs
  * gchecksum (`-n 1`):  56.08 secs
  * gchecksum (`-n 4`):  34.13 secs
  * gchecksum (`-n 8`):  33.73 secs

### XXH128

#### On tmpfs

* Small Files
  * xxh128sum:             3.63 secs
  * gchecksum (`-n 1`):    6.65 secs
  * gchecksum (`-n 4`):    2.82 secs
  * gchecksum (`-n 8`):    2.21 secs

* Medium Files
  * xxh128sum:             1.01 secs 
  * gchecksum (`-n 1`):    2.97 secs
  * gchecksum (`-n 4`):    1.14 secs
  * gchecksum (`-n 8`):    0.83 secs

#### On SATA SSD

* Small Files
  * xxh128sum:            76.44 secs
  * gchecksum (`-n 1`):   79.39 secs
  * gchecksum (`-n 4`):   29.53 secs
  * gchecksum (`-n 8`):   20.57 secs

* Medium Files
  * xxh128sum:            30.26 secs
  * gchecksum (`-n 1`):   28.85 secs
  * gchecksum (`-n 4`):   19.87 secs
  * gchecksum (`-n 8`):   19.88 secs

* Large Files
  * xxh128sum:           208.19 secs 
  * gchecksum (`-n 1`):  192.99 secs
  * gchecksum (`-n 4`):  126.74 secs
  * gchecksum (`-n 8`):  126.60 secs

#### On NVMe SSD

* Small Files
  * xxh128sum:            39.04 secs
  * gchecksum (`-n 1`):   42.96 secs
  * gchecksum (`-n 4`):   22.26 secs
  * gchecksum (`-n 8`):   32.14 secs

* Medium Files
  * xxh128sum:             8.91 secs
  * gchecksum (`-n 1`):    8.97 secs
  * gchecksum (`-n 4`):    6.01 secs
  * gchecksum (`-n 8`):    5.42 secs

* Large Files
  * xxh128sum :           51.80 secs
  * gchecksum (`-n 1`):   54.69 secs
  * gchecksum (`-n 4`):   34.19 secs
  * gchecksum (`-n 8`):   33.79 secs
