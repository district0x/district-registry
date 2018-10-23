(ns district-registry.ui.components.app-layout
  (:require
   [district-registry.ui.subs :as dr-subs]
   [district-registry.ui.utils :as dr-utils]
   [district.ui.component.active-account :refer [active-account]]
   [district.ui.component.active-account-balance :refer [active-account-balance] :as account-balances]
   [district.ui.component.form.input :as inputs :refer [text-input*]]
   [district.ui.router.events]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]))

(defn app-layout []
  (let [active-page (subscribe [::dr-subs/active-page])]
    (fn [& children]
      [:div.app-container
       [:div.app-menu
        [:div.menu-content
         [:div.dr-logo {:on-click #(dispatch [:district.ui.router.events/navigate :route/home {}])}
          [:span "District Registry"]]
         [:div {:on-click #(dispatch [:district.ui.router.events/navigate :route/submit {}])}
          "Submit"]
         [:div {:on-click #(dispatch [:district.ui.router.events/navigate :route/about {}])}
          "About"]
         [active-account-balance
          {:token-code :DNT
           :contract :DNT
           :locale "en-US"}]
         [active-account]]]
       [:div.app-content
        (into [:div.main-content]
          children)
        [:div.footer "FOOTER"]]])))
