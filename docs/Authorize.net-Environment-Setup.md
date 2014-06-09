# Authorize.net Environment Setup

## Prerequisites

- Users must establish their own test account with Authorize.net in order to use the BroadleafCommerce Authorize.net payment functionality. This can be done here: https://developer.authorize.net/testaccount
- Please familiarize yourself with the Direct Post Method of the Authorize.net API before proceeding: https://developer.authorize.net/integration/fifteenminutes/java
- Must have a publicly accessible URL ending with /authorizenet/process for local testing environments.  We recommend using [Ngrok](https://ngrok.com/) to setup a temporary publicly accessible URL.  This will be the URL set on Step 5 below and in your gateway.authorizenet.responseUrl property of your development.properties file.

### Configure your Authorize.net Account*
1. Login to your Authorize.net account (https://account.authorize.net/ or https://sandbox.authorize.net/) and navigate to your Account Settings.
![Authorize.net Console](payment-authorizenet-console-1.png)
2. Check your email to find the default Secret Answer that will be asigned to you. You can change this by going into Account > User Profile > Change Security Question and Answer
3. Create a new Transaction Key by navigating to Account > Settings > API Login ID and Transaction Key. Fill out the form to generate a new key
![Authorize.net Console](payment-authorizenet-console-2.png)
4. Create an MD5Hash (e.g. 12345) by navigating to Account > Settings > MD5Hash
![Authorize.net Console](payment-authorizenet-console-3.png)
5. Finally, configure the Relay Response URL by navigating to Account > Settings > Response/Receipt URLs. This is the public URL that Authorize.net will send the response of the transaction to.
![Authorize.net Console](payment-authorizenet-console-4.png)

> * Note all of these values down as you will need to enter them in your environment properties files.

```xml
<!-- Authorize.net Dependencies -->
<dependency>
    <groupId>org.broadleafcommerce</groupId>
    <artifactId>broadleaf-authorizenet</artifactId>
    <version>2.5.0-GA</version>
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
    gateway.authorizenet.transactionKey=?
    gateway.authorizenet.merchantMd5Key=?   
    gateway.authorizenet.responseUrl=? (e.g. http://xxxxx.ngrok.com/authorizenet/process)
    gateway.authorizenet.confirmUrl=? (e.g. http://localhost:8080/authorizenet/return)
    gateway.authorizenet.errorUrl=? (e.g. http://localhost:8080/authorizenet/error)
    gateway.authorizenet.serverUrl=https://test.authorize.net/gateway/transact.dll
    gateway.authorizenet.xTestRequest=FALSE

- gateway.authorizenet.responseUrl: must be a publicly accessible URL. We recommend using [Ngrok](https://ngrok.com/) to setup a temporary publicly accessible URL.
    
### staging.properties
    gateway.authorizenet.loginId=?
    gateway.authorizenet.transactionKey=?
    gateway.authorizenet.merchantMd5Key=?   
    gateway.authorizenet.responseUrl=? (e.g. http://staging.mycompany.com/authorizenet/process)
    gateway.authorizenet.confirmUrl=? (e.g. http://staging.mycompany.com/authorizenet/return)
    gateway.authorizenet.errorUrl=? (e.g. http://staging.mycompany.com/authorizenet/error)
    gateway.authorizenet.serverUrl=https://secure.authorize.net/gateway/transact.dll
    gateway.authorizenet.xTestRequest=TRUE

- gateway.authorizenet.responseUrl: must be a publicly accessible URL. 
- gateway.authorizenet.xTestRequest: Once the integration is successfully tested in the developer test environment,
the merchant’s Authorize.Net Payment Gateway API Login ID and Transaction Key can be plugged into the integration for testing against the live environment.
By including the x_test_request field with a value of “TRUE” in the HTML Form POST <INPUT TYPE="HIDDEN" NAME="x_test_request" VALUE="TRUE">

### production.properties 
    gateway.authorizenet.loginId=?
    gateway.authorizenet.transactionKey=?
    gateway.authorizenet.merchantMd5Key=?   
    gateway.authorizenet.responseUrl=? (e.g. http://mycompany.com/authorizenet/process)
    gateway.authorizenet.confirmUrl=? (e.g. http://mycompany.com/authorizenet/return)
    gateway.authorizenet.errorUrl=? (e.g. http://mycompany.com/authorizenet/error)
    gateway.authorizenet.serverUrl=https://secure.authorize.net/gateway/transact.dll
    gateway.authorizenet.xTestRequest=FALSE

- gateway.authorizenet.responseUrl: must be a publicly accessible URL. 
- gateway.authorizenet.xTestRequest: Only needed for testing in a live environment, e.g. staging


Now that you have your environment set up, let's begin setting up the [[Authorize.net Quick Start]].
