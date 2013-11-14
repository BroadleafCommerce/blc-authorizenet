# Authorize.net Environment Setup

## Prerequisites

- Users must establish their own test account with Authorize.net in order to use the BroadleafCommerce Authorize.net payment functionality. This can be done here: https://developer.authorize.net/testaccount
- Please familiarize yourself with the Direct Post Method of the Authorize.net API before proceeding: https://developer.authorize.net/integration/fifteenminutes/java

### Configure your Authorize.net Account*
1. Login to your Authorize.net account (https://account.authorize.net/ or https://sandbox.authorize.net/) and navigate to your Account Settings.
![Authorize.net Console](payment-authorizenet-console-1.png)
2. Create a new Transaction Key by navigating to Account > Settings > API Login ID and Transaction Key. Fill out the form to generate a new key
![Authorize.net Console](payment-authorizenet-console-2.png)
3. Create an MD5Hash (e.g. 12345) by navigating to Account > Settings > MD5Hash
![Authorize.net Console](payment-authorizenet-console-3.png)
4. Finally, configure the Relay Response URL by navigating to Account > Settings > Response/Receipt URLs. This is the public URL that Authorize.net will send the response of the transaction to.
![Authorize.net Console](payment-authorizenet-console-4.png)

> * Note all of these values down as you will need to enter them in your environment properties files.

### Download and Install the Authorize.net Java SDK
The Authorize.net SDK is not available on any public maven repository, so you must download and install it manually.
You can do that here: https://developer.authorize.net/downloads/

Once downloaded, you can install this dependency into your local or shared maven repository
```java
mvn install:install-file -Dfile=anet-java-sdk-1.4.6.jar -DgroupId=net.authorize -DartifactId=anet-java-sdk -Dversion=1.4.6 -Dpackaging=jar
```
Alternatively, you can install it in a nexus proxy that the local development build is configured to point to.

Next, include both the Authorize.net Java SDK and the Broadleaf Authorize.net Module dependency to your pom.xml.

```xml
<!-- Authorize.net Dependencies -->
<dependency>
    <groupId>net.authorize</groupId>
    <artifactId>anet-java-sdk</artifactId>
    <version>1.4.6</version>
    <type>jar</type>
    <scope>compile</scope>
</dependency>
<dependency>
    <groupId>org.broadleafcommerce</groupId>
    <artifactId>broadleaf-authorizenet</artifactId>
    <version>${blc.thirdparty.version}</version>
    <type>jar</type>
    <scope>compile</scope>
</dependency>
```
Make sure to include the dependency in your site pom.xml as well:

```xml
<!-- Authorize.net Dependencies -->
<dependency>
    <groupId>net.authorize</groupId>
    <artifactId>anet-java-sdk</artifactId>
</dependency>
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
    authorizenet.merchant.transaction.version=3.1

### local.properties, development.properties, integrationdev.properties, integrationqa.properties
    authorizenet.api.login.id=?
    authorizenet.transaction.key=?
    authorizenet.merchant.md5.key=?   
    authorizenet.relay.response.url=? (e.g. http://localhost:8080/authorizenet/process)
    authorizenet.confirm.url=? (e.g. http://localhost:8080/confirmation)
    authorizenet.error.url=? (e.g. http://localhost:8080/authorizenet/error)
    authorizenet.server.url=https://test.authorize.net/gateway/transact.dll
    authorizenet.x_test_request=FALSE

- authorizenet.relay.response.url: must be a publicly accessible URL. See [[Testing using DPM | http://community.developer.authorize.net/t5/Integration-and-Testing/Direct-Post-in-a-development-environment-behind-a-firewall/td-p/8906]] for more details; This is must be the same value you entered in your account settings.
    
### staging.properties
    authorizenet.api.login.id=?
    authorizenet.transaction.key=?
    authorizenet.merchant.md5.key=?    
    authorizenet.relay.response.url=? (e.g. http://staging.mycompany.com/authorizenet/process)
    authorizenet.confirm.url=? (e.g. http://staging.mycompany.com/confirmation)
    authorizenet.error.url=? (e.g. http://staging.mycompany.com/authorizenet/error)
    authorizenet.server.url=https://secure.authorize.net/gateway/transact.dll
    authorizenet.x_test_request=TRUE   

- authorizenet.relay.response.url: must be a publicly accessible URL. See [[Testing using DPM | http://community.developer.authorize.net/t5/Integration-and-Testing/Direct-Post-in-a-development-environment-behind-a-firewall/td-p/8906]] for more details; This is must be the same value you entered in your account settings.
- authorizenet.x_text_request: Once the integration is successfully tested in the developer test environment,
the merchant’s Authorize.Net Payment Gateway API Login ID and Transaction Key can be plugged into the integration for testing against the live environment.
By including the x_test_request field with a value of “TRUE” in the HTML Form POST <INPUT TYPE="HIDDEN" NAME="x_test_request" VALUE="TRUE">

### production.properties 
    authorizenet.api.login.id=?
    authorizenet.transaction.key=?
    authorizenet.merchant.md5.key=?
    authorizenet.relay.response.url=? (e.g. http://mycompany.com/authorizenet/process)
    authorizenet.confirm.url=? (e.g. http://mycompany.com/confirmation)
    authorizenet.error.url=? (e.g. http://mycompany.com/authorizenet/error)
    authorizenet.server.url=https://secure.authorize.net/gateway/transact.dll
    authorizenet.x_test_request=FALSE 

- authorizenet.relay.response.url: must be a publicly accessible URL. See [[Testing using DPM | http://community.developer.authorize.net/t5/Integration-and-Testing/Direct-Post-in-a-development-environment-behind-a-firewall/td-p/8906]] for more details; This is must be the same value you entered in your account settings.
- authorizenet.x_test_request: Only needed for testing in a live environment, e.g. staging


Now that you have your environment set up, let's begin setting up the [[Authorize.net Module]].
