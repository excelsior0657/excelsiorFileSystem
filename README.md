### 模块说明：
**Client:** 与应用直接交互的服务，提供文件操作相关接口

**Meta：** 维持文件元数据，统一管理chunk信息

**chunk-server：** 实现文件存储的服务器

本系统采用Google的GFS的架构思想，按照一个元数据中心，多个分片服务，多个客户端进行设计

### 项目所用技术栈：（作为手写分布式存储系统，固然少使用现成的框架与技术，尽可能采用手写的形式来完成系统开发）
1. SpringBoot

2. SpringData

3. SpringSchedule

4. MongDB

### 功能架构：
1. 项目工程化

2. 自定义服务注册与发现

3. 大文件的快速上传与下载

4. 文件服务器容错保障

### 服务注册设计：
Meta元数据中心兼任注册中心一职，chunk-server需要在启动后向Meta中心注册，以便Meta中心进行文件分片存储位置分配

chunk-server定期发送心跳（server info）到Meta服务，以确保chunk-server可用。Meta定期检查chunk-server是否存活。如果Meta检查到chunk-server上次发送心跳与此时时间间隔过大，则认为该chunk-server已经宕机

#### 元数据设计：

1. 文件名

2. 文件后缀名

3. 文件大小

4. 文件存储桶

5. 文件分片数量

6. 每个chunk的详细信息

* 分片序号

* 分片存储桶

* 分片起始位置

* 分片文件大小

* 分片文件后缀

* 分片存储地址

* 分片文件的md5值

#### 上传流程：

1. 用户端直接和Client交互，用户提供文件的基本信息提交给Client，client在发送请求给Meta。

2. Meta根据此信息生成meta元数据下发给Client，Client整理出分片的序号、起始位置和分片大小，Client返回一个chunk列表。

3. 用户根据chunk信息，可以向Client上传文件分片，Client转发给chunk-server完成分片文件上传。

4. 从用户端上传到Client时需要做md5校验以确保文件完整性。chunk-server上传完成后，同样下发一个md5给用户端，并返回上传成功信息。用户端再次校验md5值确保文件完整。


#### 下载流程：
1. 用户通过下载链接发送下载请求到Client，Client根据链接解析出文件id，然后再从Meta中解析出文件元信息。

2. Meta把元数据返回给客户端

3. Client根据文件信息请求不同的chunk-server得到每一个分片文件，最终到Client中奖分片进行拼装得到一个完整的文件，返回给用户端。

