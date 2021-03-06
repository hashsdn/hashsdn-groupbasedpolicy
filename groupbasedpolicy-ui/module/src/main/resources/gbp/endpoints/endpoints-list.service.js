define([], function () {
    'use strict';

    angular.module('app.gbp').service('EndpointsListService', EndpointsListService);

    EndpointsListService.$inject = ['$filter', 'Restangular', 'EndpointService'];

    function EndpointsListService($filter, Restangular, EndpointService) {
        /* methods */
        this.createList = createList;

        function EndpointsList() {
            /* properties */
            this.data = [];

            /* methods */
            this.setData = setData;
            this.get = get;
            this.getByEpg = getByEpg;
            this.getByTenantId = getByTenantId;
            this.clearData = clearData;

            /* Implementation */
            /**
             * fills EndpointsList object with data
             * @param data
             */
            function setData(data) {
                var self = this;

                self.clearData();
                data && data.forEach(function (dataElement) {
                    self.data.push(EndpointService.createObject(dataElement));
                });
            }

            function clearData() {
                var self = this;
                self.data = [];
            }

            function get() {
                /* jshint validthis:true */
                var self = this;
                var restObj = Restangular.one('restconf').one('operational').one('base-endpoint:endpoints');

                return restObj.get().then(function (data) {
                    self.setData(data.endpoints['address-endpoints']['address-endpoint']);
                });
            }

            function getByEpg(epg, tenant, successCallback) {
                /* jshint validthis:true */
                var self = this;
                var restObj = Restangular.one('restconf').one('operational').one('base-endpoint:endpoints');

                return restObj.get().then(function (data) {
                    var endpoints = $filter('filter')(
                        data.endpoints['address-endpoints']['address-endpoint'].map(function(endpoint) {
                            return endpoint;
                        }),
                        function (ep) {
                            if (ep.tenant === tenant && ep['endpoint-group'].indexOf(epg.id) !== -1) {
                                return true;
                            }
                        }
                    );
                    self.setData(endpoints);

                    (successCallback || angular.noop)();
                });
            }

            function getByTenantId(rootTenant) {
                var self = this;
                var restObj = Restangular.one('restconf').one('operational').one('base-endpoint:endpoints');
                return restObj.get().then(function (data) {
                    var endpoints = $filter('filter')(data.endpoints['address-endpoints']['address-endpoint'].map(function(endpoint) {
                        return endpoint;
                    }), { 'tenant': rootTenant });
                    self.setData(endpoints);
                });
            }

        }

        function createList() {
            var obj = new EndpointsList();

            return obj;
        }
    }

    return EndpointsListService;
});
