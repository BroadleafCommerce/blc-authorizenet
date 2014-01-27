# Authorize.net Module

Broadleaf Commerce currently offers integration with Authorize.net. This module uses DPM and AIM methods.

## How DPM Works
DPM integration outlined here: http://developer.authorize.net/api/dpm/

![Authorize.net DPM Diagram](payment-authorizenet-diagram.png)

1. On the final checkout page, the customer fills out their credit card and billing information and hits submit. This form will POST directly to Authorize.net with the required information contained within hidden fields and regular form fields.
2. Authorize.net will then process an Authorize and Debit transaction against the billing and credit card information submitted. Authorize.net will then call a publicly available Broadleaf URL with the response to the transaction. 
****Important: This MUST be a publicly accessible URL in order for Authorize.net to call it ****
3. Broadleaf will process the response and return a response body. The return response is just an HTML page with a Javascript redirect or Meta Refresh to either a confirmation page or error page.
4. Authorize.net will take that response and relay it to the customer's browser.
5. Upon receiving that response, the customer's browser will redirect to the appropriate page.
6. Broadleaf will serve either a confirmation or error page based on the requested URL.


## How AIM Works
AIM integration outlined here: http://developer.authorize.net/api/aim/

![Authorize.net AIM Diagram](payment-authorizenet-aim-diagram.png)


To get started integrating the Authorize.net module into your web application, checkout [[Authorize.net Environment Setup].