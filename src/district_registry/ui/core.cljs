(ns district-registry.ui.core
  (:require
    [akiroz.re-frame.storage :as storage]
    [cljs.spec.alpha :as s]
    [cljsjs.recharts]
    [day8.re-frame.async-flow-fx]
    [district-registry.shared.graphql-schema :refer [graphql-schema]]
    [district-registry.shared.routes :refer [routes]]
    [district-registry.ui.about.page]
    [district-registry.ui.config :as config]
    [district-registry.ui.detail.page]
    [district-registry.ui.edit.page]
    [district-registry.ui.events :as events]
    [district-registry.ui.home.page]
    [district-registry.ui.my-account.page]
    [district-registry.ui.not-found.page]
    [district-registry.ui.submit.page]
    [district.cljs-utils :as cljs-utils]
    [district.ui.component.router :refer [router]]
    [district.ui.graphql]
    [district.ui.ipfs]
    [district.ui.notification]
    [district.ui.now]
    [district.ui.reagent-render]
    [district.ui.router-google-analytics]
    [district.ui.router.effects :as router-effects]
    [district.ui.router]
    [district.ui.smart-contracts.events :as contracts-events]
    [district.ui.smart-contracts]
    [district.ui.web3-account-balances]
    [district.ui.web3-accounts.events :as web3-accounts-events]
    [district.ui.web3-accounts]
    [district.ui.web3-balances]
    [district.ui.web3-tx-id]
    [district.ui.web3-tx-log]
    [district.ui.web3-tx]
    [district.ui.web3]
    [district.ui.window-size]
    [mount.core :as mount]
    [print.foo :include-macros true]
    [re-frame.core :as re-frame]))

(storage/reg-co-fx!
  :district-registry                                        ;; local storage key
  {:fx :store                                               ;; re-frame fx ID
   :cofx :store})                                           ;; re-frame cofx ID

(defn dev-setup! []
  (when (:debug? config/config-map)
    (s/check-asserts true)
    (enable-console-print!)))

(def interceptors [re-frame/trim-v])

(re-frame/reg-event-fx
  ::my-account-route-active
  interceptors
  (fn [{:keys [:db]}]
    {:async-flow {:first-dispatch [::events/load-email-settings]
                  :rules [{:when :seen-all-of?
                           :events [::web3-accounts-events/active-account-changed
                                    ::contracts-events/contracts-loaded]
                           :dispatch [::events/load-email-settings]}]}}))


(re-frame/reg-event-fx
  ::init
  [(re-frame/inject-cofx :store) interceptors]
  (fn [{:keys [:db :store]}]
    {:db (-> db
           (assoc :district-registry.ui.my-account (:district-registry.ui.my-account store))
           (assoc :district-registry.ui.core/votes (:district-registry.ui.core/votes store)))
     ::router-effects/watch-active-page [{:id :route/my-account
                                          :name :route/my-account
                                          :params {:tab "email"}
                                          :dispatch [::my-account-route-active]}]}))

(defn ^:export init []
  (dev-setup!)
  (let [full-config (cljs-utils/merge-in
                      config/config-map
                      {:smart-contracts {:format :truffle-json}
                       :web3-account-balances {:for-contracts [:ETH :DNT]}
                       :web3-tx-log {:tx-costs-currencies [:USD]}
                       :reagent-render {:id "app"
                                        :component-var #'router}
                       :router {:routes routes
                                :default-route :route/not-found}
                       :notification {:default-show-duration 3000
                                      :default-hide-duration 1000}})]

    (js/console.log "config:" (clj->js full-config))
    (-> (mount/with-args full-config)
      (mount/start))
    (re-frame/dispatch-sync [::init])))
