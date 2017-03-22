package org.broadleafcommerce.vendor.authorizenet.web.expression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.web.expression.BroadleafVariableExpression;
import org.broadleafcommerce.payment.service.gateway.AuthorizeNetConfiguration;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;

/**
 * <p>A Thymeleaf Variable Expression implementation for Authorize.net specific fields.</p>
 *
 * <p>For example, to generate the client-side token for the payment nonce retrieved by the Client-Side SDK:</p>
 * <pre><code>
 * <script th:inline="javascript">
 * ... CDATA ...
 *    authData.clientKey = [[${#authorizenet.getClientKey()}]];
 *    authData.apiLoginID = [[${#authorizenet.getApiLoginId()}]];
 * ...
 * </script>
 * </code></pre>
 *
 * @author Elbert Bautista (elbertbautista)
 */
@Service("blAuthorizeNetVariableExpression")
public class AuthorizeNetVariableExpression implements BroadleafVariableExpression {

    protected static final Log LOG = LogFactory.getLog(AuthorizeNetVariableExpression.class);

    @Resource(name = "blAuthorizeNetConfiguration")
    protected AuthorizeNetConfiguration configuration;

    @Override
    public String getName() {
        return "authorizenet";
    }

    public String getClientKey() {
        return configuration.getClientKey();
    }

    public String getApiLoginId() {
        return configuration.getLoginId();
    }

}

