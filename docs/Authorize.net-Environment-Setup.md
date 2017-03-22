# Authorize.net Environment Setup

## Prerequisites

- Users must establish their own test account with Authorize.net in order to use the BroadleafCommerce Authorize.net payment functionality. This can be done here: https://developer.authorize.net/testaccount

### Configure your Authorize.net Account*
1. Login to your Authorize.net account (https://account.authorize.net/ or https://sandbox.authorize.net/) and navigate to your Account Settings.
![Authorize.net Console](payment-authorizenet-console-1.png)
2. Check your email to find the default Secret Answer that will be asigned to you. You can change this by going into Account > User Profile > Change Security Question and Answer
3. Create a new Transaction Key by navigating to Account > Settings > API Login ID and Transaction Key. Fill out the form to generate a new key
![Authorize.net Console](payment-authorizenet-console-2.png)
4. Create an MD5Hash (e.g. 12345) by navigating to Account > Settings > MD5Hash
![Authorize.net Console](payment-authorizenet-console-3.png)
5. Obtain a Client Token Key to use Accept.js 
https://developer.authorize.net/api/reference/features/acceptjs.html

> * Note all of these values down as you will need to enter them in your environment properties files.

```xml
<!-- Authorize.net Dependencies -->
<dependency>
    <groupId>org.broadleafcommerce</groupId>
    <artifactId>broadleaf-authorizenet</artifactId>
    <version>2.5.x-GA</version>
    <type>jar</type>
    <scope>compile</scope>
</dependency>
```
Make sure to include the dependency in your CORE pom.xml as well:

```xml
<!-- Authorize.net Dependencies -->
<dependency>
    <groupId>org.broadleafcommerce</groupId>
    <artifactId>broadleaf-authorizenet</artifactId>
</dependency>
```
You should now begin to setup your environment to work with Broadleaf Commerce Authorize.net support. 
The first step is to make Broadleaf Commerce aware of your Authorize.net account credentials. 
This is accomplished through environment configuration (see [[Runtime Environment Configuration]]).

Broadleaf allows you to create your own property files per environment (e.g. common.properties, local.properties, development.properties, integrationdev.properties, integrationqa.properties, staging.properties, and production.properties) 
You will need to enter the following key/value pairs in the appropriate locations:

### common.properties
    gateway.authorizenet.transactionVersion=3.1

### local.properties, development.properties, integrationdev.properties, integrationqa.properties
    gateway.authorizenet.loginId=?
    gateway.authorizenet.clientKey=?
    gateway.authorizenet.transactionKey=?
    gateway.authorizenet.merchantMd5Key=?
    gateway.authorizenet.serverUrl=https://test.authorize.net/gateway/transact.dll
    
### staging.properties
    gateway.authorizenet.loginId=?
    gateway.authorizenet.clientKey=?
    gateway.authorizenet.transactionKey=?
    gateway.authorizenet.merchantMd5Key=?
    gateway.authorizenet.serverUrl=https://secure2.authorize.net/gateway/transact.dll

### production.properties 
    gateway.authorizenet.loginId=?
    gateway.authorizenet.clientKey=?
    gateway.authorizenet.transactionKey=?
    gateway.authorizenet.merchantMd5Key=?   
    gateway.authorizenet.serverUrl=https://secure2.authorize.net/gateway/transact.dll

Now that you have your environment set up, let's begin setting up the [[Authorize.net Quick Start]].
