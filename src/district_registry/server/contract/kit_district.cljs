(ns district-registry.server.contract.kit-district
  (:require [district-registry.shared.utils :refer [kit-district-app->num]]
            [district.server.smart-contracts :as smart-contracts]))

(defn set-voting-support-required-pct [voting-support-required-pct & [opts]]
  (smart-contracts/contract-send :kit-district :set-voting-support-required-pct [(str voting-support-required-pct)] (merge {:gas 1000000} opts)))

(defn set-voting-min-accept-quorum-pct [voting-min-accept-quorum-pct & [opts]]
  (smart-contracts/contract-send :kit-district :set-voting-min-accept-quorum-pct [(str voting-min-accept-quorum-pct)] (merge {:gas 1000000} opts)))

(defn set-voting-vote-time [voting-vote-time & [opts]]
  (smart-contracts/contract-send :kit-district :set-voting-vote-time [(str voting-vote-time)] (merge {:gas 1000000} opts)))

(defn set-finance-period-duration [finance-period-duration & [opts]]
  (smart-contracts/contract-send :kit-district :set-finance-period-duration [(str finance-period-duration)] (merge {:gas 1000000} opts)))

(defn set-apps-included [apps-included & [opts]]
  (smart-contracts/contract-send :kit-district :set-apps-included [(map #(-> % kit-district-app->num str) (keys apps-included)) (vals apps-included)] (merge {:gas 2000000} opts)))

(defn set-app-included [{:keys [:app :included?]} & [opts]]
  (smart-contracts/contract-send :kit-district :set-app-included [(str (kit-district-app->num app)) included?] (merge {:gas 1000000} opts)))

(defn app-included? [app]
  (smart-contracts/contract-call :kit-district :is-app-included [(kit-district-app->num app)]))

(defn voting-support-required-pct []
  (smart-contracts/contract-call :kit-district :voting-support-required-pct))

(defn voting-min-accept-quorum-pct []
  (smart-contracts/contract-call :kit-district :voting-min-accept-quorum-pct))

(defn voting-vote-time []
  (smart-contracts/contract-call :kit-district :voting-vote-time))

(defn finance-period-duration []
  (smart-contracts/contract-call :kit-district :finance-period-duration))
