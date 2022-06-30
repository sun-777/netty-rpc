**代码实现逻辑清晰、简单高效，代码逻辑不够直观的地方，均有注释帮助理解。**
**通过合理的设置超时时间参数，可以支撑一定并发数量的请求、响应。代码使用JMeter完成并发测试，运行稳定。**


## 一、Netty RPC 框架功能说明

- 代码极力避免硬编码；常量字段集中在Constants 接口类；需求多变的参数，在application.yaml 中自定义了配置。
- 定义Id 接口类，实现类是ObjectId，目的是生成request id，唯一标识请求及对应的响应，实现基于request id 的消息异步处理。
- 定义了消息通信协议（见ExchangeCodec 类头部注释），实现了通信协议的编码、解码。实现跨平台的序例化/反序列化。
- 定义了@RpcService、@RpcServiceInterface 注解，分别标记Rpc 服务实现类、Rpc 服务接口类； 也定义了@RpcServiceScan、@RpcServiceInterfaceScan 注解，
  分别实现动态扫描被@RpcService、@RpcServiceInterface 注解的类，实现Rpc 接口类的动态代理和自动Bean注册。
- 客户端为接口生成代理访问类，并纳入 Spring 容器管理。
- 使用Netty（基于NIO）替代BIO 实现网络传输，同时使用了Netty提供的通信链路心跳检测功能。
- 使用开源的序列化机制Protobuff（也可用其他的如：JSON、Kyro等）实现序RPC 框架中的列化接口Serializer，替代JDK 自带的序列化
  机制，实现请求及其响应的高效跨平台的序列化、反序列化。
- 使用 CompletableFuture 包装服务端响应结果，实现客户端发送请求、接收请求的业务逻辑解耦。
- 服务端接收请求后，响应请求处理逻辑，使用了独立的工作线程池（实现了异步非阻塞返回处理结果），极大的提高了服务端并发请求处理能力。
- 客户端、服务端的请求、响应超时处理实现（通过CompletableFuture 实现）



## 二、可优化说明

- 使用注册中心（可使用Zookeeper，Nacos等）
- Client模块加入负载均衡
