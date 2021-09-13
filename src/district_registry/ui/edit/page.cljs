(ns district-registry.ui.edit.page
  (:require
    [district-registry.ui.components.app-layout :refer [app-layout]]
    [district-registry.ui.components.district-form :refer [district-form]]
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.subs :as gql]
    [district.ui.router.subs :as router-subs]
    [print.foo :refer [look] :include-macros true]
    [re-frame.core :refer [subscribe dispatch]]))


(defmethod page :route/edit []
  (let [params (subscribe [::router-subs/active-page-params])]
    (fn []
      (let [results @(subscribe [::gql/query
                                 {:queries [[:district {:reg-entry/address (:address @params)}
                                             [:reg-entry/address
                                              :reg-entry/version
                                              :reg-entry/creator
                                              :district/name
                                              :district/description
                                              :district/url
                                              :district/github-url
                                              :district/facebook-url
                                              :district/twitter-url
                                              :district/logo-image-hash
                                              :district/background-image-hash
                                              :district/ens-name]]]}])
            {:keys [:reg-entry/address
                    :district/name
                    :district/description
                    :district/url
                    :district/github-url
                    :district/logo-image-hash
                    :district/background-image-hash
                    :district/facebook-url
                    :district/twitter-url
                    :district/ens-name]} (:district results)]
        [app-layout
         (when (seq name)
           [district-form {:edit? true
                           :reg-entry/address address
                           :form-data {:name name
                                       :url url
                                       :github-url github-url
                                       :description description
                                       :facebook-url facebook-url
                                       :twitter-url twitter-url
                                       :district/logo-image-hash logo-image-hash
                                       :district/background-image-hash background-image-hash
                                       :ens-name ens-name}}])]))))
