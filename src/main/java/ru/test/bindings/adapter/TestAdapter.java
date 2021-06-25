package ru.test.bindings.adapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import ru.test.bindings.client.SSLClient;
import ru.test.bindings.config.BindingConfig;
import ru.test.bindings.mapping.Mapper;
import ru.test.bindings.mapping.SomeMapper;
import ru.test.bindings.model.expection.BindingException;
import ru.test.bindings.model.request.Identity;
import ru.test.bindings.model.request.Provider;
import ru.test.bindings.model.request.TestProvider;
import ru.test.bindings.model.response.BindResponse;
import ru.test.bindings.model.response.Grant;
import ru.test.bindings.model.util.TestContext;
import ru.test.bindings.utils.Log;
import ru.test.bindings.utils.Util;
import weblogic.logging.LoggingHelper;

@Stateless
public class TestAdapter {

    private static final Logger logger = LoggingHelper.getServerLogger();
    private static final Mapper mapper = new Mapper();

    @Inject
    private SSLClient sslClient;
    @Inject
    private BindingConfig config;
    @Inject
    private TestContext testContext;

    public void getUserId(BindResponse response, String msisdn) throws BindingException {
        String url = config.getEwalletUrl() + "getWallet.do";

        try {
            logger.log(Level.INFO, () -> Log.log(testContext.getRowId(), "пробуем вызвать test",
                    "method", "getUserId", "msisdn", msisdn, "url", url));

            Response testResponse = sslClient.makeClient()
                    .target(url)
                    .queryParam("phone", msisdn)
                    .request()
                    .get();

            String body = testResponse.readEntity(String.class);

            if (testResponse.getStatus() == 200) {
                logger.log(Level.INFO, () -> Log.log(testContext.getRowId(), "получен ответ от test",
                        "method", "getUserId", "code", testResponse.getStatus(), "body", body));

                Identity identity = mapper.fromJson(body, Identity.class);

                response.setUserId(identity.getUserId());
                response.setErrorCode(identity.getErrorCode());
                response.setErrorMessage(identity.getErrorMessage());
                response.setErrorCause(identity.getErrorCause());
            } else {
                logger.log(Level.INFO, () -> Log.log("", "ошибка запроса к test",
                        "method", "getUserId", "code", testResponse.getStatus(), "body", body));

                response.setErrorCode(testResponse.getStatus());
                response.setErrorMessage(testResponse.getStatusInfo().getReasonPhrase());

                throw new BindingException(testResponse.getStatusInfo().getReasonPhrase());
            }
        } catch (BindingException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, e, () -> Log.log(testContext.getRowId(), "internal error"));

            response.setErrorCode(500);
            response.setErrorMessage("weblogic internal error");
            response.setErrorCause(e.getLocalizedMessage());

            throw new BindingException(e);
        }
    }

    public void getProviders(BindResponse response) throws BindingException {
        String url = config.getEwalletUrl() + "getProviderInfo.do";

        Map<Provider, String> hash = new HashMap<>();

        try {
            Set<Provider> providers = response.getGrants().stream()
                    .map(Grant::getProvider)
                    .collect(Collectors.toSet());

            for (Provider provider : providers) {
                logger.log(Level.INFO, () -> Log.log(testContext.getRowId(), "пробуем вызвать test",
                        "method", "getProviders",
                        "serviceProviderId", provider.getSpId(), "providerId", provider.getId(), "url", url));

                Response testResponse = sslClient.makeClient()
                        .target(url)
                        .queryParam("serviceProviderId", provider.getSpId())
                        .queryParam("providerId", provider.getId())
                        .request()
                        .get();

                String body = testResponse.readEntity(String.class);

                if (testResponse.getStatus() == 200) {
                    logger.log(Level.INFO, () -> Log.log(testContext.getRowId(), "получен ответ от test",
                            "method", "getProviders", "code", testResponse.getStatus(), "body", body));

                    TestProvider testProvider = mapper.fromJson(body, TestProvider.class);

                    response.setErrorCode(testProvider.getErrorCode());
                    response.setErrorMessage(testProvider.getErrorMessage());
                    response.setErrorCause(testProvider.getErrorCause());

                    if (Util.nonEmpty(testProvider.getName())) {
                        hash.put(provider, testProvider.getName());
                    }
                } else {
                    logger.log(Level.INFO, () -> Log.log("", "ошибка запроса к test",
                            "method", "getProviders", "code", testResponse.getStatus(), "body", body));

                    response.setErrorCode(testResponse.getStatus());
                    response.setErrorMessage(testResponse.getStatusInfo().getReasonPhrase());

                    throw new BindingException(testResponse.getStatusInfo().getReasonPhrase());
                }
            }
        } catch (BindingException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, e, () -> Log.log(testContext.getRowId(), "internal error"));

            response.setErrorCode(500);
            response.setErrorMessage("weblogic internal error");
            response.setErrorCause(e.getLocalizedMessage());

            throw new BindingException(e);
        }

        SomeMapper.provider(response, hash);
    }
}
