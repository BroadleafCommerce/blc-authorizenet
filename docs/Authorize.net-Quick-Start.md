# Authorize.net Quick Start

Broadleaf Commerce offers an out-of-the-box Authorize.net solution that requires little configuration and is easily set up. For a more customized solution, please see [[Authorize.net Advance Configuration]].

**You must have completed the [[Authorize.net Environment Setup]] before continuing**

1. Exclude the authorizenet URL from the CSRF token filter in your `applicationContext-security.xml`

```xml
    <bean id="blCsrfFilter" class="org.broadleafcommerce.common.security.handler.CsrfFilter" >
        <property name="excludedRequestPatterns">
            <list>
                <value>/authorizenet/**</value>
            </list>
        </property>
    </bean>
```

## Done!
At this point, all the configuration should be complete and you are now ready to test your integration with Authorize.net. Add something to your cart and proceed with checkout.

> Note: When checking out, make sure that your credit card expiration date is in the form of MM-YYYY.

> Note: To view transactions, go to the [Authorize.net Sandbox](https://sandbox.authorize.net/) and click on "Search" at the top.  You can view both settled and unsettled transactions this way.
