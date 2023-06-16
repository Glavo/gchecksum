# gchecksum

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

gchecksum is an easy-to-use and high-performance file hashing tool.

It can quickly calculate the hash value of all files in directories and generate a checksums file, 
and it can also verify the entire directories based on the checksums file.

[Click here](#Benchmark) to see the performance test results comparing gchecksum with shasum and xxhsum.

Usage:
```
# Create checksums file
gchecksum create # or 'gchecksum c'

# Verify files by checksums file
gchecksum verify # or 'gchecksum v'

# Update the existing checksums file
gchecksum update # or 'gchecksum u'
```

Help message:
```
Usage:
    gchecksum c(reate) [options]    : Create checksums file
    gchecksum v(erify) [options]    : Verify files using checksums file
    gchecksum u(pdate) [options]    : Update the existing checksums file and print the changes

Options:
    -h -? --help            Print this help message
    -v --version            Print version information
    -f <checksums file>     Specify the checksums file [default=checksums.txt]
    -d <directory>          Specify the directory that will be validated [default=.]
    -y --yes --assume-yes   Overwrite the existing checksums file silently
    -a --algorithm <algorithm>
                            Specify the hash algorithm to be used
    -n --num-threads <num threads>
                            Specify the number of threads used for validation [default=4]
```

## Install

gchecksum is written in Java 11, you can download the jar file from the [GitHub Release page](https://github.com/Glavo/gchecksum/releases)
and execute `java -jar <jar-path>`.

### Linux

gchecksum provides executable script for Linux, you can install it with the following command:

```shell
sudo sh -c '(echo "#!/usr/bin/env sh" && curl -L https://github.com/Glavo/gchecksum/releases/download/0.13.0/gchecksum-0.13.0.sh) > /usr/local/bin/gchecksum && chmod +x /usr/local/bin/gchecksum'
```

If you don't have Java installed, you can use the following native-image instead:

```shell
sudo sh -c '(echo "#!/usr/bin/env sh" && curl -L https://github.com/Glavo/gchecksum/releases/download/0.13.0/gchecksum-0.13.0) > /usr/local/bin/gchecksum && chmod +x /usr/local/bin/gchecksum'
```

Note: native-image only supports the x86-64 platform and is slower than the Java version. If you can install Java, don't use it.


### Windows

If you don't have Java installed, you can use the following native-image instead of jar:

* [gchecksum-0.13.0.exe](https://github.com/Glavo/gchecksum/releases/download/0.13.0/gchecksum-0.13.0.exe)

Note: native-image only supports the x86-64 platform and is slower than the Java version. If you can install Java, don't use it.

## Introduce

gchecksum supports three modes:

* Create Mode: Create a checksums file, which records the hash values of all files;
* Verify Mode: Checks that the hash of files matches the record in the checksums file;
* Update Mode: Update the existing checksums file and print all changed files.

The mode name should be passed as the first option to gchecksum.

If no mode is specified, gchecksum will choose the default mode: `verify`.

The `-f` option is used to specify the path to the checksums file.
The default is the `checksums.txt` file in the current directory.
If the passed filename is `-`, then gchecksum will use the standard input/output stream instead of the checksums file.

The `-d` option is used to specify the directory to process. The default is the current directory.

The `-a` option is used to specify the hash algorithm to use.
If this option is not present:

* In the `create` or `update` mode, gchecksum will choose SHA-256;
* In the `verify` mode, gchecksum will guess the hash algorithm based on the first line of the checksums file.

Supported hash algorithms:

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
* xxHash (Experimental)
  * xxHash64
  * xxHash128

The algorithms that can be automatically detected in the `verify` mode:

* MD5
* SHA-1
* SHA-224
* SHA-256
* SHA-384
* SHA-512

If the hashes in the checksums file are calculated with another algorithm, use the `-a` option to specify the algorithm.

The `--num-threads`(`-n`) option is used to specify the number of threads to read files.
The default is 4, if you use gchecksum on hard disk drive, please specify it as 1.

## checksums file

gchecksum uses BSD-style checksums files, which is compatible with commands like `shasum`/`xxhsum` on Linux.

For example, if you generated the checksums file using the `gchecksum create -a SHA-512 -f checksums.txt`, 
then you can use `sha512sum -c checksums.txt` for validation.


A sample checksums file:
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

## Benchmark

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

I used three sets of sample files to test the situation in different scenarios:

* Large Files:        8 * 8GiB (= 64GiB)
* Medium Files:   10240 * 1MiB (= 10GiB)
* Small files:  1048576 * 1KiB (=  1GiB)

All sample files are randomly generated by [RandomFileGenerator 0.2.0](https://github.com/Glavo/RandomFileGenerator) (Options: `-e 0 -n <number of files> -s <size of file> -o file-%d.bin`).

And I also ran benchmarks on RAM Disk, SATA SSD and NVMe SSD to cover more scenarios.

When running on SSD, I perform `sync && sudo bash -c "echo 3 > /proc/sys/vm/drop_caches"` to clean the page cache to simulate reading cold data before each benchmark run.

### Large files

Sample files: 8 * 8GiB (= 64GiB)

SHA-256:

|                               | SATA SSD          | NVMe SSD                     |
|-------------------------------|-------------------|------------------------------|
| sha256sum                     | 247.34s           | 246.47s                      |
| gchecksum (`--num-threads 1`) | 193.12s (-21.92%) | &nbsp;&nbsp;61.96s (-74.86%) |
| gchecksum (`--num-threads 4`) | 126.37s (-48.91%) | &nbsp;&nbsp;34.54s (-85.99%) |
| gchecksum (`--num-threads 8`) | 126.36s (-48.91%) | &nbsp;&nbsp;33.70s (-86.33%) |


SHA-512:

|                               | SATA SSD          | NVMe SSD                     |
|-------------------------------|-------------------|------------------------------|
| sha512sum                     | 197.97s           | 174.12s                      |
| gchecksum (`--num-threads 1`) | 205.64s (+ 3.87%) | 118.13s (-32.16%)            |
| gchecksum (`--num-threads 4`) | 127.03s (-35.83%) | &nbsp;&nbsp;40.20s (-76.91%) |
| gchecksum (`--num-threads 8`) | 129.60s (-34.54%) | &nbsp;&nbsp;34.13s (-80.40%) |


XXH64:

|                               | SATA SSD          | NVMe SSD         |
|-------------------------------|-------------------|------------------|
| xxh64sum                      | 212.45s           | 51.87s           |
| gchecksum (`--num-threads 1`) | 192.59s (- 9.35%) | 56.08s (+ 8.12%) |
| gchecksum (`--num-threads 4`) | 126.89s (-40.27%) | 34.13s (-34.20%) |
| gchecksum (`--num-threads 8`) | 126.45s (-40.48%) | 33.73s (-34.97%) |


XXH128:

|                               | SATA SSD          | NVMe SSD         |
|-------------------------------|-------------------|------------------|
| xxh128sum                     | 208.19s           | 51.80s           |
| gchecksum (`--num-threads 1`) | 192.99s (- 7.30%) | 54.69s (+ 5.58%) |
| gchecksum (`--num-threads 4`) | 126.74s (-39.12%) | 34.19s (-34.00%) |
| gchecksum (`--num-threads 8`) | 126.60s (-39.19%) | 33.79s (-34.00%) |

### Medium Files

Sample files: 10240 * 1MiB (= 10GiB)

SHA-256:

|                               | SATA SSD         | NVMe SSD                    | RAM Disk                    |
|-------------------------------|------------------|-----------------------------|-----------------------------|
| sha256sum                     | 44.57s           | 39.60s                      | 36.84s                      |
| gchecksum (`--num-threads 1`) | 29.25s (-34.37%) | 11.95s (-69.82%)            | &nbsp;&nbsp;6.91s (-81.24%) |
| gchecksum (`--num-threads 4`) | 19.74s (-55.71%) | &nbsp;&nbsp;5.79s (-85.38%) | &nbsp;&nbsp;1.83s (-95.03%) |
| gchecksum (`--num-threads 8`) | 19.75s (-55.69%) | &nbsp;&nbsp;5.31s (-86.59%) | &nbsp;&nbsp;1.01s (-97.26%) |


SHA-512:

|                               | SATA SSD         | NVMe SSD                    | RAM Disk                    |
|-------------------------------|------------------|-----------------------------|-----------------------------|
| sha512sum                     | 36.03s           | 28.28s                      | 25.48s                      |
| gchecksum (`--num-threads 1`) | 33.94s (-5.80%)  | 20.22s (-43.88%)            | 15.68s (-38.46%)            |
| gchecksum (`--num-threads 4`) | 20.01s (-44.46%) | &nbsp;&nbsp;7.12s (-80.24%) | &nbsp;&nbsp;4.05s (-84.11%) |
| gchecksum (`--num-threads 8`) | 19.83s (-44.96%) | &nbsp;&nbsp;5.38s (-85.07%) | &nbsp;&nbsp;2.13s (-91.64%) |


XXH64:

|                               | SATA SSD         | NVMe SSD        | RAM Disk        |
|-------------------------------|------------------|-----------------|-----------------|
| xxh64sum                      | 30.53s           | 9.00s           | 1.50s           |
| gchecksum (`--num-threads 1`) | 28.81s (-5.63%)  | 8.76s (- 2.68%) | 2.27s (+51.33%) |
| gchecksum (`--num-threads 4`) | 19.75s (-35.31%) | 5.63s (-37.44%) | 0.73s (-51.33%) |
| gchecksum (`--num-threads 8`) | 19.72s (-35.41%) | 5.30s (-41.11%) | 0.60s (-60.00%) |


XXH128:

|                               | SATA SSD         | NVMe SSD        | RAM Disk         |
|-------------------------------|------------------|-----------------|------------------|
| xxh128sum                     | 30.26s           | 8.91s           | 1.01s            |
| gchecksum (`--num-threads 1`) | 28.85s (- 4.66%) | 8.97s (+ 0.67%) | 2.97s (+194.06%) |
| gchecksum (`--num-threads 4`) | 19.87s (-34.33%) | 6.01s (-32.55%) | 1.14s (+ 12.87%) |
| gchecksum (`--num-threads 8`) | 19.88s (-34.30%) | 5.42s (-39.17%) | 0.83s (- 17.82%) |


### Small files

Sample files: 1048576 * 1KiB (=  1GiB)


SHA-256:

|                               | SATA SSD         | NVMe SSD         | RAM Disk        |
|-------------------------------|------------------|------------------|-----------------|
| sha256sum                     | 79.89s           | 41.70s           | 7.36s           |
| gchecksum (`--num-threads 1`) | 81.69s (+2.25%)  | 42.12s (+1.01%)  | 7.33s (- 0.41%) |
| gchecksum (`--num-threads 4`) | 29.75s (-62.76%) | 20.77s (-50.19%) | 3.20s (-56.52%) |
| gchecksum (`--num-threads 8`) | 20.85s (-73.90%) | 30.75s (-26.26%) | 2.23s (-69.70%) |


SHA-512:

|                               | SATA SSD         | NVMe SSD         | RAM Disk        |
|-------------------------------|------------------|------------------|-----------------|
| sha512sum                     | 79.87s           | 41.57s           | 6.41s           |
| gchecksum (`--num-threads 1`) | 83.46s (+ 4.50%) | 44.34s (+ 6.66%) | 8.52s (+32.92%) |
| gchecksum (`--num-threads 4`) | 30.13s (-62.27%) | 25.73s (-38.10%) | 3.46s (-46.02%) |
| gchecksum (`--num-threads 8`) | 21.02s (-73.68%) | 29.18s (-29.81%) | 2.37s (-63.03%) |


XXH64:

|                               | SATA SSD         | NVMe SSD         | RAM Disk        |
|-------------------------------|------------------|------------------|-----------------|
| xxh64sum                      | 76.14s           | 38.60s           | 3.53s           |
| gchecksum (`--num-threads 1`) | 79.76s (+ 4.75%) | 42.50s (10.10%)  | 6.65s (+88.39%) |
| gchecksum (`--num-threads 4`) | 29.49s (-61.27%) | 22.81s (-40.91%) | 3.05s (-13.60%) |
| gchecksum (`--num-threads 8`) | 20.53s (-73.04%) | 29.99s (-22.31%) | 2.15s (-39.09%) |


XXH128:

|                               | SATA SSD         | NVMe SSD         | RAM Disk        |
|-------------------------------|------------------|------------------|-----------------|
| xxh128sum                     | 76.44s           | 39.04s           | 3.63s           |
| gchecksum (`--num-threads 1`) | 79.39s (+ 3.86%) | 42.96s (+10.04%) | 6.65s (+83.20%) |
| gchecksum (`--num-threads 4`) | 29.53s (-61.37%) | 22.26s (-42.98%) | 2.82s (-22.31%) |
| gchecksum (`--num-threads 8`) | 20.57s (-73.09%) | 32.14s (-17.67%) | 2.21s (-39.12%) |
