(ns district-registry.server.generator
  (:require
    [bignumber.core :as bn]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.evm :as web3-evm]
    [cljs-web3.utils :refer [js->cljkk camel-case]]
    [district.cljs-utils :refer [rand-str]]
    [district.server.config :refer [config]]
    [district.server.smart-contracts :refer [contract-address contract-call instance]]
    [district.server.web3 :refer [web3]]
    [district-registry.server.contract.dnt :as dank-token]
    [district-registry.server.contract.eternal-db :as eternal-db]
    [district-registry.server.contract.meme :as meme]
    [district-registry.server.contract.meme-factory :as meme-factory]
    [district-registry.server.contract.meme-registry :as meme-registry]
    [district-registry.server.contract.minime-token :as minime-token]
    [district-registry.server.contract.param-change :as param-change]
    [district-registry.server.contract.param-change-factory :as param-change-factory]
    [district-registry.server.contract.param-change-registry :as param-change-registry]
    [district-registry.server.contract.registry :as registry]
    [district-registry.server.contract.registry-entry :as registry-entry]
    [district-registry.server.deployer]
    [mount.core :as mount :refer [defstate]]))

(declare start)
(defstate ^{:on-reload :noop} generator :start (start (merge (:generator @config)
                                                             (:generator (mount/args)))))


(defn get-scenarios [{:keys [:accounts :use-accounts :items-per-account :scenarios]}]
  (when (and (pos? use-accounts)
             (pos? items-per-account)
             (seq scenarios))
    (let [accounts-repeated (flatten (for [x accounts]
                                       (repeat items-per-account x)))
          scenarios (map #(if (keyword? %) {:scenario-type %} %) scenarios)
          scenarios-repeated (take (* use-accounts items-per-account) (cycle scenarios))]
      (partition 2 (interleave accounts-repeated scenarios-repeated)))))


(defn generate-memes [{:keys [:accounts :memes/use-accounts :memes/items-per-account :memes/scenarios]}]
  (let [[max-total-supply max-auction-duration deposit commit-period-duration reveal-period-duration]
        (->> (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :max-auction-duration :deposit :commit-period-duration
                                                            :reveal-period-duration])
          (map bn/number))]
    (doseq [[account {:keys [:scenario-type]}] (get-scenarios {:accounts accounts
                                                               :use-accounts use-accounts
                                                               :items-per-account items-per-account
                                                               :scenarios scenarios})]
      (let [meta-hash "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH"
            total-supply (inc (rand-int max-total-supply))
            auction-duration (+ 60 (rand-int (- max-auction-duration 60)))]

        (let [tx-hash (meme-factory/approve-and-create-meme {:meta-hash meta-hash
                                                             :total-supply total-supply
                                                             :amount deposit}
                                                            {:from account})]

          (when-not (= :scenario/create scenario-type)
            (let [{{:keys [:registry-entry]} :args} (meme-registry/registry-entry-event-in-tx tx-hash)]
              (when-not registry-entry
                (throw (js/Error. "Registry Entry wasn't found")))

              (registry-entry/approve-and-create-challenge registry-entry
                                                           {:meta-hash meta-hash
                                                            :amount deposit}
                                                           {:from account})

              (when-not (= :scenario/challenge scenario-type)
                (let [{:keys [:reg-entry/creator]} (registry-entry/load-registry-entry registry-entry)
                      balance (dank-token/balance-of creator)]

                  (registry-entry/approve-and-commit-vote registry-entry
                                                          {:amount balance
                                                           :salt "abc"
                                                           :vote-option :vote.option/vote-for}
                                                          {:from creator})

                  (when-not (= :scenario/commit-votde scenario-type)
                    (web3-evm/increase-time! @web3 [(inc commit-period-duration)])

                    (registry-entry/reveal-vote registry-entry
                                                {:vote-option :vote.option/vote-for
                                                 :salt "abc"}
                                                {:from creator})

                    (when-not (= :scenario/reveal-vote scenario-type)
                      (web3-evm/increase-time! @web3 [(inc reveal-period-duration)])

                      (registry-entry/claim-vote-reward registry-entry {:from creator}))))))))))))


(defn generate-param-changes [{:keys [:accounts
                                      :param-changes/use-accounts
                                      :param-changes/items-per-account
                                      :param-changes/scenarios]}]
  (let [[deposit challenge-period-duration]
        (->> (eternal-db/get-uint-values :param-change-registry-db [:deposit :challenge-period-duration])
          (map bn/number))]
    (doseq [[account {:keys [:scenario-type :param-change-db]}]
            (get-scenarios {:accounts accounts
                            :use-accounts use-accounts
                            :items-per-account items-per-account
                            :scenarios scenarios})]

      (let [tx-hash (param-change-factory/approve-and-create-param-change
                      {:db (contract-address (or param-change-db :meme-registry-db))
                       :key :deposit
                       :value (web3/to-wei 800 :ether)
                       :amount deposit}
                      {:from account})

            {:keys [:registry-entry]} (:args (param-change-registry/registry-entry-event-in-tx tx-hash))]

        (when-not (= scenario-type :scenario/create)
          (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])

          (param-change-registry/apply-param-change registry-entry {:from account}))))))


(defn start [opts]
  (let [opts (assoc opts :accounts (web3-eth/accounts @web3))]
    (generate-memes opts)
    (generate-param-changes opts)))
