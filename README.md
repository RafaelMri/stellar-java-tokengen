# stellar-java-tokengen

## Description

Application to automatically generate a token.

## Versions:

v0.1 provides token generation for testnet only

## Important info:

Important step before running:

* Copy src/main/resources/template.properties to src/main/resources/ico.properties
* Change values in ico.properties to fill the needs
* No Windows support yet
* You will need at least to have ipfs install: https://ipfs.io/docs/install/

## Build and run

```
cp src/main/resources/template.properties src/main/resources/ico.properties
./gradlew clean
./gradlew create
java -jar build/libs/stellar-tokengen-all-0.1.jar
```

## Sample output

```
Token and Account creation started for TESTNET
ISSUER created with: 
    Public Key : GDF4JLU7TB74EZ6RFKJR4MLRJVZBXC5NCVTRRVBOP6U3HBCSIK2OYGS3
    Secret Seed: SAN7FVXEROA5LKQXOQKBJEERUZSKWATTORV6E23MARFMT4F7DL6BLTDC
DISTRIBUTOR created with: 
    Public Key : GDPEUYI2QV5Z7XIKUBJCDJBPUUVJD4L6D47W5RXLG52YWIIRXJLRUTTB
    Secret Seed: SAXMCPKQYMW7MNWWOGC7CZDWQQIRSANIBJTQS5JXRZWBVV65TOZEIMNR
IPFS location: https://ipfs.io/ipfs/QmaiwQ3SkXxppjb6nwVarLUNxAKAFQwpUMN8nopPftn4cg
Account locked. Take a look at https://horizon-testnet.stellar.org/accounts/GDF4JLU7TB74EZ6RFKJR4MLRJVZBXC5NCVTRRVBOP6U3HBCSIK2OYGS3
---------------------------------
Token created.
First open https://stellarterm.com/#testnet and open then in the same tab the following URL:
You can now trade it on https://stellarterm.com/#exchange/AppToken-GDF4JLU7TB74EZ6RFKJR4MLRJVZBXC5NCVTRRVBOP6U3HBCSIK2OYGS3/XLM-native
USER created with: 
    Public Key : GATRTYLC2VSNMSG6KZZGIVOZEFGSXF7CKIXFZCE5J3ZDQGRQRIBRPJAH
    Secret Seed: SDVGOAK37NZN3XVY42YSYHFL4TKY4KTGTOCTTUENHIEY53RCX5DH4KBV
Checkout my balance: https://horizon-testnet.stellar.org/accounts/GATRTYLC2VSNMSG6KZZGIVOZEFGSXF7CKIXFZCE5J3ZDQGRQRIBRPJAH
```

## Donations
If you like the code, a donation would be appreciated. Even a single XLM!

```
XLM: GB7OE7PTL5FKRW6WXFGSDDOA7WR4HT6LPMKCQAWK7QXLSMCQWIKYTTY5
```
