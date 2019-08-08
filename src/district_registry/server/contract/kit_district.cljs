(ns district-registry.server.contract.kit-district
  (:require
    [district.server.smart-contracts :refer [contract-call instance contract-address]]
    [district-registry.shared.utils :refer [kit-district-app->num]]))

(defn set-voting-support-required-pct [voting-support-required-pct & [opts]]
  (contract-call :kit-district :set-voting-support-required-pct [voting-support-required-pct] (merge {:gas 1000000} opts)))


(defn set-voting-min-accept-quorum-pct [voting-min-accept-quorum-pct & [opts]]
  (contract-call :kit-district :set-voting-min-accept-quorum-pct [voting-min-accept-quorum-pct] (merge {:gas 1000000} opts)))


(defn set-voting-vote-time [voting-vote-time & [opts]]
  (contract-call :kit-district :set-voting-vote-time [voting-vote-time] (merge {:gas 1000000} opts)))


(defn set-finance-period-duration [finance-period-duration & [opts]]
  (contract-call :kit-district :set-finance-period-duration [finance-period-duration] (merge {:gas 1000000} opts)))


(defn set-apps-included [apps-included & [opts]]
  (contract-call :kit-district :set-apps-included [(map kit-district-app->num (keys apps-included)) (vals apps-included)] (merge {:gas 2000000} opts)))


(defn set-app-included [{:keys [:app :included?]} & [opts]]
  (contract-call :kit-district :set-app-included [(kit-district-app->num app) included?] (merge {:gas 1000000} opts)))


(defn app-included? [app]
  (contract-call :kit-district :is-app-included [(kit-district-app->num app)]))


(defn voting-support-required-pct []
  (contract-call :kit-district :voting-support-required-pct))


(defn voting-min-accept-quorum-pct []
  (contract-call :kit-district :voting-min-accept-quorum-pct))


(defn voting-vote-time []
  (contract-call :kit-district :voting-vote-time))


(defn finance-period-duration []
  (contract-call :kit-district :finance-period-duration))