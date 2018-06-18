(ns district-registry.tests.smart-contracts.deployment-tests
  (:require
    [bignumber.core :as bn]
    [cljs-time.coerce :refer [to-epoch from-long]]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.evm :as web3-evm]
    [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
    [district.server.config]
    [district.server.smart-contracts :refer [contract-address]]
    [district.server.web3 :refer [web3]]
    [district-registry.server.contract.meme :as meme]
    [district-registry.server.contract.meme-factory :as meme-factory]
    [district-registry.server.contract.param-change :as param-change]
    [district-registry.server.contract.param-change-factory :as param-change-factory]
    [district-registry.server.contract.registry :as registry]
    [district-registry.server.contract.registry-entry :as registry-entry]
    [district-registry.server.deployer]
    [district-registry.shared.smart-contracts :refer [smart-contracts]]
    [district-registry.tests.smart-contracts.utils :refer [now create-before-fixture after-fixture]]
    [print.foo :include-macros true]
    [district-registry.server.contract.ds-auth :as ds-auth]
    [district-registry.server.contract.minime-token :as minime-token]
    [district-registry.server.contract.ds-guard :as ds-guard]
    [district-registry.server.contract.dnt :as dank-token]
    [district.web3-utils :as web3-utils]))


(use-fixtures
  :each {:before (create-before-fixture)
         :after after-fixture})


(deftest deployment-tests
  (let [[addr0] (web3-eth/accounts @web3)]
       ))