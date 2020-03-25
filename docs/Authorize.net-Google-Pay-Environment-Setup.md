# Authorize.net Google Pay Environment Setup

**You must have completed the [[Authorize.net Environment Setup]] before continuing**

To work with Google Pay, we need to specify additional property [[Gateway Merchant ID | https://support.authorize.net/s/article/What-Is-My-Payment-Gateway-ID]].

Broadleaf allows you to create your own property files per environment (e.g. common.properties, local.properties, development.properties, integrationdev.properties, integrationqa.properties, staging.properties, and production.properties) 
You will need to enter the following key/value pairs in the appropriate locations:

### common.properties
    gateway.authorizenet.gatewayMerchantId={Your Gateway Merchant Id}

### local.properties, development.properties, integrationdev.properties, integrationqa.properties
    gateway.authorizenet.gatewayMerchantId={Your Gateway Merchant Id}
    
### staging.properties
    gateway.authorizenet.gatewayMerchantId={Your Gateway Merchant Id}

### production.properties 
    gateway.authorizenet.gatewayMerchantId={Your Gateway Merchant Id}

Now that you have your environment set up, let's begin setting up the [[Authorize.net Quick Start Google Pay]].