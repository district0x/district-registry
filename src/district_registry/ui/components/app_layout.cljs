(ns district-registry.ui.components.app-layout
  (:require
   [district-registry.ui.subs :as dr-subs]
   [district-registry.ui.utils :as dr-utils]
   [district.ui.component.active-account :refer [active-account]]
   [district.ui.component.active-account-balance :refer [active-account-balance] :as account-balances]
   [district.ui.component.form.input :as inputs :refer [text-input*]]
   [district.ui.router.events]
   [district.ui.router.subs :as router-subs]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]))

(defn header [active-page-name]
  [:header#globalHeader
   [:div.container
    [:div.logo.sized
     {:on-click #(dispatch [:district.ui.router.events/navigate :route/home {}])}
     [:img {:src "images/registry-logo@2x.png"}]]
    [:nav.toplinks
     [:ul
      [:li
       {:class (when (= active-page-name :route/submit)
                 "on")}
       [:a
        {:on-click #(dispatch [:district.ui.router.events/navigate :route/submit {}])}
        "Submit"]]
      [:li
       {:class (when (= active-page-name :route/about)
                 "on")}
       [:a
        {:on-click #(dispatch [:district.ui.router.events/navigate :route/about {}])}
        "About"]]]]
    [:div.dnt-wrap
     [:div.total-dnt
      [active-account-balance
       {:token-code :DNT
        :contract :DNT
        :locale "en-US"}]]
     [:div.select-menu
      [:div.select-choice.cta-btn
       [:div.select-text [active-account]]]]]]])

(defn footer []
  [:footer#globalFooter
   [:div.bg-wrap
    [:div.background.sized
     [:img {:src "images/blobbg-bot@2x.png"}]]]
   [:div.container
    [:div.logo.sized [:img {:src "images/registry-logo@2x.png"}]]
    [:div.row.spaced
     [:div.col
      [:p
       "A network of decentralized markets and communities. Create, operate, and govern. Powered by Ethereum, Aragon, and IPFS."]]
     [:div.col
      [:nav.footerlinks
       [:ul
        [:li [:a "Blog"]]
        [:li [:a "Team"]]
        [:li [:a "Transparency"]]
        [:li [:a "FAQ"]]]]]
     [:div.col
      [:a.cta-btn.hasIcon
       [:span "Join Us On Rocketchat"]]]
     [:div.col
      [:nav.social
       [:ul
        [:li [:a {:href "https://www.reddit.com/r/district0x"
                  :target "_blank"}
              [:span {:class "icon-reddit-alien"}]]]
        [:li [:a {:href "..."
                  :target "_blank"}
              [:span  {:class "icon-slack"}]]]
        [:li [:a {:href "https://twitter.com/district0x"
                  :target "_blank"}
              [:span {:class "icon-twitter"}]]]
        [:li [:a {:href "https://blog.district0x.io"
                  :target "_blank"}
              [:span {:class "icon-medium"}]
              ]]
        [:li [:a {:href "https://github.com/district0x"
                  :target "_blank"}
              [:span {:class "icon-github"}]]]]]]]]])

(defn app-layout []
  (let [active-page (subscribe [::router-subs/active-page])]
    (fn [& children]
      [:div {:id (case (:name @active-page)
                   :route/about "page-about"
                   :route/detail "page-details"
                   :route/home "page-registry"
                   :route/submit "page-submit"
                   :route/not-found "not-found")}
       [header (:name @active-page)]
       (into [:div]
         children)
       [footer]])))
