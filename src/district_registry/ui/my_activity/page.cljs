(ns district-registry.ui.my-activity.page
  (:require
    [bignumber.core :as bn]
    [clojure.pprint :refer [pprint]]
    [district-registry.ui.components.app-layout :refer [app-layout]]
    [district.ui.component.page :refer [page]]
    [district.ui.component.tx-log :refer [tx-log]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn my-activity []
  [:section#main
   [:div.container
    [:div.box-wrap
     [:div.body-text
      [:div.container
       [tx-log {:header-props {:text [:h1 "My Activity"]}
                :settings-el [:div]
                :tx-cost-currency :USD}]]]]]])


(defmethod page :route/my-activity []
  (let []
    (fn []
      [app-layout
       [my-activity]])))
