/*
 * Copyright 2008-2009 the original author or authors.
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

package org.broadleafcommerce.vendor;

import net.authorize.sim.Result;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderImpl;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerImpl;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.test.BaseTest;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetCheckoutService;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetPaymentService;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *  @author elbertbautista
 *
 **/
public class AuthorizeNetIntegrationTest extends BaseTest {

    static {
        moduleContexts.add("bl-authorizenet-applicationContext-test.xml");
    }

    private static final Log LOG = LogFactory.getLog(AuthorizeNetIntegrationTest.class);
    public static final String BLC_CID = "blc_cid";
    public static final String BLC_OID = "blc_oid";
    public static final String BLC_TPS = "blc_tps";

    @Resource(name = "blAuthorizeNetVendorOrientedPaymentService")
    protected AuthorizeNetPaymentService paymentService;

    @Resource(name = "blAuthorizeNetCheckoutService")
    protected AuthorizeNetCheckoutService checkoutService;

    @Resource(name="blCustomerService")
    protected CustomerService customerService;

    @Resource(name="blOrderService")
    protected OrderService orderService;

    @Value("${authorizenet.error.url}")
    protected String authorizeNetErrorUrl;

    @Value("${authorizenet.confirm.url}")
    protected String authorizeNetConfirmUrl;

    @Value("${authorizenet.jetty.integration.port}")
    protected String authorizeNetJettyIntegrationPort;

    final Order order = new OrderImpl();

    @BeforeClass
    @Rollback(false)
    @Transactional
    public void setupData() throws PricingException {
        order.setId(1L);
        order.setTotal(new Money("100.00"));

        Customer customer = new CustomerImpl();
        customer.setId(1L);
        customer.setFirstName("Michael");
        customer.setLastName("Jordan");
        customer = customerService.saveCustomer(customer);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.IN_PROCESS);

