(ns district-registry.ui.home.page
  (:require
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.subs :as gql]
    [district-registry.ui.home.events]
    [re-frame.core :refer [subscribe]]))


(defmethod page :route/home []
  (fn []
    [:div "Hello"]))