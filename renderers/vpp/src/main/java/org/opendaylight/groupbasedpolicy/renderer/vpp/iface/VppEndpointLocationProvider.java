/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.iface;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.CloseOnFailTransactionChain;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.ProviderName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProviderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class VppEndpointLocationProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(VppEndpointLocationProvider.class);
    public static final ProviderName VPP_ENDPOINT_LOCATION_PROVIDER =
            new ProviderName("VPP endpoint location provider");
    public static final long PROVIDER_PRIORITY = 10L;
    private final BindingTransactionChain txChain;

    public VppEndpointLocationProvider(DataBroker dataProvider) {
        LocationProvider locationProvider = new LocationProviderBuilder().setProvider(VPP_ENDPOINT_LOCATION_PROVIDER)
            .setPriority(PROVIDER_PRIORITY)
            .build();
        txChain = checkNotNull(dataProvider)
            .createTransactionChain(new CloseOnFailTransactionChain(VppEndpointLocationProvider.class.getSimpleName()));
        WriteTransaction wTx = txChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.locationProviderIid(VPP_ENDPOINT_LOCATION_PROVIDER),
                locationProvider, true);

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                LOG.debug("{} was created", VPP_ENDPOINT_LOCATION_PROVIDER.getValue());
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("{} was NOT created", VPP_ENDPOINT_LOCATION_PROVIDER.getValue());
            }
        });
    }

    public void createLocationForVppEndpoint(VppEndpoint vppEndpoint) {
        ProviderAddressEndpointLocation providerAddressEndpointLocation =
                createProviderAddressEndpointLocation(vppEndpoint);
        WriteTransaction wTx = txChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.providerAddressEndpointLocationIid(VPP_ENDPOINT_LOCATION_PROVIDER,
                        providerAddressEndpointLocation.getKey()),
                providerAddressEndpointLocation);
        LOG.debug("Creating location for {}", providerAddressEndpointLocation.getKey());
        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                LOG.debug("{} provided location: {}", VPP_ENDPOINT_LOCATION_PROVIDER.getValue(),
                        providerAddressEndpointLocation);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("{} failed to provide location: {}", VPP_ENDPOINT_LOCATION_PROVIDER.getValue(),
                        providerAddressEndpointLocation, t);
            }
        });
    }

    public static ProviderAddressEndpointLocation createProviderAddressEndpointLocation(VppEndpoint vppEndpoint) {
        String restIfacePath = VppPathMapper.interfaceToRestPath(vppEndpoint.getVppInterfaceName());
        AbsoluteLocation absoluteLocation = new AbsoluteLocationBuilder()
            .setLocationType(new ExternalLocationCaseBuilder().setExternalNodeMountPoint(vppEndpoint.getVppNodePath())
                .setExternalNodeConnector(restIfacePath)
                .build())
            .build();
        return new ProviderAddressEndpointLocationBuilder()
            .setKey(createProviderAddressEndpointLocationKey(vppEndpoint))
            .setAbsoluteLocation(absoluteLocation)
            .build();
    }

    public void deleteLocationForVppEndpoint(VppEndpoint vppEndpoint) {
        ProviderAddressEndpointLocationKey provAddrEpLocKey = createProviderAddressEndpointLocationKey(vppEndpoint);
        WriteTransaction wTx = txChain.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.CONFIGURATION,
                IidFactory.providerAddressEndpointLocationIid(VPP_ENDPOINT_LOCATION_PROVIDER, provAddrEpLocKey));
        LOG.debug("Deleting location for {}", provAddrEpLocKey);
        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                LOG.debug("{} removed location: {}", VPP_ENDPOINT_LOCATION_PROVIDER.getValue(), provAddrEpLocKey);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("{} failed to remove location: {}", VPP_ENDPOINT_LOCATION_PROVIDER.getValue(),
                        provAddrEpLocKey, t);
            }
        });
    }

    private static ProviderAddressEndpointLocationKey createProviderAddressEndpointLocationKey(
            VppEndpoint vppEndpoint) {
        return new ProviderAddressEndpointLocationKey(vppEndpoint.getAddress(), vppEndpoint.getAddressType(),
                vppEndpoint.getContextId(), vppEndpoint.getContextType());
    }

    public void replaceLocationForEndpoint(@Nonnull ExternalLocationCase location, @Nonnull AddressEndpointWithLocationKey addrEpWithLocKey) {
        ProviderAddressEndpointLocationKey provAddrEpLocKey =
                KeyFactory.providerAddressEndpointLocationKey(addrEpWithLocKey);
        AbsoluteLocation absoluteLocation =
                new AbsoluteLocationBuilder().setLocationType(location).build();
        ProviderAddressEndpointLocation providerAddressEndpointLocation = new ProviderAddressEndpointLocationBuilder()
            .setKey(provAddrEpLocKey).setAbsoluteLocation(absoluteLocation).build();
        WriteTransaction wTx = txChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.providerAddressEndpointLocationIid(VPP_ENDPOINT_LOCATION_PROVIDER,
                        providerAddressEndpointLocation.getKey()),
                providerAddressEndpointLocation);
        LOG.debug("Updating location for {}", provAddrEpLocKey);
        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                LOG.debug("{} replaced location: {}", VPP_ENDPOINT_LOCATION_PROVIDER.getValue(),
                        providerAddressEndpointLocation);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("{} failed to replace location: {}", VPP_ENDPOINT_LOCATION_PROVIDER.getValue(),
                        providerAddressEndpointLocation, t);
            }
        });
    }

    @Override
    public void close() throws Exception {
        WriteTransaction wTx = txChain.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.CONFIGURATION, IidFactory.locationProviderIid(VPP_ENDPOINT_LOCATION_PROVIDER));
        wTx.submit();
    }
}
