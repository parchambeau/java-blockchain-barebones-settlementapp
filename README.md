# Java Barebones Settlement App #

## Getting Started ##
A sample use-case of a application that can make Settlement requests. To get started you should take the following steps

 * Install protoc on your workstation (Google's protocol buffer compiler)
 * Clone this repository
 * Edit the message.proto file and assign your APP_ID for value of package
 * Goto Mastercard Developers and create a Mastercard Blockchain project (note this is currently a private API and you may need to request access). You will be taken through the wizard to create a node. You must provide an APP_ID and a protocol buffer definition i.e. message.proto.
 * You will receive a p12 file and a consumer key from Mastercard Developers for your project.
 * Execute the following commands
```bash
mvn package
java -jar target/java-blockchain-barebones-settlementapp-0.0.1-SNAPSHOT-jar-with-dependencies -kp <path to p12> -ck <your consumer key>
```

When started it gets you to confirm your parameters and then displays a simple menu. 

## Menu ##
```
============ MENU ============
1. Create node (optional, onetime)
2. Update protocol buffer definition
3. Create settlement request
4. Confirm settlement
5. Show Protocol Buffer Definition
6. Re-initialize API
7. Print Command Line Options
0. Quit
Option [0]: 
```

## More Commandline Options ##
```
============ COMMAND LINE OPTIONS ============
usage: java -jar <jarfile>
 -ck,--consumerKey <arg>    consumer key (mastercard developers)
 -ka,--keyAlias <arg>       key alias (mastercard developers)
 -kp,--keystorePath <arg>   the path to your keystore (mastercard
                            developers)
 -sp,--storePass <arg>      keystore password (mastercard developers)
 -v,--verbosity             log mastercard developers sdk to console
```

## Useful Info ##
This project makes use of the Mastercard Blockchain SDK available from mvn.

```xml
<dependency>
	<groupId>com.mastercard.api</groupId>
	<artifactId>blockchain</artifactId>
	<version>0.0.2</version>
</dependency>

```
