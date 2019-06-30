package com.revolut.money.transfer.api.configuration.component;

import com.revolut.money.transfer.api.configuration.module.ControllerModule;
import com.revolut.money.transfer.api.configuration.module.RepositoryModule;
import com.revolut.money.transfer.api.configuration.module.ValidationModule;
import com.revolut.money.transfer.api.controller.AccountController;
import com.revolut.money.transfer.api.controller.TransactionController;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = {
    ControllerModule.class,
    RepositoryModule.class,
    ValidationModule.class,
})
public interface ApplicationComponent {
  AccountController accountController();

  TransactionController transactionController();
}
