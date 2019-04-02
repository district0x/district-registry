(ns district-registry.ui.not-found.page
  (:require
   [district-registry.ui.components.app-layout :refer [app-layout]]
   [district.ui.component.page :refer [page]]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [reagent.core :as r]))

(defn not-found []
  [:div
   [:img {:src "/images/404.svg"
          :style {:height "400px"
                  :left "0"
                  :margin "0 auto"
                  :position "absolute"
                  :right "0"
                  :top "0"
                  :z-index "2"}}]
   [:section#main
    [:div.container
     [:div.box-wrap {:style {:height "700px"
                             :padding-top "300px"
                             :text-align "center"}}
      [:h1 "Error"]
      [:h1 {:style {:font-size "165px"}}
       "404"]
      [:p.intro-text "We searched high and low, but couldn't find what you're looking for."]
      [:p.intro-text "Let's find a better place for you to go."]
      [:br]
      [:button.cta-btn {:on-click #(dispatch [:district.ui.router.events/navigate :route/home])}
       "Home"]]]]])

(defmethod page :route/not-found []
  [app-layout
   [not-found]])
