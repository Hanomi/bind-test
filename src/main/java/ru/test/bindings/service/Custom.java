package ru.test.bindings.service;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import ru.test.bindings.adapter.TestAdapter;
import ru.test.bindings.model.expection.BindingException;
import ru.test.bindings.model.response.BindResponse;
import ru.test.bindings.utils.Log;
import weblogic.logging.LoggingHelper;

@Stateless
public class Custom {

    private static final Logger logger = LoggingHelper.getServerLogger();

    @Inject
    public TestAdapter testAdapter;

    public BindResponse getIt(String msisdn) {

        BindResponse response = new BindResponse();

        try {
            //Получение userId по номеру
            testAdapter.getUserId(response, msisdn);
            //Получение расшифровки для каждой привязки
            testAdapter.getProviders(response);

        } catch (BindingException e) {
            logger.log(Level.INFO, () -> Log.log("", "прерываем дальнейшую обработку запроса из-за ошибки",
                    "cause", e.getCause().getLocalizedMessage(), "message", e.getMessage()));
        }

        return response;
    }


}
