(ns district-registry.server.contract.district-registry
  (:require [district-registry.server.contract.registry :as registry]))

(def registry-entry-event (partial registry/registry-entry-event [:district-registry :district-registry-fwd]))
(def registry-entry-event-in-tx (partial registry/registry-entry-event-in-tx [:district-registry :district-registry-fwd]))


