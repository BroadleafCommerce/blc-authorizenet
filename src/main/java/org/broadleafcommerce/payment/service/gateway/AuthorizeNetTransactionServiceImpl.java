package org.broadleafcommerce.payment.service.gateway;

import java.math.BigDecimal;

import javax.annotation.Resource;

import net.authorize.AuthNetField;
import net.authorize.Environment;
import net.authorize.Merchant;
import net.authorize.ResponseField;
import net.authorize.TransactionType;
import net.authorize.aim.Result;
import net.authorize.aim.Transaction;
import net.authorize.data.creditcard.CreditCard;

import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayTransactionService;
import org.broadleafcommerce.common.time.SystemTime;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.vendor.authorizenet.service.payment.AuthorizeNetGatewayType;
import org.broadleafcommerce.vendor.authorizenet.service.payment.type.MessageConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;


@Service("blAuthorizeNetTransactionService")
public class AuthorizeNetTransactionServiceImpl implements PaymentGatewayTransactionService {

    @Resource(name = "blAuthorizeNetConfiguration")
    protected AuthorizeNetConfiguration configuration;

    @Override
    public PaymentResponseDTO authorize(PaymentRequestDTO paymentRequestDTO) throws PaymentException {
        return common(paymentRequestDTO, TransactionType.AUTH_ONLY);
    }

    @Override
    public PaymentResponseDTO capture(PaymentRequestDTO paymentRequestDTO) throws PaymentException {
        Assert.isTrue(paymentRequestDTO.getAdditionalFields().containsKey(ResponseField.TRANSACTION_ID.getFieldName()),
                "Must pass 'x_trans_id' value on the additionalFields of the Payment Request DTO");
        Assert.isTrue(paymentRequestDTO.getTransactionTotal() != null,
                "The Transaction Total must not be null on the Payment Request DTO");
        return common(paymentRequestDTO, TransactionType.PRIOR_AUTH_CAPTURE);
    }

    @Override
    public PaymentResponseDTO authorizeAndCapture(PaymentRequestDTO paymentRequestDTO) throws PaymentException {
        return common(paymentRequestDTO, TransactionType.AUTH_CAPTURE);
    }

    @Override
    public PaymentResponseDTO reverseAuthorize(PaymentRequestDTO paymentRequestDTO) throws PaymentException {
        return voidPayment(paymentRequestDTO);
    }

    @Override
    public PaymentResponseDTO refund(PaymentRequestDTO paymentRequestDTO) throws PaymentException {
        Assert.isTrue(paymentRequestDTO.getAdditionalFields().containsKey(AuthNetField.X_TRANS_ID.getFieldName()),
                "Must pass 'x_trans_id' value on the additionalFields of the Payment Request DTO");
        Assert.isTrue(paymentRequestDTO.getTransactionTotal() != null,
                "The Transaction Total must not be null on the Payment Request DTO");
        Boolean cardNumOrLastFourPopulated = paymentRequestDTO.creditCardPopulated() &&
                (paymentRequestDTO.getCreditCard().getCreditCardLastFour() != null || paymentRequestDTO.getCreditCard().getCreditCardNum() != null);
        Assert.isTrue(cardNumOrLastFourPopulated || (paymentRequestDTO.getAdditionalFields().get(AuthNetField.X_CARD_NUM.getFieldName()) != null),
                "Must pass the Last four card number digits on the credit card of the Payment Request DTO");
        return common(paymentRequestDTO, TransactionType.CREDIT);
    }

    @Override
    public PaymentResponseDTO voidPayment(PaymentRequestDTO paymentRequestDTO) throws PaymentException {
        Assert.isTrue(paymentRequestDTO.getAdditionalFields().containsKey(AuthNetField.X_TRANS_ID.getFieldName()),
                "Must pass 'x_trans_id' value on the additionalFields of the Payment Request DTO");
        return common(paymentRequestDTO, TransactionType.VOID);
    }
    