        orderService.save(order, false);
    }

    @Test(groups = { "testSuccessfulAuthorizenetAuthorizeAndDebit" })
    @Rollback(false)
    @Transactional
    public void testSuccessfulAuthorizenetAuthorizeAndDebit() throws Exception {
        if (checkoutService == null ||
                paymentService == null ||
                "?".equals(paymentService.getGatewayRequest().getApiLoginId()) || "${authorizenet.api.login.id}".equals(paymentService.getGatewayRequest().getApiLoginId()) ||
                "?".equals(paymentService.getGatewayRequest().getMerchantMD5Key()) || "${authorizenet.merchant.md5.key}".equals(paymentService.getGatewayRequest().getMerchantMD5Key()) ||
                "?".equals(paymentService.getGatewayRequest().getTransactionKey()) || "${authorizenet.transaction.key}".equals(paymentService.getGatewayRequest().getTransactionKey()) ||
                "?".equals(paymentService.getGatewayRequest().getServerUrl()) || "${authorizenet.server.url}".equals(paymentService.getGatewayRequest().getServerUrl()) ||
                "?".equals(paymentService.getGatewayRequest().getRelayResponseUrl()) || "${authorizenet.relay.response.url}".equals(paymentService.getGatewayRequest().getRelayResponseUrl()) ||
                "?".equals(authorizeNetConfirmUrl) || "${authorizenet.confirm.url}".equals(authorizeNetConfirmUrl) ||
                "?".equals(authorizeNetJettyIntegrationPort) || "${authorizenet.jetty.integration.port}".equals(authorizeNetJettyIntegrationPort)
                ) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("*** Skipping testSuccessfulAuthorizenetAuthorizeAndDebit - (Merchant Keys/Properties are not set or the Jetty Integration Environment properties are not configured.) ***");
            }
            return;
        }

        //Create a Handler to process the callback from Authorize.net
        //This mocked handler returns a default "Success" response.
        Handler handler=new AbstractHandler()
        {
            @Override
            public void handle(String s, Request r, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

                if (LOG.isDebugEnabled()) {
                    LOG.debug("*** Callback received from Authorize.net ***");
                    LOG.debug("*** Authorize.net Parameters: ***");
                    LOG.debug(requestParamToString(request));
                }

                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_OK);
                String responseBody = "";

                System.out.println("Hi, I am the handler.");
                try {
                    Result result = paymentService.createResult(request.getParameterMap());
                    Long customerId = Long.parseLong(result.getResponseMap().get(BLC_CID));
                    Long orderId = Long.parseLong(result.getResponseMap().get(BLC_OID));
                    String formTps = result.getResponseMap().get(BLC_TPS);
                    String tps = checkoutService.createTamperProofSeal(customerId, orderId);

                    if (result.isAuthorizeNet() && result.isApproved() && formTps.equals(tps)) {
                        responseBody = checkoutService.buildRelayResponse(authorizeNetConfirmUrl);
                    } else {
                        responseBody = checkoutService.buildRelayResponse(authorizeNetErrorUrl);
                    }

                } catch (NoSuchAlgorithmException e) {
                    LOG.fatal(e);
                } catch (InvalidKeyException e) {
                    LOG.fatal(e);
                }


                response.getWriter().println(responseBody);
                ((Request)request).setHandled(true);
            }
        };

        //Start an embedded Jetty Server
        Server server = new Server(Integer.parseInt(authorizeNetJettyIntegrationPort));
        server.setHandler(handler);
        server.start();
        System.out.println("Jetty server started.");
        this.wait();

        if (LOG.isDebugEnabled()) {
            LOG.debug("*** Initiating testSuccessfulAuthorizenetAuthorizeAndDebit ***");
        }


        /*
         *  authorizeAndDebit
         */
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
        HttpParams params = httpClient.getParams();
        HttpClientParams.setRedirecting(params, false);

        try {
            HttpPost httpPost = new HttpPost(paymentService.getGatewayRequest().getServerUrl());
            Map<String, String> fields = checkoutService.constructAuthorizeAndDebitFields(order);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("blc_cid", fields.get("blc_cid")));
            nameValuePairs.add(new BasicNameValuePair("blc_oid", fields.get("blc_oid")));
            nameValuePairs.add(new BasicNameValuePair("blc_tps", fields.get("blc_tps")));
            nameValuePairs.add(new BasicNameValuePair("x_invoice_num", fields.get("x_invoice_num")));
            nameValuePairs.add(new BasicNameValuePair("x_relay_url", fields.get("x_relay_url")));
            nameValuePairs.add(new BasicNameValuePair("x_login", fields.get("x_login")));
            nameValuePairs.add(new BasicNameValuePair("x_fp_sequence",fields.get("x_fp_sequence")));
            nameValuePairs.add(new BasicNameValuePair("x_fp_timestamp",fields.get("x_fp_timestamp")));
            nameValuePairs.add(new BasicNameValuePair("x_fp_hash",fields.get("x_fp_hash")));
            nameValuePairs.add(new BasicNameValuePair("x_version",fields.get("x_version")));
            nameValuePairs.add(new BasicNameValuePair("x_method",fields.get("x_method")));
            nameValuePairs.add(new BasicNameValuePair("x_type",fields.get("x_type")));
            nameValuePairs.add(new BasicNameValuePair("x_amount", fields.get("x_amount") ));
            nameValuePairs.add(new BasicNameValuePair("x_test_request", fields.get("x_test_request")));

            nameValuePairs.add(new BasicNameValuePair("x_first_name", "Michael"));
            nameValuePairs.add(new BasicNameValuePair("x_last_name", "Jordan"));
            nameValuePairs.add(new BasicNameValuePair("x_phone", "2124567890"));
            nameValuePairs.add(new BasicNameValuePair("x_address", "123 Test Dr."));
            nameValuePairs.add(new BasicNameValuePair("x_city", "Austin"));
            nameValuePairs.add(new BasicNameValuePair("x_state", "TX"));
            nameValuePairs.add(new BasicNameValuePair("x_country", "US"));
            nameValuePairs.add(new BasicNameValuePair("x_zip", "78704"));
            nameValuePairs.add(new BasicNameValuePair("x_email", "michael.jordan@broadleafcommerce.org"));
            nameValuePairs.add(new BasicNameValuePair("x_card_num", "4007000000027"));
            nameValuePairs.add(new BasicNameValuePair("x_exp_date", "0119"));

            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            if (LOG.isDebugEnabled()) {
                LOG.debug("executing request " + httpPost.getURI());
            }

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = httpClient.execute(httpPost, responseHandler);
            if (LOG.isDebugEnabled()) {
                LOG.debug("----------------------------------------");
                LOG.debug(responseBody);
                LOG.debug("----------------------------------------");
            }

            System.out.println("Post sent.");
            System.out.println(responseBody);
            assert (responseBody.contains(authorizeNetConfirmUrl));

        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpClient.getConnectionManager().shutdown();
            server.stop();
        }
    }

    private String requestParamToString(HttpServletRequest request) {
        StringBuffer requestMap = new StringBuffer();
        for (String key : (Set<String>)request.getParameterMap().keySet()) {
            requestMap.append(key + ": " + request.getParameter(key) + ", ");
        }
        return requestMap.toString();
    }

}
