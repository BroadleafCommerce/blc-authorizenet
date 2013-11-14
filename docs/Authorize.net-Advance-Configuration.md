# Authorize.net Advance Configuration

Broadleaf allows you to customize many aspects of your Authorize.net DPM integration.

**You must have completed the [[Authorize.net Environment Setup]] before continuing**

## Configuring Authorize.net Payments

You will need to declare the following Spring beans in your application context:

```xml
    <!-- Override the default Broadleaf Credit Card Service with Authorize.net -->
    <bean id="blCreditCardService" class="org.broadleafcommerce.core.payment.service.PaymentServiceImpl">
        <property name="paymentModule" ref="blAuthorizeNetModule"/>
    </bean>

    <bean id="blAuthorizeNetModule" class="org.broadleafcommerce.payment.service.module.AuthorizeNetPaymentModule">
        <property name="authorizeNetPaymentService" ref="blAuthorizeNetVendorOrientedPaymentService"/>
        <property name="stateService" ref="blStateService"/>
        <property name="countryService" ref="blCountryService"/>
        <property name="customerService" ref="blCustomerService"/>
    </bean>

    <bean id="blAuthorizeNetVendorOrientedPaymentService" class="org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetPaymentServiceImpl">
        <property name="failureReportingThreshold" value="1"/>
        <property name="gatewayRequest">
            <bean class="org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetGatewayRequestImpl">
                <property name="apiLoginId" value="${authorizenet.api.login.id}"/>
                <property name="transactionKey" value="${authorizenet.transaction.key}"/>
                <property name="relayResponseUrl" value="${authorizenet.relay.response.url}"/>
                <property name="merchantMD5Key" value="${authorizenet.merchant.md5.key}"/>
            </bean>
        </property>
    </bean>
```
> Note: The [[Authorize.net Quick Start]] solution offers a default application context with these beans already defined and can be used as a reference. Please see `bl-authorizenet-applicationContext.xml`

* `failureReportingThreshold` - used by [[QoS | QoS Configuration]] to determine how many times the service should fail before it is considered to be "down".
* `apiLoginId` - the Authorize.net api login
* `transactionKey` - the Authorize.net transaction key
* `relayResponseUrl` - the destination in your app that Authorize.net calls

> IMPORTANT: relayResponseUrl : must be a publicly accessible URL. See [[Testing using DPM | http://community.developer.authorize.net/t5/Integration-and-Testing/Direct-Post-in-a-development-environment-behind-a-firewall/td-p/8906]] for more details;

See [[Authorize.net Environment Setup]] to learn how to configure the variable properties.

## Customizing the AuthorizenetCheckoutService

Broadleaf provides the `AuthorizenetCheckoutService`, an abstraction layer on top of the payment workflow that aids in creating
the objects necessary for completing a successful checkout. The `blAuthorizenetCheckoutService` can be overridden using a custom implementation.
This API is called from the `BroadleafAuthorizenetController` used in the [[Authorize.net Quick Start]] solution.

## Manually Configuring the Presentation Layer

It is up to you to choose the presentation layer approach that best fits your needs, but regardless of the approach, 
you will be required at some point to compile the [[PaymentInfo | https://github.com/BroadleafCommerce/BroadleafCommerce/blob/master/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/payment/domain/PaymentInfo.java]] information 
to the order before calling performCheckout on the CheckoutService. 
Most Broadleaf Commerce users will choose Spring MVC and will likely implement their own CheckoutController. 
If your implementation does not require that much customization, consider extending the `BroadleafAuthorizenetController`.
This class is also a useful reference in setting up a custom payment workflow with Authorize.net.

The final step is to create the dynamic HTML form that will make a direct post to Authorize.net.
A list of fields that can be sent to Authorize.net [[can be found here | http://developer.authorize.net/api/dpm/]].
