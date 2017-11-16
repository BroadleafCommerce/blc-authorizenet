/*
 * Copyright 2008-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadleafcommerce.payment.service.module;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.persistence.EntityConfiguration;
import org.broadleafcommerce.common.time.SystemTime;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.payment.domain.PaymentInfo;
import org.broadleafcommerce.core.payment.domain.PaymentResponseItem;
import org.broadleafcommerce.core.payment.service.PaymentContext;
import org.broadleafcommerce.core.payment.service.exception.PaymentException;
import org.broadleafcommerce.core.payment.service.module.PaymentModule;
import org.broadleafcommerce.core.payment.service.type.PaymentInfoAdditionalFieldType;
import org.broadleafcommerce.core.payment.service.type.PaymentInfoType;
import org.broadleafcommerce.profile.core.domain.Address;
import org.broadleafcommerce.profile.core.domain.Country;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.Phone;
import org.broadleafcommerce.profile.core.domain.State;
import org.broadleafcommerce.profile.core.service.CountryService;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.StateService;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetPaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Resource;

import net.authorize.Environment;
import net.authorize.ResponseCode;
import net.authorize.ResponseField;
import net.authorize.api.contract.v1.CreateTransactionRequest;
import net.authorize.api.contract.v1.CreateTransactionResponse;
import net.authorize.api.contract.v1.CustomerAddressType;
import net.authorize.api.contract.v1.CustomerDataType;
import net.authorize.api.contract.v1.CustomerTypeEnum;
import net.authorize.api.contract.v1.MerchantAuthenticationType;
import net.authorize.api.contract.v1.MessageTypeEnum;
import net.authorize.api.contract.v1.OpaqueDataType;
import net.authorize.api.contract.v1.OrderType;
import net.authorize.api.contract.v1.TransactionRequestType;
import net.authorize.api.contract.v1.TransactionTypeEnum;
import net.authorize.api.controller.CreateTransactionController;
import net.authorize.api.controller.base.ApiOperationBase;
import net.authorize.sim.Result;

import static org.broadleafcommerce.core.payment.service.type.TransactionType.AUTHORIZEANDDEBIT;

/**
 * @author elbertbautista
 */
public class AuthorizeNetPaymentModule implements PaymentModule {

    private static final Log LOG = LogFactory.getLog(AuthorizeNetPaymentModule.class);

    protected AuthorizeNetPaymentService authorizeNetPaymentService;

    protected StateService stateService;

    protected CountryService countryService;

    protected CustomerService customerService;
    
    @Resource(name="blEntityConfiguration")
    protected EntityConfiguration entityConfiguration;

    @Value("${authorizenet.is_production:false}")
    protected boolean isProduction;

    @Value("${authorizenet.api.login.id}")
    protected String loginId;
    @Value("${authorizenet.transaction.key}")
    protected String transactionKey;

    @Override
    public PaymentResponseItem authorize(PaymentContext paymentContext) throws PaymentException {
        return authorizeAndDebit(paymentContext);
    }

    @Override
    public PaymentResponseItem reverseAuthorize(PaymentContext paymentContext) throws PaymentException {
        throw new PaymentException("The reverseAuthorize method is not supported by this org.broadleafcommerce.payment.service.module.AuthorizeNetPaymentModule");
    }

    @Override
    public PaymentResponseItem debit(PaymentContext paymentContext) throws PaymentException {
        throw new PaymentException("The debit method is not supported by this org.broadleafcommerce.payment.service.module.AuthorizeNetPaymentModule");
    }

