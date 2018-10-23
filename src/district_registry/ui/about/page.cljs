(ns district-registry.ui.about.page
  (:require
   [district-registry.ui.components.app-layout :refer [app-layout]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [re-frame.core :refer [subscribe]]))

(defmethod page :route/about []
  (fn []
    [app-layout
     [:div "Hello about"]]))
