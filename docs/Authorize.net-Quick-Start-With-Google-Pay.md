# Authorize.net Quick Start With Google Pay

Broadleaf Commerce offers an out-of-the-box Authorize.net solution that requires little configuration and is easily set up. 
The quick start solution implements the [[Google Pay | https://developer.authorize.net/api/reference/features/in-app.html#Google_Pay%E2%84%A2]] model offered by Authorize.net.
This implementation should be useful for those with a simple checkout flow.

**You must have completed the [[Authorize.net Environment Setup]] before continuing**


## Adding Authorize.net Checkout Support

These instructions assume integration with the default Heat Clinic Demo Site provided with the framework.

1. Make sure the Google Pay JS library is loaded on your checkout page:

```html
<script src="https://pay.google.com/gp/p/js/pay.js"></script>
```

2. Modify the Payment Form on your checkout page to look something like:

```html
<div id="google_pay_container"></div>
<blc:form id="anet_checkout_form" th:action="@{checkout/authnet/complete}" method="POST">
    <input type="hidden" name="OPAQUE_DATA_DESCRIPTOR" id="OPAQUE_DATA_DESCRIPTOR"/>
    <input type="hidden" name="OPAQUE_DATA_VALUE" id="OPAQUE_DATA_VALUE"/>
</blc:form>
```

3. Insert the following Javascript into your checkout html in order to generate the client token and retrieve the payment nonce:

```html
<script>
    document.addEventListener("DOMContentLoaded", function(event) {
        onGooglePayLoaded();
    });
        /**
         * Define the version of the Google Pay API referenced when creating your
         * configuration
         *
         * @see {@link https://developers.google.com/pay/api/web/reference/request-objects#PaymentDataRequest|apiVersion in PaymentDataRequest}
         */
        const baseRequest = {
            apiVersion: 2,
            apiVersionMinor: 0
        };

        /**
         * Card networks supported by your site and your gateway
         *
         * @see {@link https://developers.google.com/pay/api/web/reference/request-objects#CardParameters|CardParameters}
         * @todo confirm card networks supported by your site and gateway
         */
        const allowedCardNetworks = ["AMEX", "DISCOVER", "INTERAC", "JCB", "MASTERCARD", "VISA"];

        /**
         * Card authentication methods supported by your site and your gateway
         *
         * @see {@link https://developers.google.com/pay/api/web/reference/request-objects#CardParameters|CardParameters}
         * @todo confirm your processor supports Android device tokens for your
         * supported card networks
         */
        const allowedCardAuthMethods = ["PAN_ONLY", "CRYPTOGRAM_3DS"];

        /**
         * Identify your gateway and your site's gateway merchant identifier
         *
         * The Google Pay API response will return an encrypted payment method capable
         * of being charged by a supported gateway after payer authorization
         *
         * @todo check with your gateway on the parameters to pass
         * @see {@link https://developers.google.com/pay/api/web/reference/request-objects#gateway|PaymentMethodTokenizationSpecification}
         */
        const tokenizationSpecification = {
            type: 'PAYMENT_GATEWAY',
            parameters: {
                'gateway': 'authorizenet',
                'gatewayMerchantId': '' + [[${#authorizenet.getGatewayMerchantId()}]]
            }
        };

        /**
         * Describe your site's support for the CARD payment method and its required
         * fields
         *
         * @see {@link https://developers.google.com/pay/api/web/reference/request-objects#CardParameters|CardParameters}
         */
        const baseCardPaymentMethod = {
            type: 'CARD',
            parameters: {
                allowedAuthMethods: allowedCardAuthMethods,
                allowedCardNetworks: allowedCardNetworks
            }
        };

        /**
         * Describe your site's support for the CARD payment method including optional
         * fields
         *
         * @see {@link https://developers.google.com/pay/api/web/reference/request-objects#CardParameters|CardParameters}
         */
        const cardPaymentMethod = Object.assign(
            {},
            baseCardPaymentMethod,
            {
                tokenizationSpecification: tokenizationSpecification
            }
        );

        /**
         * An initialized google.payments.api.PaymentsClient object or null if not yet set
         *
         * @see {@link getGooglePaymentsClient}
         */
        let paymentsClient = null;

        /**
         * Configure your site's support for payment methods supported by the Google Pay
         * API.
         *
         * Each member of allowedPaymentMethods should contain only the required fields,
         * allowing reuse of this base request when determining a viewer's ability
         * to pay and later requesting a supported payment method
         *
         * @returns {object} Google Pay API version, payment methods supported by the site
         */
        function getGoogleIsReadyToPayRequest() {
            return Object.assign(
                {},
                baseRequest,
                {
                    allowedPaymentMethods: [baseCardPaymentMethod]
                }
            );
        }

        /**
         * Configure support for the Google Pay API
         *
         * @see {@link https://developers.google.com/pay/api/web/reference/request-objects#PaymentDataRequest|PaymentDataRequest}
         * @returns {object} PaymentDataRequest fields
         */
        function getGooglePaymentDataRequest() {
            const paymentDataRequest = Object.assign({}, baseRequest);
            paymentDataRequest.allowedPaymentMethods = [cardPaymentMethod];
            paymentDataRequest.transactionInfo = getGoogleTransactionInfo();
            paymentDataRequest.merchantInfo = {
                // @todo a merchant ID is available for a production environment after approval by Google
                // See {@link https://developers.google.com/pay/api/web/guides/test-and-deploy/integration-checklist|Integration checklist}
                // merchantId: '01234567890123456789',
                merchantName: 'Example Merchant'
            };

            paymentDataRequest.callbackIntents = ["PAYMENT_AUTHORIZATION"];

            return paymentDataRequest;
        }

        /**
         * Return an active PaymentsClient or initialize
         *
         * @see {@link https://developers.google.com/pay/api/web/reference/client#PaymentsClient|PaymentsClient constructor}
         * @returns {google.payments.api.PaymentsClient} Google Pay API client
         */
        function getGooglePaymentsClient() {
            if ( paymentsClient === null ) {
                paymentsClient = new google.payments.api.PaymentsClient({
                    environment: 'TEST',
                    paymentDataCallbacks: {
                        onPaymentAuthorized: onPaymentAuthorized
                    }
                });
            }
            return paymentsClient;
        }

        /**
         * Handles authorize payments callback intents.
         *
         * @param {object} paymentData response from Google Pay API after a payer approves payment through user gesture.
         * @see {@link https://developers.google.com/pay/api/web/reference/response-objects#PaymentData object reference}
         *
         * @see {@link https://developers.google.com/pay/api/web/reference/response-objects#PaymentAuthorizationResult}
         * @returns Promise<{object}> Promise of PaymentAuthorizationResult object to acknowledge the payment authorization status.
         */
        function onPaymentAuthorized(paymentData) {
            return new Promise(function(resolve, reject){
                // handle the response
                processPayment(paymentData)
                    .then(function() {
                        resolve({transactionState: 'SUCCESS'});
                    })
                    .catch(function() {
                        resolve({
                            transactionState: 'ERROR',
                            error: {
                                intent: 'PAYMENT_AUTHORIZATION',
                                message: 'Insufficient funds',
                                reason: 'PAYMENT_DATA_INVALID'
                            }
                        });
                    });
            });
        }

        /**
         * Initialize Google PaymentsClient after Google-hosted JavaScript has loaded
         *
         * Display a Google Pay payment button after confirmation of the viewer's
         * ability to pay.
         */
        function onGooglePayLoaded() {
            const paymentsClient = getGooglePaymentsClient();
            paymentsClient.isReadyToPay(getGoogleIsReadyToPayRequest())
                .then(function(response) {
                    if (response.result) {
                        addGooglePayButton();
                    }
                })
                .catch(function(err) {
                    // show error in developer console for debugging
                    console.error(err);
                });
        }

        /**
         * Add a Google Pay purchase button alongside an existing checkout button
         *
         * @see {@link https://developers.google.com/pay/api/web/reference/request-objects#ButtonOptions|Button options}
         * @see {@link https://developers.google.com/pay/api/web/guides/brand-guidelines|Google Pay brand guidelines}
         */
        function addGooglePayButton() {
            const paymentsClient = getGooglePaymentsClient();
            const button =
                paymentsClient.createButton({onClick: onGooglePaymentButtonClicked});
            document.getElementById('google_pay_container').appendChild(button);
        }

        /**
         * Provide Google Pay API with a payment amount, currency, and amount status
         *
         * @see {@link https://developers.google.com/pay/api/web/reference/request-objects#TransactionInfo|TransactionInfo}
         * @returns {object} transaction info, suitable for use as transactionInfo property of PaymentDataRequest
         */
        function getGoogleTransactionInfo() {
            return {
                displayItems: [
                    {
                        label: "Subtotal",
                        type: "SUBTOTAL",
                        price: "" + [[${cart.subTotal}]],
                    },
                    {
                        label: "Tax",
                        type: "TAX",
                        price: "" + [[${cart.totalTax}]],
                    }
                ],
                countryCode: 'US',
                currencyCode: "USD",
                totalPriceStatus: "FINAL",
                totalPrice: "" + [[${cart.total}]],
                totalPriceLabel: "Total"
            };
        }

        /**
         * Show Google Pay payment sheet when Google Pay payment button is clicked
         */
        function onGooglePaymentButtonClicked() {
            const paymentDataRequest = getGooglePaymentDataRequest();
            paymentDataRequest.transactionInfo = getGoogleTransactionInfo();

            const paymentsClient = getGooglePaymentsClient();
            paymentsClient.loadPaymentData(paymentDataRequest);
        }

        /**
         * Process payment data returned by the Google Pay API
         *
         * @param {object} paymentData response from Google Pay API after user approves payment
         * @see {@link https://developers.google.com/pay/api/web/reference/response-objects#PaymentData|PaymentData object reference}
         */
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
            paymentToken = paymentData.paymentMethodData.tokenizationData.token;
            var enc = window.btoa(paymentToken);
            $("#OPAQUE_DATA_DESCRIPTOR").val('COMMON.GOOGLE.INAPP.PAYMENT');
            $("#OPAQUE_DATA_VALUE").val(enc);
            $("#anet_checkout_form").submit();
        }
</script>
```

4. Create a Spring MVC Controller to handle the form you created above in order to process the payment nonce and confirm the transaction. It would look something like this:

```java
@Controller
public class AuthorizeNetCheckoutController extends BroadleafCheckoutController {

    @Resource(name = "blAddressService")
    protected AddressService addressService;

    @RequestMapping(value = "/checkout/authnet/complete")
    public String completeAuthorizeNetCheckout(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes, @PathVariable Map<String, String> pathVars) throws PaymentException, PricingException {
        //Get Cart
        Order cart = CartState.getCart();
        //Get Payment Nonce From Request
        String dataDescriptor = request.getParameter("OPAQUE_DATA_DESCRIPTOR");
        String dataValue = request.getParameter("OPAQUE_DATA_VALUE");

        //Create a new PAYMENT_NONCE Order Payment
        OrderPayment paymentNonce = orderPaymentService.create();
        paymentNonce.setType(PaymentType.CREDIT_CARD);
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
        
        if (tempPayment != null){
            paymentNonce.setBillingAddress(addressService.copyAddress(tempPayment.getBillingAddress()));
            orderService.removePaymentFromOrder(cart, tempPayment);
        }

        // Create the UNCONFIRMED transaction for the payment
        PaymentTransaction transaction = orderPaymentService.createTransaction();
        transaction.setAmount(cart.getTotalAfterAppliedPayments());
        transaction.setRawResponse("Authorize.net Accept.js Payment Nonce");
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

}
```

When `processCompleteCheckoutOrderFinalized` is called, the checkout workflow is invoked and the `ValidateAndConfirmPaymentActivity`
is executed to confirm the payment nonce.

## Done!
At this point, all the configuration should be complete and you are now ready to test your integration with Authorize.net and Google Pay. Add something to your cart and proceed with checkout.

> Note: When checking out, make sure that your credit card expiration date is in the form of MM-YYYY.

> Note: To view transactions, go to the [Authorize.net Sandbox](https://sandbox.authorize.net/) and click on "Search" at the top.  You can view both settled and unsettled transactions this way.
