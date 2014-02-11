# Authorize.net Quick Start

Broadleaf Commerce offers an out-of-the-box Authorize.net solution that requires little configuration and is easily set up. For a more customized solution, please see [[Authorize.net Advance Configuration]].

**You must have completed the [[Authorize.net Environment Setup]] before continuing**

1. In your `applicationContext-servlet.xml`, replace the component scan

```xml
    <context:component-scan base-package="com.mycompany.sample" />
```

with

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
```

3. Exclude the authorizenet URL from the CSRF token filter in your `applicationContext-security.xml`

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
