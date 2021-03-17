(ns district-registry.ui.components.app-layout
  (:require
   [district-registry.ui.components.nav :as nav]
   [district-registry.ui.subs :as dr-subs]
   [district.ui.component.active-account-balance :refer [active-account-balance]]
   [district.ui.component.form.input :as inputs :refer [text-input*]]
   [district.ui.router.events]
   [district.ui.router.subs :as router-subs]
   [district.ui.router.utils :as router-utils]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]))

(defn header [active-page-name]
  [:header#globalHeader
   [:div.container
    (nav/div {:class "logo sized"
              :route [:route/home]}
      [:img {:src "/images/registry-logo@2x.png"}])
    [:nav.toplinks
     [:ul
      [:li {:class (when (= active-page-name :route/submit)
                     "on")}
       [nav/a {:route [:route/submit]}
        "Submit"]]
      [:li
       {:class (when (= active-page-name :route/about)
                 "on")}
       [nav/a {:route [:route/about]}
        "About"]]]]
    [:div.dnt-wrap
     [:div.total-dnt
      [active-account-balance
       {:token-code :DNT
        :contract :DNT
        :locale "en-US"}]]
     [nav/a {:route [:route/my-account {:tab "activity"}]}
      [:div.select-menu
       [:div.select-choice.cta-btn.my-account-btn
        [:div.select-text "My Account"]]]]]]])

(defn footer []
  [:footer#globalFooter
   [:div.bg-wrap
    [:div.background.sized
     [:img {:src "/images/blobbg-bot@2x.png"}]]]
   [:div.container
    [:div.logo.sized [:img {:src "/images/registry-logo@2x.png"}]]
    [:div.row.spaced
     [:div.col
      [:p "A network of decentralized markets and communities. Create, operate, and govern. Powered by Ethereum, Aragon, and IPFS."]
      [:br]
      [:p.cookies-note "We use cookies to improve your experience on our website. By continuing to use this website, you agree to the use of cookies. To learn more about how we use cookies, please see our " [nav/a {:route [:route/privacy-policy]} "Cookie Policy."]]]
     [:div.col
      [:nav.footerlinks
       [:ul
        [:li [:a {:href "https://blog.district0x.io" :target :_blank} "Blog"]]
        [:li [:a {:href "https://district0x.io/team/" :target :_blank} "Team"]]
        [:li [:a {:href "https://district0x.io/transparency/" :target :_blank} "Transparency"]]
        [:li [:a {:href "https://district0x.io/faq/" :target :_blank} "FAQ"]]
        [:li [nav/a {:route [:route/terms]} "Terms Of Use"]]
        [:li [nav/a {:route [:route/privacy-policy]} "Privacy Policy"]]]]]
     [:div.col
      [:a.cta-btn.has-icon
       {:href "https://discord.com/invite/sS2AWYm"
        :target :_blank}
       [:span "Join Us On Discord"]
       [:span.icon-discord]]]
     [:div.col
      [:nav.social
       [:ul
        [:li [:a {:href "https://www.facebook.com/district0x/"
                  :target :_blank}
              [:span.icon-facebook]]]
        [:li [:a {:href "https://www.reddit.com/r/district0x"
                  :target :_blank}
              [:span.icon-reddit-alien]]]
        [:li [:a {:href "https://t.me/district0x"
                  :target :_blank}
              [:span.icon-telegram]]]
        [:li [:a {:href "https://twitter.com/district0x"
                  :target :_blank}
              [:span.icon-twitter]]]
        [:li [:a {:href "https://blog.district0x.io"
                  :target :_blank}
              [:span.icon-medium]]]
        [:li [:a {:href "https://github.com/district0x"
                  :target :_blank}
              [:span.icon-github]]]]]]]]])

(defn app-layout []
  (let [active-page (subscribe [::router-subs/active-page])]
    (fn [& children]
      [:div {:id (case (:name @active-page)
                   :route/about "page-about"
                   :route/detail "page-details"
                   :route/home "page-registry"
                   :route/submit "page-submit"
                   :route/edit "page-submit"
                   :route/my-account "page-my-account"
                   :route/privacy-policy "page-privacy-policy"
                   :route/terms "page-terms"
                   :route/not-found "not-found")}
       [header (:name @active-page)]
       (into [:div#page-content]
         children)
       [footer]])))
