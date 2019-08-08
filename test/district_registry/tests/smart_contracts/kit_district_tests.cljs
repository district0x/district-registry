(ns district-registry.tests.smart-contracts.kit-district-tests
  (:require
    [bignumber.core :as bn]
    [cljs-promises.async :refer-macros [<?]]
    [cljs-web3.eth :as web3-eth]
    [cljs.test :refer-macros [deftest is testing use-fixtures async]]
    [clojure.core.async :refer [<! go]]
    [district-registry.server.contract.ds-auth :as ds-auth]
    [district-registry.server.contract.kit-district :as kit-district]
    [district-registry.shared.utils :refer [reg-entry-status->kw]]
    [district.server.web3 :refer [web3]]
    [district.web3-utils :refer [eth->wei-number eth->wei]]
    [print.foo :include-macros true :refer [look]]))


(deftest set-apps-parameters
  (async done
    (go
      (let [[owner non-owner] (web3-eth/accounts @web3)]
        (is (= owner (<? (ds-auth/owner :kit-district))))

        (testing "Non owner is not allowed to set voting-support-required-pct"
          (is (nil? (try (<? (kit-district/set-voting-support-required-pct 55e17 {:from non-owner}))
                         (catch :default)))))

        (testing "Owner is allowed to set voting-support-required-pct"
          (<? (kit-district/set-voting-support-required-pct 56e17 {:from owner}))
          (is (= 56e17 (bn/number (<? (kit-district/voting-support-required-pct))))))

        (testing "Non owner is not allowed to set voting-min-accept-quorum-pct"
          (is (nil? (try (<? (kit-district/set-voting-min-accept-quorum-pct 55e17 {:from non-owner}))
                         (catch :default)))))

        (testing "Owner is allowed to set voting-min-accept-quorum-pct"
          (<? (kit-district/set-voting-min-accept-quorum-pct 56e17 {:from owner}))
          (is (= 56e17 (bn/number (<? (kit-district/voting-min-accept-quorum-pct))))))


        (testing "Non owner is not allowed to set voting-vote-time"
          (is (nil? (try (<? (kit-district/set-voting-vote-time 110000 {:from non-owner}))
                         (catch :default)))))

        (testing "Owner is allowed to set voting-vote-time"
          (<? (kit-district/set-voting-vote-time 172800 {:from owner}))
          (is (= 172800 (bn/number (<? (kit-district/voting-vote-time))))))

        (testing "Non owner is not allowed to set finance-period-duration"
          (is (nil? (try (<? (kit-district/set-finance-period-duration 100000 {:from non-owner}))
                         (catch :default)))))

        (testing "Owner is allowed to set finance-period-duration"
          (<? (kit-district/set-finance-period-duration 432000 {:from owner}))
          (is (= 432000 (bn/number (<? (kit-district/finance-period-duration))))))

        (done)))))


(deftest set-apps-included
  (async done
    (go
      (let [[owner non-owner] (web3-eth/accounts @web3)]

        (testing "Non owner is not allowed to set app included"
          (is (nil? (try (<? (kit-district/set-app-included {:app :finance :included? false}
                                                            {:from non-owner}))
                         (catch :default)))))


        (testing "Owner is allowed to set app included"
          (<? (kit-district/set-app-included {:app :finance :included? false}
                                             {:from owner}))

          (is (false? (<? (kit-district/app-included? :finance)))))


        (testing "Non owner is not allowed to set app included"
          (is (nil? (try (<? (kit-district/set-apps-included {:finance false
                                                              :vault false
                                                              :voting false}
                                                             {:from non-owner}))
                         (catch :default)))))


        (testing "Owner is not allowed to set app included"
          (<? (kit-district/set-apps-included {:finance true
                                               :vault false
                                               :voting false}
                                              {:from owner})))

        (is (false? (<? (kit-district/app-included? :voting))))
        (is (false? (<? (kit-district/app-included? :vault))))
        (is (true? (<? (kit-district/app-included? :finance))))

        (done)))))