    @Override
    public PaymentResponseItem authorizeAndDebit(PaymentContext paymentContext) throws PaymentException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating Payment Response for authorize and debit.");
        }

        PaymentInfo paymentInfo = paymentContext.getPaymentInfo();

        Assert.isTrue(paymentInfo.getRequestParameterMap() != null && !paymentInfo.getRequestParameterMap().isEmpty(), "Must set the Request Parameter Map on the PaymentInfo instance.");

        PaymentResponseItem paymentResponseItem = doAuthorizeAndDebitWithToken(paymentContext);
        Map<String, String[]> requestParameterMap = paymentContext.getPaymentInfo().getRequestParameterMap();
        saveAnonymousCustomerInfo(paymentContext, paymentResponseItem, flattenMap(requestParameterMap));
        return paymentResponseItem;
    }

    private Map<String,String> flattenMap(Map<String, String[]> requestParameterMap) {
        Iterator<Map.Entry<String, String[]>> iterator = requestParameterMap.entrySet().iterator();
        Map<String,String> result = new HashMap<String, String>();
        while (iterator.hasNext()){
            Map.Entry<String, String[]> next = iterator.next();
            result.put(next.getKey(),next.getValue()!=null && next.getValue().length>0?next.getValue()[0]:"");
        }
        return result;
    }

    private PaymentResponseItem doAuthorizeAndDebitWithToken(PaymentContext paymentContext){
        OrderType orderType = new OrderType();

        Order order = paymentContext.getPaymentInfo().getOrder();
        orderType.setInvoiceNumber(String.valueOf(order.getId()));
        orderType.setDescription(order.getName());

        TransactionRequestType transaction = new TransactionRequestType();
        transaction.setOrder(orderType);

        if (!isProduction) {
            ApiOperationBase.setEnvironment(Environment.SANDBOX);
        } else {
            ApiOperationBase.setEnvironment(Environment.PRODUCTION);
        }
        MerchantAuthenticationType merchantAuthenticationType  = new MerchantAuthenticationType() ;
        merchantAuthenticationType.setName(loginId);
        merchantAuthenticationType.setTransactionKey(transactionKey);
        ApiOperationBase.setMerchantAuthentication(merchantAuthenticationType);

        net.authorize.api.contract.v1.PaymentType paymentType = new net.authorize.api.contract.v1.PaymentType();
        OpaqueDataType data = new OpaqueDataType();
        data.setDataDescriptor((String)paymentContext.getPaymentInfo().getAdditionalFields().get("OPAQUE_DATA_DESCRIPTOR"));
        data.setDataValue((String)paymentContext.getPaymentInfo().getAdditionalFields().get("OPAQUE_DATA_VALUE"));
        paymentType.setOpaqueData(data);

        CustomerDataType customer = new CustomerDataType();
        customer.setType(CustomerTypeEnum.INDIVIDUAL);
        customer.setId(String.valueOf(order.getCustomer().getId()));
        customer.setEmail(order.getEmailAddress());

        transaction.setCustomer(customer);

        Address address = paymentContext.getPaymentInfo().getAddress();

        CustomerAddressType customerAddress = new CustomerAddressType();
        customerAddress.setFirstName(address.getFirstName());
        customerAddress.setLastName(address.getLastName());
        customerAddress.setAddress(address.getAddressLine1());
        customerAddress.setCity(address.getCity());
        customerAddress.setState(address.getState().getAbbreviation());
        customerAddress.setZip(address.getPostalCode());
        customerAddress.setCountry(address.getCountry().getName());
        customerAddress.setPhoneNumber(address.getPrimaryPhone());
        if (StringUtils.isNotEmpty(address.getEmailAddress())) {
            customerAddress.setEmail(address.getEmailAddress());
        } else {
            customerAddress.setEmail(order.getEmailAddress());
        }

        transaction.setPayment(paymentType);
        transaction.setBillTo(customerAddress);
        transaction.setAmount(paymentContext.getPaymentInfo().getAmount().getAmount());
        transaction.setTransactionType(TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION.value());

        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setTransactionRequest(transaction);

        CreateTransactionController controller = new CreateTransactionController(apiRequest);
        controller.execute();

        CreateTransactionResponse response = controller.getApiResponse();
        PaymentResponseItem responseItem = entityConfiguration.createEntityInstance(PaymentResponseItem.class.getName(), PaymentResponseItem.class);
        responseItem.setProcessorResponseText(response.getTransactionResponse().getRawResponseCode());

        responseItem.setTransactionTimestamp(SystemTime.asDate());
        responseItem.setProcessorResponseCode(response.getTransactionResponse().getResponseCode());
        responseItem.setTransactionType(AUTHORIZEANDDEBIT);
        responseItem.setAmountPaid(paymentContext.getPaymentInfo().getAmount());
        responseItem.setTransactionId(response.getTransactionResponse().getTransId());
        responseItem.setReferenceNumber(response.getRefId());
        responseItem.setTransactionSuccess(response.getMessages().getResultCode().equals(MessageTypeEnum.OK) &&
                Integer.toString(ResponseCode.APPROVED.getCode()).equals(response.getTransactionResponse().getResponseCode()));

        return responseItem;
    }

    @Override
    public PaymentResponseItem credit(PaymentContext paymentContext) throws PaymentException {
        throw new PaymentException("The credit method is not supported by this org.broadleafcommerce.payment.service.module.AuthorizeNetPaymentModule");
    }

    @Override
    public PaymentResponseItem voidPayment(PaymentContext paymentContext) throws PaymentException {
        throw new PaymentException("The voidPayment method is not supported by this org.broadleafcommerce.payment.service.module.AuthorizeNetPaymentModule");
    }

    @Override
    public PaymentResponseItem balance(PaymentContext paymentContext) throws PaymentException {
        throw new PaymentException("The balance method is not supported by this org.broadleafcommerce.payment.service.module.AuthorizeNetPaymentModule");
    }



    protected boolean isValidTransaction(Result result){
       return result.isAuthorizeNet() && result.isApproved();
    }

    protected void saveAnonymousCustomerInfo(PaymentContext paymentContext, PaymentResponseItem responseItem, Map<String, String> result) {
        if (responseItem.getTransactionSuccess()) {
            if (LOG.isDebugEnabled()){
                LOG.debug("Fill out a few customer values for anonymous customers");
            }

            Order order = paymentContext.getPaymentInfo().getOrder();
            Customer customer = order.getCustomer();
            if (StringUtils.isEmpty(customer.getFirstName()) && result.get(ResponseField.FIRST_NAME.getFieldName()) != null) {
                customer.setFirstName(result.get(ResponseField.FIRST_NAME.getFieldName()));
            }
            if (StringUtils.isEmpty(customer.getLastName()) && result.get(ResponseField.LAST_NAME.getFieldName()) != null) {
                customer.setLastName(result.get(ResponseField.LAST_NAME.getFieldName()));
            }
            if (StringUtils.isEmpty(customer.getEmailAddress()) && result.get(ResponseField.EMAIL_ADDRESS.getFieldName()) != null) {
                customer.setEmailAddress(result.get(ResponseField.EMAIL_ADDRESS.getFieldName()));
            }
            customerService.saveCustomer(customer, false);
        }
    }

    protected void setPaymentInfoAdditionalFields(PaymentContext paymentContext, Result result) {
        //Note that you cannot perform operations on paymentContext.getPaymentInfo() directly because that is a copy of the actual payment on the order.
        //In order to persist custom attributes to the credit card payment info on the order we must look it up first.
        PaymentInfo paymentInfo = null;
        for (PaymentInfo pi : paymentContext.getPaymentInfo().getOrder().getPaymentInfos()) {
            if (PaymentInfoType.CREDIT_CARD.equals(pi.getType())) {
                paymentInfo = pi;
            }
        }

        if (paymentInfo != null) {
            Map<String, String> additionalFields = new HashMap<String, String>();
            additionalFields.put(PaymentInfoAdditionalFieldType.NAME_ON_CARD.getType(), result.getResponseMap().get(ResponseField.FIRST_NAME.getFieldName()) + " " + result.getResponseMap().get(ResponseField.LAST_NAME.getFieldName()));
            additionalFields.put(PaymentInfoAdditionalFieldType.CARD_TYPE.getType(), result.getResponseMap().get(ResponseField.CARD_TYPE.getFieldName()));
            additionalFields.put(PaymentInfoAdditionalFieldType.LAST_FOUR.getType(), result.getResponseMap().get(ResponseField.ACCOUNT_NUMBER.getFieldName()));
            paymentInfo.setAdditionalFields(additionalFields);
        }
    }

    protected void setPaymentResponseAdditionalFields(PaymentContext paymentContext, PaymentResponseItem responseItem, Result result) {
        Map<String, String> map = new HashMap<String, String>();

        for (ResponseField field : ResponseField.values()) {
            if (!isBillingAddressField(field) && !isShippingAddressField(field) && !StringUtils.isEmpty(result.getResponseMap().get(field.getFieldName()))){
                map.put(field.getFieldName(), result.getResponseMap().get(field.getFieldName()));
            }
        }

        responseItem.setAdditionalFields(map);
    }

    protected void setBillingInfo(PaymentContext paymentContext, Result result) {
        //Note that you cannot perform operations on paymentContext.getPaymentInfo() directly because that is a copy of the actual payment on the order.
        //In order to persist custom attributes to the credit card payment info on the order we must look it up first.
        PaymentInfo paymentInfo = null;
        for (PaymentInfo pi : paymentContext.getPaymentInfo().getOrder().getPaymentInfos()) {
            if (PaymentInfoType.CREDIT_CARD.equals(pi.getType())) {
                paymentInfo = pi;
            }
        }

        Address address = entityConfiguration.createEntityInstance(Address.class.getName(), Address.class);
        initializeEmptyAddress(address);
        Phone phone = null;
        boolean billingPopulated = false;

        for (ResponseField field : ResponseField.values()) {
            if (isBillingAddressField(field) && !StringUtils.isEmpty(result.getResponseMap().get(field.getFieldName()))) {
                String value = result.getResponseMap().get(field.getFieldName());

                if (!StringUtils.isEmpty(value)) {
                    switch (field) {
                        case FIRST_NAME: address.setFirstName(value); break;
                        case LAST_NAME: address.setLastName(value); break;
                        case COMPANY: address.setCompanyName(value); break;
                        case ADDRESS: address.setAddressLine1(value); break;
                        case CITY: address.setCity(value); break;
                        case STATE:
                            State state = stateService.findStateByAbbreviation(value);
                            if (state != null) {
                                address.setState(state);
                            }
                            break;
                        case COUNTRY:
                            Country country = countryService.findCountryByAbbreviation(value);
                            address.setCountry(country);
                            break;
                        case ZIP_CODE: address.setPostalCode(value); break;
                        case EMAIL_ADDRESS: address.setEmailAddress(value); break;
                        case PHONE: 
                            phone = entityConfiguration.createEntityInstance(Phone.class.getName(), Phone.class);
                            phone.setPhoneNumber(value);
                            break;
                        default: break;
                    }
                }
                billingPopulated = true;
            }
        }

        //set billing address on the payment info
        if (billingPopulated) {
            paymentInfo.setAddress(address);
            paymentInfo.setPhone(phone);
        }
    }

    protected void setShippingInfo(PaymentContext paymentContext, Result result) {
        Order order = paymentContext.getPaymentInfo().getOrder();
        Address address = entityConfiguration.createEntityInstance(Address.class.getName(), Address.class);
        initializeEmptyAddress(address);
        boolean shippingPopulated = false;
        for (ResponseField field : ResponseField.values()) {
            if (isShippingAddressField(field) && !StringUtils.isEmpty(result.getResponseMap().get(field.getFieldName()))) {
                String value = result.getResponseMap().get(field.getFieldName());

                if (!StringUtils.isEmpty(value) && address != null) {
                    switch (field) {
                        case SHIP_TO_FIRST_NAME: address.setFirstName(value); break;
                        case SHIP_TO_LAST_NAME: address.setLastName(value); break;
                        case SHIP_TO_COMPANY: address.setCompanyName(value); break;
                        case SHIP_TO_ADDRESS: address.setAddressLine1(value); break;
                        case SHIP_TO_CITY: address.setCity(value); break;
                        case SHIP_TO_STATE:
                            State state = stateService.findStateByAbbreviation(value);
                            if (state != null) {
                                address.setState(state);
                            }
                            break;
                        case SHIP_TO_COUNTRY:
                            Country country = countryService.findCountryByAbbreviation(value);
                            address.setCountry(country);
                            break;
                        case SHIP_TO_ZIP_CODE: address.setPostalCode(value); break;
                        default: break;
                    }
                }
                shippingPopulated = true;
            }
        }

        //set shipping info on the fulfillment group
        if (shippingPopulated) {
            populateShippingAddressOnOrder(order, address);
        }
    }
    
    /**
     * Sets all the non-nullable fields for an address to empty strings. This way we won't get any Hibernate exceptions
     * should a user get past any client-side validations for BLC-required address fields. The correct thing to do is not
     * to show an error message because the card would have already been successfully charged.
     * 
     * @param address
     */
    protected void initializeEmptyAddress(Address address) {
        address.setAddressLine1("");
        address.setCity("");
        address.setPostalCode("");
    }

    /**
     * If you pass the shipping address to Authorize.net, there has to be an existing fulfillment group on the order.
     * This must be done because of pricing considerations. The fulfillment group must be constructed when adding to the
     * cart or sometime before calling the gateway. This depends on UX. This default implementation assumes one fulfillment
     * group per order because Authorize.net only supports a single shipping address. Override this method if necessary.
     * 
     * @param order
     * @param shippingAddress
     */
    protected void populateShippingAddressOnOrder(Order order, Address shippingAddress) {
        if (order.getFulfillmentGroups() != null && order.getFulfillmentGroups().size()==1) {
            FulfillmentGroup fg = order.getFulfillmentGroups().get(0);
            fg.setAddress(shippingAddress);
        }
    }

    protected boolean isBillingAddressField(ResponseField field) {
        if (ResponseField.FIRST_NAME.equals(field) || ResponseField.LAST_NAME.equals(field) ||
                ResponseField.COMPANY.equals(field) ||
                ResponseField.ADDRESS.equals(field) ||
                ResponseField.CITY.equals(field) ||
                ResponseField.STATE.equals(field) ||
                ResponseField.ZIP_CODE.equals(field) ||
                ResponseField.COUNTRY.equals(field) ||
                ResponseField.EMAIL_ADDRESS.equals(field) ||
                ResponseField.PHONE.equals(field)) {
            return true;
        }
        return false;
    }

    protected boolean isShippingAddressField(ResponseField field) {
        if (ResponseField.SHIP_TO_FIRST_NAME.equals(field) || ResponseField.SHIP_TO_LAST_NAME.equals(field) ||
                ResponseField.SHIP_TO_COMPANY.equals(field) ||
                ResponseField.SHIP_TO_ADDRESS.equals(field) ||
                ResponseField.SHIP_TO_CITY.equals(field) ||
                ResponseField.SHIP_TO_STATE.equals(field) ||
                ResponseField.SHIP_TO_ZIP_CODE.equals(field) ||
                ResponseField.SHIP_TO_COUNTRY.equals(field)) {
            return true;
        }
        return false;
    }

    @Override
    public Boolean isValidCandidate(PaymentInfoType paymentType) {
        return PaymentInfoType.CREDIT_CARD.equals(paymentType);
    }

    public AuthorizeNetPaymentService getAuthorizeNetPaymentService() {
        return authorizeNetPaymentService;
    }

    public void setAuthorizeNetPaymentService(AuthorizeNetPaymentService authorizeNetPaymentService) {
        this.authorizeNetPaymentService = authorizeNetPaymentService;
    }

    public StateService getStateService() {
        return stateService;
    }

    public void setStateService(StateService stateService) {
        this.stateService = stateService;
    }

    public CountryService getCountryService() {
        return countryService;
    }

    public void setCountryService(CountryService countryService) {
        this.countryService = countryService;
    }

    public CustomerService getCustomerService() {
        return customerService;
    }

    public void setCustomerService(CustomerService customerService) {
        this.customerService = customerService;
    }
}
