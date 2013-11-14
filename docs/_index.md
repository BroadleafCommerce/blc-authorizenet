# Authorize.net Module

Broadleaf Commerce currently offers integration with Authorize.net. This module uses the DPM integration method outlined here:  [http://developer.authorize.net/api/dpm/](http://developer.authorize.net/api/dpm/)

![Authorize.net Diagram](payment-authorizenet-diagram.png)

## How It Works
1. On the final checkout page, the customer fills out their credit card and billing information and hits submit. This form will POST directly to Authorize.net with the required information contained within hidden fields and regular form fields.
2. Authorize.net will then process an Authorize and Debit transaction against the billing and credit card information submitted. Authorize.net will then call a publicly available Broadleaf URL with the response to the transaction. 
****Important: This MUST be a publicly accessible URL in order for Authorize.net to call it ****
3. Broadleaf will process the response and return a response body. The return response is just an HTML page with a Javascript redirect or Meta Refresh to either a confirmation page or error page.
4. Authorize.net will take that response and relay it to the customer's browser.
5. Upon receiving that response, the customer's browser will redirect to the appropriate page.
6. Broadleaf will serve either a confirmation or error page based on the requested URL.

There are two ways to get started integrating the Authorize.net module into your web application. Broadleaf offers an [[Authorize.net Quick Start]] solution that allows developers to easily add Authorize.net functionality with little configuration. If you have a complex payment workflow, please take a look at our [[Authorize.net Advance Configuration]]
