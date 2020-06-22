# Authorize.net Apple Pay Quick Start

Broadleaf Commerce offers an out-of-the-box Authorize.net solution that requires little configuration and is easily set up. 
The quick start solution implements the [Apple Pay](https://developer.authorize.net/api/reference/features/in-app.html#Apple_Pay) model offered by Authorize.net.
This implementation should be useful for those with a simple checkout flow.

**You must have completed the [[Authorize.net Apple Pay Environment Setup]] before continuing**


## Adding Authorize.net Checkout Support

These instructions assume integration with the default Heat Clinic Demo Site provided with the framework.

1. Configure domain verification file path in SiteServletConfig:

```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("{wellKnownFolder:\\.well-known}/**")
        .addResourceLocations("classpath:/well-known/");
}
```

2. Modify the Payment Form on your checkout page to look something like:

```html
<div class="applePayButtonContainer">
    <a lang="us" class="applePayButton" style="-webkit-appearance: -apple-pay-button; -apple-pay-button-type:buy; -apple-pay-button-style: black; height: 30px; width: 150px;" onclick="javascript:startApplePaySession()" title="Start Apple Pay" role="link" tabindex="0"></a>
</div>

<blc:form id="anet_checkout_form" th:action="@{checkout/authnet/complete}" method="POST">
    <input type="hidden" name="OPAQUE_DATA_DESCRIPTOR" id="OPAQUE_DATA_DESCRIPTOR"/>
    <input type="hidden" name="OPAQUE_DATA_VALUE" id="OPAQUE_DATA_VALUE"/>
</blc:form>
```

3. Insert the following Javascript into your checkout html in order to generate the client token and retrieve the payment nonce:

```html
<script>
    document.addEventListener("DOMContentLoaded", function (event) {
        if (window.ApplePaySession) {
            var merchantIdentifier = [[${#authorizenet.getAppleMerchantId()}]];
            var promise = ApplePaySession.canMakePaymentsWithActiveCard(merchantIdentifier);
            promise.then(function (canMakePayments) {
                if (canMakePayments) {
                    $('.applePayButtonContainer').show();
                }
            });
        }
    });

    /**
     * Apple Pay Logic
     * Our entry point for Apple Pay interactions.
     * Triggered when the Apple Pay button is pressed
     */
    function startApplePaySession() {
        var supportsVersion = 3;
        var request = {
            "countryCode": "US",
            "currencyCode": "USD",
            "merchantCapabilities": [
                "supports3DS"
            ],
            "supportedNetworks": [
                "visa",
                "masterCard",
                "amex",
                "discover"
            ],
            "total": {
                "label": "Demo (Card is not charged)",
                "type": "final",
                "amount": "" + [[${cart.total}]]
            }
        };
        var session = new ApplePaySession(supportsVersion, request);

        session.begin();

        /**
         * Merchant Validation
         * We call our merchant session endpoint, passing the URL to use
         */
        session.onvalidatemerchant = function (event) {
            var validationURL = event.validationURL;

            getApplePaySession(validationURL).then(function (response) {
                session.completeMerchantValidation(response);
            });
        };

        session.onpaymentauthorized = function (event) {
            // Send payment for processing...
            var paymentData = event.payment.token.paymentData;
            processPayment(paymentData);

            var authorizationResult = {
                status: ApplePaySession.STATUS_SUCCESS,
                errors: []
            };

            session.completePayment(authorizationResult);
        };

        function processPayment(paymentData) {
            return new Promise(function(resolve, reject) {
                setTimeout(function() {
                    // @todo pass payment token to your gateway to process payment
                    useOpaqueData(paymentData);
                    resolve({});
                }, 3000);
            });
        }

        function useOpaqueData(paymentData) {
            var enc = window.btoa(JSON.stringify(paymentData));
            $("#OPAQUE_DATA_DESCRIPTOR").val('COMMON.APPLE.INAPP.PAYMENT');
            $("#OPAQUE_DATA_VALUE").val(enc);
            $("#anet_checkout_form").submit();
        }

         /**
         * Server to Server call to apple for response used to validate merchant
         */
        function getApplePaySession(url) {
            return new Promise(function (resolve, reject) {
                var xhr = new XMLHttpRequest();
                var token = $('input[name=csrfToken]').val();

                xhr.open('POST', '/checkout/authnet/validate/merchant?csrfToken=' + token);
                xhr.onload = function () {
                    if (this.status >= 200 && this.status < 300) {
                        resolve(JSON.parse(xhr.response));
                    } else {
                        reject({
                            status: this.status,
                            statusText: xhr.statusText
                        });
                    }
                };

                xhr.onerror = function () {
                    reject({
                        status: this.status,
                        statusText: xhr.statusText
                    });
                };

                xhr.send(url);
            });
        }
    }
</script>
```

4. Create a Spring MVC Controller to handle the form you created above in order to process the payment nonce and confirm the transaction. It would look something like this:

```java
@Controller
public class AuthorizeNetCheckoutController extends BroadleafCheckoutController {

    @Resource(name = "blAddressService")
    protected AddressService addressService;

    @Resource(name = "blAuthorizeNetConfiguration")
    protected AuthorizeNetConfiguration authorizeNetConfiguration;

    @RequestMapping(value = "/checkout/authnet/complete")
    public String completeAuthorizeNetCheckout(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes, @PathVariable Map<String, String> pathVars) throws PaymentException, PricingException {
        //Get Cart
        Order cart = CartState.getCart();
        //Get Payment Nonce From Request
        String dataDescriptor = request.getParameter("OPAQUE_DATA_DESCRIPTOR");
        String dataValue = request.getParameter("OPAQUE_DATA_VALUE");

        //Create a new PAYMENT_NONCE Order Payment
        OrderPayment paymentNonce = orderPaymentService.create();
        paymentNonce.setType(PaymentType.APPLE_PAY);
        paymentNonce.setPaymentGatewayType(AuthorizeNetGatewayType.AUTHORIZENET);
        paymentNonce.setAmount(cart.getTotalAfterAppliedPayments());
        paymentNonce.setOrder(cart);

        //Populate Billing Address per UI requirements
        //For this example, we'll copy the address from the temporary Credit Card's Billing address and archive the payment,
        // (since Heat Clinic's checkout template saves and validates the address in a previous section).
        OrderPayment tempPayment = null;
        for (OrderPayment payment : cart.getPayments()) {
            if (PaymentGatewayType.TEMPORARY.equals(payment.getGatewayType()) &&
                    PaymentType.CREDIT_CARD.equals(payment.getType())) {
                tempPayment = payment;
                break;
            }
        }

        if (tempPayment != null) {
            paymentNonce.setBillingAddress(addressService.copyAddress(tempPayment.getBillingAddress()));
            orderService.removePaymentFromOrder(cart, tempPayment);
        }

        // Create the UNCONFIRMED transaction for the payment
        PaymentTransaction transaction = orderPaymentService.createTransaction();
        transaction.setAmount(cart.getTotalAfterAppliedPayments());
        transaction.setRawResponse("Authorize.net Apple Payment Nonce");
        transaction.setSuccess(true);
        transaction.setType(PaymentTransactionType.UNCONFIRMED);
        transaction.getAdditionalFields().put("OPAQUE_DATA_DESCRIPTOR", dataDescriptor);
        transaction.getAdditionalFields().put("OPAQUE_DATA_VALUE", dataValue);

        transaction.setOrderPayment(paymentNonce);
        paymentNonce.addTransaction(transaction);
        orderService.addPaymentToOrder(cart, paymentNonce, null);
        orderService.save(cart, true);

        return processCompleteCheckoutOrderFinalized(redirectAttributes);
    }

    @RequestMapping(value = "/checkout/authnet/validate/merchant",
            method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String validateMerchant(@RequestBody String validationUrl) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException, JSONException {
        String keyStoreFile = authorizeNetConfiguration.getAppleKeyStoreFilePath();
        String keyStorePassword = authorizeNetConfiguration.getAppleKeyStorePassword();
        String uri = validationUrl;

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        KeyStore clientStore = KeyStore.getInstance("PKCS12");
        clientStore.load(new FileInputStream(classLoader.getResource(keyStoreFile).getFile()), keyStorePassword.toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(clientStore, keyStorePassword.toCharArray());
        KeyManager[] kms = kmf.getKeyManagers();
        SSLContext sslContext = null;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kms, null, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        URL url = new URL(uri);
        HttpsURLConnection urlConn = (HttpsURLConnection) url.openConnection();
        urlConn.setDoOutput(true);
        urlConn.setDoInput(true);
        urlConn.setRequestProperty("Content-Type", "application/json");
        urlConn.setRequestProperty("Accept", "application/json");
        urlConn.setRequestMethod("POST");
        JSONObject cred   = new JSONObject();
        cred.put("merchantIdentifier", authorizeNetConfiguration.getAppleMerchantId());
        cred.put("domainName", authorizeNetConfiguration.getVerifiedDomainName());
        cred.put("displayName", authorizeNetConfiguration.getVerifiedDomainDisplayName());

        OutputStreamWriter wr = new OutputStreamWriter
                (urlConn.getOutputStream());
        wr.write(cred.toString());
        wr.flush();
        StringBuilder sb = new StringBuilder();
        int HttpResult = urlConn.getResponseCode();
        if (HttpResult == HttpURLConnection.HTTP_OK) {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(urlConn.getInputStream(), "utf-8"));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();
            return sb.toString();
        } else {
            return urlConn.getResponseMessage();
        }
    }

}
```

When `processCompleteCheckoutOrderFinalized` is called, the checkout workflow is invoked and the `ValidateAndConfirmPaymentActivity`
is executed to confirm the payment nonce.

## Done!
At this point, all the configuration should be complete and you are now ready to test your integration with Authorize.net and Apple Pay.

> Note: To view transactions, go to the [Authorize.net Sandbox](https://sandbox.authorize.net/) and click on "Search" at the top.  You can view both settled and unsettled transactions this way.
