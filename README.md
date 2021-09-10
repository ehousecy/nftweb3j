# 开发工具

* [web3j](!https://docs.web3j.io/4.8.7/)
* [solc-select](!https://github.com/crytic/solc-select)
* [web3j-cli 命令行工具](!https://github.com/web3j/web3j-cli)

# 开发步骤
* solc-select
    * solc-select install "正确版本的solc编译器"
    * solc DocumentRegistry.sol --bin --abi --optimize -o ./
* web3j-cli 生成基于合约的 java wrapper
    * web3j generate solidity -b yourcontract.bin -a yourcontract.abi -o /path/to/your/generated/package -p your.package.name
* web3j
    * 直接调用生成的合约 java wrapper调用、部署合约，或者
    * 参考java wrapper数据组装方式调用、部署合约

# 合约地址

|链|合约地址|测试私钥|合约Owner（可根据测试私钥导出）|
|:---:|:---:|:---:|:---:|
|以太坊|0x2f80F28102f42368B56Bbd52e1EC6452EBf35069|0x6c7117111a42dd5dfcff752ee0b32c3f85699192d6c18297fc23d473bf8089c9|0x78C531c538A5013121E81BBA80cB9D3Bcee46962|
|conflux|cfxtest:acfwunb38ht2p2df53w4hb9vnfrr4vubxyng9zg2y6|0x3B57A00547A05511B0339FFBD9AA7A0AD2F6FD3D0B980372C22252F77D14D03D|cfxtest:aam66h08kb8acc4ea25661xpg1xbf2e8xy1a95pnju|

# 接口
* 初始化web3j

|参数|类型|备注|
|:---:|:---:|:---:|
|nodeAdd|String|节点地址|

```
Web3j web3j = Web3j.build(new HttpService(nodeAdd));
```

* 导入私钥

|参数|类型|备注|
|:---:|:---:|:---:|
|sk|String|私钥字符串|

```
credentials = Credentials.create(sk);
```
* 部署合约

|参数|类型|备注|
|:---:|:---:|:---:|
|gasPrice|BigInteger|每单位gas支付的费用|
|gasLimit|BigInteger|交易发送者愿意为该交易执行付出的最大gas|
|to|null|接收账户地址|
|contractData|String|编译过后的合约数据|
|value|BigInteger|交易发送的以太币|
```
String txHash = txManager.sendTransaction(gasPrice, gasLimit, null, contractData, value).getTransactionHash();
```

* 发送交易（伦敦分叉EIP-1559后的方式发交易，之后的合约交易依托此方法）

|参数|类型|备注|
|:---:|:---:|:---:|
|chainId|long|网络号|
|maxPriorityFeePerGas|BigInteger|愿意为单位 Gas 支付的最大优先价格，实际支付的gasPrice为min(MaxFeePerGas, MaxPriorityFeePerGas + BaseFee)|
|maxFeePerGas|BigInteger|愿意为单位 Gas 支付的最大价格，实际支付的gasPrice为min(MaxFeePerGas, MaxPriorityFeePerGas + BaseFee)|
|gasLimit|BigInteger|单笔交易最多愿意承担的gas|
|to|String|接收账户地址|
|data|String|编译过后的合约数据|
|value|BigInteger|交易发送的以太币数量|
```
String txHash = txManager.sendEIP1559Transaction(
        chainId,
        gasPrice,
        gasPrice,
        gasLimit,
        "0x2f80F28102f42368B56Bbd52e1EC6452EBf35069",
        txData,
        BigInteger.ZERO).getTransactionHash();
System.out.println(txHash);
```

* mintArtWorkToken（伦敦分叉EIP-1559后的方式发交易）

|参数|类型|备注|
|:---:|:---:|:---:|
|chainId|long|网络号|
|maxPriorityFeePerGas|BigInteger|愿意为单位 Gas 支付的最大优先价格，实际支付的gasPrice为min(MaxFeePerGas, MaxPriorityFeePerGas + BaseFee)|
|maxFeePerGas|BigInteger|愿意为单位 Gas 支付的最大价格，实际支付的gasPrice为min(MaxFeePerGas, MaxPriorityFeePerGas + BaseFee)|
|gasLimit|BigInteger|单笔交易最多愿意承担的gas|
|to|String|接收账户地址|
|data|String|调用合约方法'mineArtWorkToken'的数据|
|value|BigInteger|交易发送的以太币数量|

```
// 构建交易data
DynamicStruct basicInfoArg = new DynamicStruct(
        new Utf8String("zzz"),
        new Utf8String("111"),
        new Utf8String("ZHH"),
        new Utf8String("lately"),
        new Utf8String("asdasd"),
        new Utf8String("morden"),

        new Utf8String("122"),
        new Utf8String("12"),
        new Utf8String("33"),
        new Utf8String("11"),
        new Utf8String("12"),
        new Utf8String("11")
);
// 合约方法第一个参数
DynamicStruct firstArg = new DynamicStruct(basicInfoArg, new Bool(true));

//[["https://sad","adasd"],["https://sad","adasd"]]
DynamicStruct image1 = new DynamicStruct(
        new Utf8String("https://sad"),
        new Utf8String("adasd")
);
DynamicStruct image2 = new DynamicStruct(
        new Utf8String("https://sad"),
        new Utf8String("adasd")
);
// 合约方法第二个参数
DynamicArray secondArg =  new DynamicArray<DynamicStruct>(DynamicStruct.class, Arrays.asList(image1, image2));

Function function = new Function(
        "mintArtWorksToken",  // 合约方法名
        Arrays.<Type>asList(firstArg,
                secondArg,
                new Uint256(120)), // 合约方法第三个参数
        Collections.<TypeReference<?>>emptyList());
// 交易参考以上
。。。
```

* mineArtWorkToken2Owner（伦敦分叉EIP-1559后的方式发交易）

|参数|类型|备注|
|:---:|:---:|:---:|
|chainId|long|网络号|
|maxPriorityFeePerGas|BigInteger|愿意为单位 Gas 支付的最大优先价格，实际支付的gasPrice为min(MaxFeePerGas, MaxPriorityFeePerGas + BaseFee)|
|maxFeePerGas|BigInteger|愿意为单位 Gas 支付的最大价格，实际支付的gasPrice为min(MaxFeePerGas, MaxPriorityFeePerGas + BaseFee)|
|gasLimit|BigInteger|单笔交易最多愿意承担的gas|
|to|String|接收账户地址|
|data|String|调用合约方法'mineArtWorkToken'的数据|
|value|BigInteger|交易发送的以太币数量|

```
// 构建交易data
DynamicStruct basicInfoArg = new DynamicStruct(
        new Utf8String("zzz"),
        new Utf8String("111"),
        new Utf8String("ZHH"),
        new Utf8String("lately"),
        new Utf8String("asdasd"),
        new Utf8String("morden"),

        new Utf8String("122"),
        new Utf8String("12"),
        new Utf8String("33"),
        new Utf8String("11"),
        new Utf8String("12"),
        new Utf8String("11")
);
// 合约方法第一个参数
DynamicStruct firstArg = new DynamicStruct(basicInfoArg, new Bool(true));

//[["https://sad","adasd"],["https://sad","adasd"]]
DynamicStruct image1 = new DynamicStruct(
        new Utf8String("https://sad"),
        new Utf8String("adasd")
);
DynamicStruct image2 = new DynamicStruct(
        new Utf8String("https://sad"),
        new Utf8String("adasd")
);
// 合约方法第二个参数
DynamicArray secondArg =  new DynamicArray<DynamicStruct>(DynamicStruct.class, Arrays.asList(image1, image2));

Function function = new Function(
        "mintArtWorksToken2Owner",  // 合约方法名
        Arrays.<Type>asList(firstArg,
                secondArg,
                new Uint256(120)), // 合约方法第三个参数
                new Address(160, "0xed774722ec7fbee84117d7da7856a1bc8a60b813")), // 合约方法第四个参数
        Collections.<TypeReference<?>>emptyList());
// 交易参考以上
。。。
```

* 查询日志

|参数|类型|备注|
|:---:|:---:|:---:|
|contractAdd|String|合约地址|
|fromBlock|实现DefaultBlockParameter接口|开始监控的块高|
|toBlock|实现DefaultBlockParameter接口|结束监控的块高|
|topic|String|合约事件哈希|

```
String topic = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
EthFilter filter = new EthFilter(DefaultBlockParameterName.EARLIEST,
        DefaultBlockParameterName.LATEST, "0x2f80F28102f42368B56Bbd52e1EC6452EBf35069")
        .addSingleTopic(topic);

web3j.ethLogFlowable(filter).subscribe(log -> {
    System.out.println(log);
    System.out.println(log.getTopics());
});
```