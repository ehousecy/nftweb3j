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