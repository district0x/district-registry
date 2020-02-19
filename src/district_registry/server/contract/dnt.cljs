(ns district-registry.server.contract.dnt
  (:require [district-registry.server.contract.minime-token :as minime-token]))

(def approve-and-call (partial minime-token/approve-and-call :DNT))
(def balance-of (partial minime-token/balance-of :DNT))
(def controller (partial minime-token/controller :DNT))
(def total-supply (partial minime-token/total-supply :DNT))
