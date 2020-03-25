# Authorize.net Apple Pay Environment Setup

Broadleaf uses Apple Pay JS framework to work with Apple Pay. You should have Apple Developer Program account.

**You must have completed the [[Authorize.net Environment Setup]] before continuing**

1. To use Apple Pay to submit payment transactions, follow the processes https://developer.authorize.net/api/reference/features/in-app.html#Apple_Pay.

2. Follow the steps to obtain your Apple Pay Merchant ID, Payment Processing Certificate, Domain Verification file and Merchant Identity Certificate if you do not already have them: https://developer.apple.com/documentation/apple_pay_on_the_web/configuring_your_environment

3. Place the domain verification file (apple-developer-merchantid-domain-association) in the site/src/main/resources/well-known folder.

4. After application started you will be able to complete domain verification.

5. Download Apple Pay Merchant Identity (certificate) - a file called merchant_id.cer.

6. Double-click it to insert it into keychain access.app. This should automatically get appended to the existing entry for your Private key in keychain access.app.

7. Right-click that certificate (probably named "Merchant ID: merchant...." from within keychain access.app (you may need to expand the private key entry to see the certificate under it) and select "Export 'Merchant ID merchant....' ". 
This will default to exporting a xxxx.p12 file to your desktop.

8. Place xxxx.p12 file in classpath and specify it's name in `gateway.authorizenet.apple.keyStoreFilePath` property.

9. Remember keychain password for certificate and specify it in `gateway.authorizenet.apple.keyStorePassword` property.

10. Specify configuration properties to work with Apple Pay.

Broadleaf allows you to create your own property files per environment (e.g. common.properties, local.properties, development.properties, integrationdev.properties, integrationqa.properties, staging.properties, and production.properties) 
You will need to enter the following key/value pairs in the appropriate locations:

### common.properties
    gateway.authorizenet.apple.merchantId={Registered Merchant Id In Apple Developer Account}
    gateway.authorizenet.apple.keyStoreFilePath={xxxx.p12 File Name}
    gateway.authorizenet.apple.keyStorePassword={Password For xxxx.p12 File}
    gateway.authorizenet.apple.verifiedDomainName={Verified Domain In Apple Developer Account}
    gateway.authorizenet.apple.verifiedDomainDisplayName={Domain Display Name}

### local.properties, development.properties, integrationdev.properties, integrationqa.properties
    gateway.authorizenet.apple.merchantId={Registered Merchant Id In Apple Developer Account}
    gateway.authorizenet.apple.keyStoreFilePath={xxxx.p12 File Name}
    gateway.authorizenet.apple.keyStorePassword={Password For xxxx.p12 File}
    gateway.authorizenet.apple.verifiedDomainName={Verified Domain In Apple Developer Account}
    gateway.authorizenet.apple.verifiedDomainDisplayName={Domain Display Name}
    
### staging.properties
    gateway.authorizenet.apple.merchantId={Registered Merchant Id In Apple Developer Account}
    gateway.authorizenet.apple.keyStoreFilePath={xxxx.p12 File Name}
    gateway.authorizenet.apple.keyStorePassword={Password For xxxx.p12 File}
    gateway.authorizenet.apple.verifiedDomainName={Verified Domain In Apple Developer Account}
    gateway.authorizenet.apple.verifiedDomainDisplayName={Domain Display Name}

### production.properties 
    gateway.authorizenet.apple.merchantId={Registered Merchant Id In Apple Developer Account}
    gateway.authorizenet.apple.keyStoreFilePath={xxxx.p12 File Name}
    gateway.authorizenet.apple.keyStorePassword={Password For xxxx.p12 File}
    gateway.authorizenet.apple.verifiedDomainName={Verified Domain In Apple Developer Account}
    gateway.authorizenet.apple.verifiedDomainDisplayName={Domain Display Name}

Now that you have your environment set up, let's begin setting up the [[Authorize.net Apple Pay Quick Start]].