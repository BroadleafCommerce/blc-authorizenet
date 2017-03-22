# Authorize.net Quick Start

Broadleaf Commerce offers an out-of-the-box Authorize.net solution that requires little configuration and is easily set up. 
The quick start solution implements the [[Accept.js | https://developer.authorize.net/api/reference/features/acceptjs.html]] model offered by Authorize.net.
This implementation should be useful for those with a simple checkout flow.

**You must have completed the [[Authorize.net Environment Setup]] before continuing**

## Authorize.net Configuration
1. In your `applicationContext-servlet.xml`, add the component scan

```xml
    <context:component-scan base-package="org.broadleafcommerce.vendor.authorizenet"/>
```

2. In your core `applicationContext.xml`, replace the block of text

```xml
    <!-- Scan DemoSite Sample Payment Gateway Implementation -->
    ...
    <!-- /End DemoSite NullPaymentGateway Config -->
```

with

```xml
    <!-- Scan for Authorize.net -->
    <context:component-scan base-package="org.broadleafcommerce.payment.service.gateway"/>
    <context:component-scan base-package="org.broadleafcommerce.vendor.authorizenet"/>
    <bean class="org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetGatewayType"/>

    <!-- Add Sample Thymeleaf Processor to test Hosted Payment Gateway (e.g. PayPal Express Flow) -->
    <bean id="mySampleConfigurationServices" class="org.springframework.beans.factory.config.ListFactoryBean">
        <property name="sourceList">
            <list>
               <ref bean="blAuthorizeNetConfigurationService"/>
            </list>
        </property>
    </bean>
    <bean class="org.broadleafcommerce.common.extensibility.context.merge.LateStageMergeBeanPostProcessor">
        <property name="collectionRef" value="mySampleConfigurationServices"/>
        <property name="targetRef" value="blPaymentGatewayConfigurationServices"/>
    </bean>
    
    <bean id="mySampleVariableExpressions" class="org.springframework.beans.factory.config.ListFactoryBean">
        <property name="sourceList">
            <list>
                <ref bean="blAuthorizeNetVariableExpression"/>
            </list>
        </property>
    </bean>
    <bean class="org.broadleafcommerce.common.extensibility.context.merge.LateStageMergeBeanPostProcessor">
        <property name="collectionRef" value="mySampleVariableExpressions"/>
        <property name="targetRef" value="blVariableExpressions"/>
    </bean>
```

## Adding Authorize.net Checkout Support

These instructions assume integration with the default Heat Clinic Demo Site provided with the framework.

1. Make sure the Authorize.net JS library is loaded on your checkout page:

```html
<script src="https://jstest.authorize.net/v1/Accept.js"></script>
```

2. Modify the Payment Form on your checkout page to look something like:

```html
<blc:form id="anet_checkout_form" th:action="@{checkout/authnet/complete}" method="POST">
    <input type="text" id="cardNumber" autocomplete="off" placeholder="Card Number" />
    <input type="text" id="nameOnCard" placeholder="Name on Card" />
    <input type="text" id="cardExpDate" autocomplete="off" placeholder="MM-YYYY" />
    <input type="text" id="securityCode" autocomplete="off" placeholder="CVV" />
    <input type="hidden" name="OPAQUE_DATA_DESCRIPTOR" id="OPAQUE_DATA_DESCRIPTOR"/>
    <input type="hidden" name="OPAQUE_DATA_VALUE" id="OPAQUE_DATA_VALUE"/>
    <input type="submit" id="anet_checkout_form_button" value="Complete Order" />
</blc:form>
```

> Note: IMPORTANT! Make sure you don't include the `name` attribute on sensitive data form elements. If the form is accidentally submitted and name attributes are present, sensitive data can reach your server.


3. Insert the following Javascript into your checkout html in order to generate the client token and retrieve the payment nonce:

```html
    <script type="text/javascript" th:inline="javascript">
        
        function sendPaymentDataToAnet() {
            var secureData = {}, authData = {}, cardData = {};
            
            cardData.cardNumber = document.getElementById('cardNumber').value;
            cardData.month = document.getElementById('cardExpDate').value.substring(0,2);
            cardData.year = document.getElementById('cardExpDate').value.substring(3,7);
            secureData.cardData = cardData;
        
            authData.clientKey = [[${#authorizenet.getClientKey()}]];
            authData.apiLoginID = [[${#authorizenet.getApiLoginId()}]];
            secureData.authData = authData;
            
            Accept.dispatchData(secureData, 'responseHandler');
            return false;
            
        }
        function responseHandler(response) {
            if (response.messages.resultCode === 'Error') {
                for (var i = 0; i < response.messages.message.length; i++) {
                    console.log(response.messages.message[i].code + ':' + response.messages.message[i].text);
                }
            } else {
                useOpaqueData(response.opaqueData)
            }
        }
        
        function useOpaqueData(responseData) {
            $("#OPAQUE_DATA_DESCRIPTOR").val(responseData.dataDescriptor);
            $("#OPAQUE_DATA_VALUE").val(responseData.dataValue);
            $("#anet_checkout_form").submit();
        }
    </script>
```

You'll also want to create an "on click" listener for submit button on the payment form. 
In our example, we'll create a separate Javascript file `authnet.js` to be loaded later with the following:

```
$(document).ready(function() {
    $('#anet_checkout_form_button').on("click", function(e) {
        e.preventDefault();
        sendPaymentDataToAnet();
    });
});        
```

> Note: the above sample JS assumes jQuery is loaded

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
At this point, all the configuration should be complete and you are now ready to test your integration with Authorize.net Accept.js. Add something to your cart and proceed with checkout.

> Note: When checking out, make sure that your credit card expiration date is in the form of MM-YYYY.

> Note: To view transactions, go to the [Authorize.net Sandbox](https://sandbox.authorize.net/) and click on "Search" at the top.  You can view both settled and unsettled transactions this way.
