# Authorize.net Quick Start

Broadleaf Commerce offers an out-of-the-box Authorize.net solution that requires little configuration and is easily set up. For a more customized solution, please see [[Authorize.net Advance Configuration]].

To quickly apply the following functionality to The Heat Clinic, copy the patch `demosite.diff` from the [Authorize.Net module repository](https://github.com/BroadleafCommerce/blc-authorizenet/tree/develop) into your Demo Site directory and type `git apply demosite.diff`.  Please note the patch currently supports Broadleaf framework versions 3.1 and up.

**You must have completed the [[Authorize.net Environment Setup]] before continuing**

##1) Adding Authorize.net DPM Support

First, you will need to add the quick-start Authorize.net application context `bl-authorizenet-applicationContext.xml` to your web.xml.
Your `patchConfigLocations` should look something like this:

```xml
<context-param>
    <param-name>patchConfigLocation</param-name>
    <param-value>
          classpath:/bl-open-admin-contentClient-applicationContext.xml
          classpath:/bl-cms-contentClient-applicationContext.xml
          classpath:/bl-authorizenet-applicationContext.xml
          classpath:/applicationContext.xml
          /WEB-INF/applicationContext-datasource.xml
          /WEB-INF/applicationContext-email.xml
          /WEB-INF/applicationContext-security.xml
          /WEB-INF/applicationContext.xml         
    </param-value>
</context-param>
```
> IMPORTANT: The order in which the application contexts are specified matters to the merge process. Make sure the "bl-authorizenet-applicationContext.xml" is specified BEFORE your applicationContext.xml that defines your "blConfiguration" bean. If you have customized your Runtime Environment Properties or Checkout Workflow, make sure to add this file in the appropriate order so that Broadleaf will pick up the the correct bean.

##2) Make your CheckoutController extend BroadleafAuthrorizeNetController

Next, you will need to create a controller that extends `BroadleafAuthrorizeNetController` to provide default `@RequestMappings` for your application.
Here is an example controller with the minimum amount of code needed to get Authorize.net integrated.

```java
@Controller
public class CheckoutController extends BroadleafAuthorizeNetController {

    //This method will build the dynamic form necessary to POST directly to Authorize.net
    @RequestMapping(value = "/checkout")
    public String checkout(HttpServletRequest request, HttpServletResponse response, Model model,
                           @ModelAttribute("shippingInfoForm") ShippingInfoForm shippingForm,
                           @ModelAttribute("billingInfoForm") BillingInfoForm billingForm) {
        return super.checkout(request, response, model);
    }

    //This is the URL that Authorize.net will call after receiving a Direct Post from a payment
    //This should match ${authorizenet.relay.response.url} in your properties file.
    @RequestMapping(value = "/process", method = RequestMethod.POST, produces = "text/html")
    public @ResponseBody String processAuthorizeNetAuthorizeAndDebit(HttpServletRequest request,     HttpServletResponse response, Model model, @ModelAttribute("shippingInfoForm") ShippingInfoForm shippingForm, @ModelAttribute("billingInfoForm") BillingInfoForm billingForm) throws NoSuchAlgorithmException, UnsupportedEncodingException, PricingException {
        return super.processAuthorizeNetAuthorizeAndDebit(request, response, model);
    }

}
```
##3) Create the HTML for the DPM form

You will also need to construct the form that you will send via the Direct Post Method (DPM). The checkout() method defined above will add the necessary attributes on the Spring Model object. 
Your page may look something like this:

> Note: it is important that all the hidden fields listed in the form below be included.

```html
<form th:action="${authorizenet_server_url}" method="post" id="billing_info">

    <input type="hidden" name="blc_cid" th:value="${blc_cid}" />
    <input type="hidden" name="blc_oid" th:value="${blc_oid}" />
    <input type="hidden" name="blc_tps" th:value="${blc_tps}" />
    <input type="hidden" name="x_invoice_num" th:value="${x_invoice_num}" />
    <input type="hidden" name="x_relay_url" th:value="${x_relay_url}" />
    <input type="hidden" name="x_login" th:value="${x_login}" />
    <input type="hidden" name="x_fp_sequence" th:value="${x_fp_sequence}" />
    <input type="hidden" name="x_fp_timestamp" th:value="${x_fp_timestamp}" />
    <input type="hidden" name="x_fp_hash" th:value="${x_fp_hash}" />
    <input type="hidden" name="x_version" th:value="${x_version}" />
    <input type="hidden" name="x_method" th:value="${x_method}" />
    <input type="hidden" name="x_type" th:value="${x_type}" />
    <input type="hidden" name="x_amount" th:value="${x_amount}" />
    <input type="hidden" name="x_test_request" th:value="${x_test_request}" />

    <div class="left_content">

        <input type="hidden" name="x_country" value="US" />

        <div class="form100" th:unless="${cart.fulfillmentGroups != null and #lists.size(cart.fulfillmentGroups) > 1}">
            <input id="use_shipping_address" type="checkbox" name="blc_use_shipping" th:disabled="${!validShipping}" /> Use Shipping Information
        </div>

        <div class="form30">
            <label for="firstName">First Name</label>
            <input type="text" name="x_first_name" class="field30 required clearable" th:disabled="${!validShipping}" />
        </div>

        <div class="form30 margin20">
            <label for="lastName">Last Name</label>
            <input type="text" name="x_last_name" class="field30 required clearable" th:disabled="${!validShipping}" />
        </div>

        <div class="form30 margin20">
            <label for="phone">Phone</label>
            <input type="text" name="x_phone" class="field30 clearable" th:disabled="${!validShipping}"/>
        </div>

        <div class="clearfix"></div>

        <div class="form50">
            <label for="address1">Address</label>
            <input type="text" name="x_address" class="field50 required clearable" th:disabled="${!validShipping}" />
        </div>

        <div class="form50 margin20">
            <label for="address2">Address 2</label>
            <input type="text" name="blc_address_line_2" class="field50 clearable" th:disabled="${!validShipping}" />
        </div>

        <div class="clearfix"></div>

        <div class="form30">
            <label for="city">City / State</label>

            <input type="text" name="x_city" class="field25 required clearable" th:disabled="${!validShipping}" />

            <select id="state" name="x_state" size="1" style="width: 48px;" class="required clearable" th:disabled="${!validShipping}">
                <option value="">--</option>
                <option th:each="state : ${states}" th:value="${state.abbreviation}" th:text="${state.abbreviation}"></option>
            </select>
        </div>

        <div class="form25 margin20">
            <label for="postal_code">Postal Code</label>
            <input type="text" name="x_zip" class="field25 clearable" th:disabled="${!validShipping}" />
        </div>

        <div class="form35 margin20">
            <label for="email">Email</label>
            <input type="text" name="x_email" class="field35 required clearable" th:disabled="${!validShipping}" />
        </div>
    </div>

    <div class="right_content payment_info">
        <h3>Payment Information</h3>

        <ul id="payment_methods">
            <li><img th:src="@{/img/payment/american-express-curved-32px.png}"/></li>
            <li><img th:src="@{/img/payment/mastercard-curved-32px.png}"/></li>
            <li><img th:src="@{/img/payment/visa-curved-32px.png}"/></li>
            <li><img th:src="@{/img/payment/paypal-curved-32px.png}"/></li>
        </ul>

        <dl id="paymentOptions">
            <dt>
                <input type="radio" name="paymentMethod" value="credit_card" id="paymentMethod_cc" />
                <label for="paymentMethod_cc">Credit Card</label>
            </dt>
            <dd>
                <div id="creditCardFields">


                    <div class="form25" style="width: 94%;">
                        <div style="float: left; width: 70%;">
                            <label for="cardNumber" class="prompt">Card Number</label>
                            <div class="element">
                                <input type="text" name="x_card_num" value="" id="cardNumber" class="field30" autocomplete="off" style="width: 100%" th:disabled="${!validShipping}" />
                            </div>
                        </div>
                    </div>

                    <div class="form25" style="width: 94%;">
                        <div style="float: left; width: 70%;">
                            <label for="cardNumber" class="prompt">Expiration Date (MMYY)</label>
                            <div class="element">
                                <input type="text" name="x_exp_date" value="" id="x_exp_date" class="field30" autocomplete="off" style="width: 100%" th:disabled="${!validShipping}" />
                            </div>
                        </div>
                    </div>

                    <div class="clearfix"></div>
                </div>

                <div>
                    <input type="submit" class="medium" value="Complete Order" th:disabled="${!validShipping}" th:classappend="${validShipping}? 'red' : 'gray'"/>
                </div>

            </dd>
        </dl>

    </div>
</form>
```

##4) Exclude the Authorize.net process URL from the CSRF checks

Since Authorize.net sends a POST to whatever you have in ${authorizenet.relay.response.url}, we will need to exclude this URL from XSRF attack prevention built into the Broadleaf demo. In this case, we _want_ the POST to come from a different site.  Override the Broadleaf CsrfFilter to exclude the process URL in your applicationContext-security.xml:

```xml
<bean id="blCsrfFilter" class="org.broadleafcommerce.common.security.handler.CsrfFilter">
    <property name="excludedRequestPatterns">
        <list>
            <value>/process</value>
        </list>
    </property>
</bean>
```

## Done!
At this point, all the configuration should be complete and you are now ready to test your integration with Authorize.net. Add something to your cart and proceed with checkout.
To customize your integration with Authorize.net even further, see [[Authorize.net Advance Configuration]] 
