(ns district-registry.ui.not-found.page
  (:require
   [district-registry.ui.components.app-layout :refer [app-layout]]
   [district.ui.component.page :refer [page]]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [reagent.core :as r]))

(defn not-found []
  [:div])

(defmethod page :route/not-found []
  [app-layout
   [not-found]])
