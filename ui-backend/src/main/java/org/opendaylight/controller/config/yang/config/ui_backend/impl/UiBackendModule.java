package org.opendaylight.controller.config.yang.config.ui_backend.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.ui.backend.UiBackendServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class UiBackendModule extends org.opendaylight.controller.config.yang.config.ui_backend.impl.AbstractUiBackendModule {

    private static final Logger LOG = LoggerFactory.getLogger(UiBackendModule.class);

    public UiBackendModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public UiBackendModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.ui_backend.impl.UiBackendModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        DataBroker dataProvider = Preconditions.checkNotNull(getDataBrokerDependency());
        final UiBackendServiceImpl pgnApplicationService = new UiBackendServiceImpl(dataProvider,
                getRpcRegistryDependency());
        LOG.info("ui-backend started.");

        return new AutoCloseable() {

            @Override
            public void close() throws Exception {
                pgnApplicationService.close();
            }
        };
    }

}