blc-authorizenet
=============

Broadleaf Commerce currently offers integration with Authorize.net using the following two methods.

- Direct Post Integration Method for silent post/transparent redirect and reduced PCI compliance for AUTH_ONLY and AUTH_CAPTURE.
- Advanced Integration Method for more advanced transaction methods of PRIOR_AUTH_CAPTURE, VOID, and REFUND and increased PCI compliance for AUTH_ONLY and AUTH_CAPTURE.

> Note: As per the Authorize.net docs on AIM, transactions submitted to the test environment using a developer test account are not submitted to financial institutions for authorization and are not stored in the Merchant Interface.

For instruction on how to integrate this module into your Broadleaf project, please follow our integration guide here: http://docs.broadleafcommerce.org/current/Authorize.net-Module.html

Authorize.net Links and Resources:

- Home: http://www.authorize.net/
- Merchant Interface: https://sandbox.authorize.net/
- Sales: http://www.authorize.net/signupnow or 1-888-323-4289
- Support: https://support.authorize.net or 1-877-447-3938
- Merchant Integration Guide: http://www.authorize.net/support/merchant/wwhelp/wwhimpl/js/html/wwhelp.htm

List of Authorize.net DPM Links and Resources:

- http://developer.authorize.net/api/dpm/
- http://developer.authorize.net/guides/DPM/wwhelp/wwhimpl/js/html/wwhelp.htm
- http://developer.authorize.net/integration/fifteenminutes/java/#directpost

List of Authorize.net AIM Links and Resources:

- http://developer.authorize.net/api/aim/
- http://developer.authorize.net/guides/AIM/wwhelp/wwhimpl/js/html/wwhelp.htm
- http://developer.authorize.net/integration/fifteenminutes/java/#custom

### Example Response
For convenience, here are some example responses from the Gateway

Server to Server Authorization Request
```text
x_tran_key=6Pv4WeXF5n3M2t33&x_allow_partial_Auth=FALSE&x_trans_id=0&x_method=CC&x_card_num=XXXX1111&x_delim_data=TRUE&x_exp_date=42016&x_relay_response=FALSE&x_login=4UDq5H4sg&x_auth_code=000000&x_version=3.1&x_amount=11.99&x_type=AUTH_CAPTURE&x_test_request=TRUE&x_delim_char=%7C&x_encap_char=&masterpass_payPassWalletIndicator=101&masterpass_contact.emailAddress=joe.test%40email.com&CARD_TYPE=Visa&masterpass_card.brandId=visa&masterpass_contact.lastName=Test&LAST_FOUR=1111&masterpass_contact.country=US&masterpass_contact.phoneNumber=1-9876543210&masterpass_card.brandName=Visa&masterpass_contact.firstName=JOE&NAME_ON_CARD=Joe+Test&EXP_DATE=4%2F2016&masterpass_transactionId=a4a6x55a2m6wrhvqzkpyr1hw7vqj3jjva4&blc_oid=1
```

