(ns district-registry.tests.smart-contracts.deployment-tests
  (:require
    [bignumber.core :as bn]
    [cljs-time.coerce :refer [to-epoch from-long]]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
    [district.server.config]
    [district.server.smart-contracts :refer [contract-address]]
    [district.server.web3 :refer [web3]]
    [district.web3-utils :as web3-utils]
    [district.shared.async-helpers :refer [promise->]]
    [print.foo :include-macros true]

    ))

(deftest deployment-tests
  (testing "testing if deployment was succesfull"
    (let [[addr0] (web3-eth/accounts @web3)]
      (println "everything ok")


      #_ (async done
        (promise->
          #(done)))

      )))