    private PaymentResponseDTO common(PaymentRequestDTO paymentRequestDTO, TransactionType transactionType) {
        Environment e = Environment.createEnvironment(configuration.getServerUrl(), configuration.getServerUrl().replace("test", "apitest").replace("secure", "api"));
        Merchant merchant = Merchant.createMerchant(e, configuration.getLoginId(), configuration.getTransactionKey());
        
        Transaction transaction = merchant.createAIMTransaction(transactionType, new BigDecimal(paymentRequestDTO.getTransactionTotal()));
        transaction.setMerchantDefinedField(MessageConstants.BLC_OID, paymentRequestDTO.getOrderId());
        for(String fieldKey : paymentRequestDTO.getAdditionalFields().keySet()) {
            transaction.setMerchantDefinedField(fieldKey, (String)paymentRequestDTO.getAdditionalFields().get(fieldKey));
        }
        transaction.setTransactionId((String)paymentRequestDTO.getAdditionalFields().get(AuthNetField.X_TRANS_ID.getFieldName()));
        
        if(transactionType.equals(TransactionType.AUTH_CAPTURE) || transactionType.equals(TransactionType.AUTH_ONLY)) {
            CreditCard creditCard = CreditCard.createCreditCard();
            creditCard.setCreditCardNumber(paymentRequestDTO.getCreditCard().getCreditCardNum());
            creditCard.setExpirationMonth(paymentRequestDTO.getCreditCard().getCreditCardExpMonth());
            creditCard.setExpirationYear(paymentRequestDTO.getCreditCard().getCreditCardExpYear());
            transaction.setCreditCard(creditCard);
        }
        if(transactionType.equals(TransactionType.CREDIT)) {
            String cardNumOrLastFour = null;
            if(paymentRequestDTO.creditCardPopulated()) {
                cardNumOrLastFour = paymentRequestDTO.getCreditCard().getCreditCardLastFour();
            }
            if(cardNumOrLastFour == null && ((String)paymentRequestDTO.getAdditionalFields().get(AuthNetField.X_CARD_NUM.getFieldName())).length() == 4) {
                cardNumOrLastFour = (String)paymentRequestDTO.getAdditionalFields().get(AuthNetField.X_CARD_NUM.getFieldName());
            }
            if(cardNumOrLastFour == null && paymentRequestDTO.creditCardPopulated()) {
                cardNumOrLastFour = paymentRequestDTO.getCreditCard().getCreditCardNum();
            }

            CreditCard creditCard = CreditCard.createCreditCard();
            creditCard.setCreditCardNumber(cardNumOrLastFour);
            transaction.setCreditCard(creditCard);
        }

        Result<Transaction> result = (Result<Transaction>)
          merchant.postTransaction(transaction);

        PaymentResponseDTO responseDTO = new PaymentResponseDTO(PaymentType.CREDIT_CARD, AuthorizeNetGatewayType.AUTHORIZENET);
        responseDTO.orderId(result.getTarget().getMerchantDefinedField(MessageConstants.BLC_OID));
        responseDTO.responseMap(MessageConstants.TRANSACTION_TIME, SystemTime.asDate().toString());
        responseDTO.responseMap(ResponseField.RESPONSE_CODE.getFieldName(), ""+result.getResponseCode().getCode());
        responseDTO.responseMap(ResponseField.RESPONSE_REASON_CODE.getFieldName(), ""+result.getReasonResponseCode().getResponseReasonCode());
        responseDTO.responseMap(ResponseField.RESPONSE_REASON_TEXT.getFieldName(), result.getResponseText());
        responseDTO.responseMap(ResponseField.TRANSACTION_TYPE.getFieldName(), result.getTarget().getTransactionType().getValue());
        responseDTO.successful(result.isApproved());
        if(result.isError()) {
            responseDTO.valid(false);
            responseDTO.completeCheckoutOnCallback(false);
        }
        
        for(String fieldKey : result.getTarget().getMerchantDefinedMap().keySet()) {
            responseDTO.responseMap(fieldKey, result.getTarget().getMerchantDefinedField(fieldKey));
        }
        
        return responseDTO;

    }

}
