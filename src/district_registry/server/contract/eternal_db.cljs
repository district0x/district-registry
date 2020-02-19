(ns district-registry.server.contract.eternal-db
  (:require [camel-snake-kebab.core :as camel-snake-kebab]
            [cljs-web3-next.utils :as web3-utils]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]))

(defn get-uint-values [contract-key db-keys]
  (smart-contracts/contract-call contract-key
                                 :get-u-int-values
                                 [(->> db-keys
                                       (map camel-snake-kebab/->camelCaseString)
                                       (map #(web3-utils/solidity-sha3 @web3 %)))]))
