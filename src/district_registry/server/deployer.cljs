(ns district-registry.server.deployer
  (:require
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [district.cljs-utils :refer [rand-str]]
   [district.server.config :refer [config]]
   [district.server.smart-contracts :refer [contract-event-in-tx contract-address contract-call deploy-smart-contract! instance write-smart-contracts!]]
   [district.server.web3 :refer [web3]]
   [district-registry.server.contract.dnt :as dnt]
   [district-registry.server.contract.ds-auth :as ds-auth]
   [district-registry.server.contract.ds-guard :as ds-guard]
   [district-registry.server.contract.eternal-db :as eternal-db]
   [district-registry.server.contract.registry :as registry]
   [mount.core :as mount :refer [defstate]]

   [district-registry.server.contract.district-factory :as district-factory]
   [district-registry.server.contract.district-registry :as district-registry]
   [district-registry.server.contract.district :as district]
   [district-registry.server.contract.registry-entry :as registry-entry]
   [cljs-web3.evm :as web3-evm]
   [cljs-time.core :as t]
   ))

(declare deploy)
(defstate ^{:on-reload :noop} deployer
  :start (deploy (merge (:deployer @config)
                   (:deployer (mount/args)))))

(def registry-placeholder "feedfeedfeedfeedfeedfeedfeedfeedfeedfeed")
(def dnt-placeholder "deaddeaddeaddeaddeaddeaddeaddeaddeaddead")
(def forwarder-target-placeholder "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef")
(def district-config-placeholder "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
(def district-token-placeholder "dabbdabbdabbdabbdabbdabbdabbdabbdabbdabb")

(defn deploy-dnt! [default-opts]
  (deploy-smart-contract! :DNT (merge default-opts {:gas 5200000
                                                    :arguments [(contract-address :minime-token-factory)
                                                                (web3/to-wei 1000000 :ether)]})))

(defn deploy-district-token! [default-opts]
  (deploy-smart-contract! :district-token (merge default-opts {:gas 2200000
                                                               :arguments [(contract-address :district-registry-fwd)]})))

(defn deploy-minime-token-factory! [default-opts]
  (deploy-smart-contract! :minime-token-factory (merge default-opts {:gas 2300000})))

(defn deploy-ds-guard! [default-opts]
  (deploy-smart-contract! :ds-guard (merge default-opts {:gas 1000000})))

(defn deploy-district-config! [{:keys [:deposit-collector :district-auction-cut-collector :district-auction-cut] :as default-opts}]
  (deploy-smart-contract! :district-config (merge default-opts {:gas 1000000
                                                                :arguments [deposit-collector
                                                                            district-auction-cut-collector
                                                                            district-auction-cut]})))

(defn deploy-district-registry-db! [default-opts]
  (deploy-smart-contract! :district-registry-db (merge default-opts {:gas 1700000})))

(defn deploy-param-change-registry-db! [default-opts]
  (deploy-smart-contract! :param-change-registry-db (merge default-opts {:gas 1700000})))


(defn deploy-district-registry! [default-opts]
  (deploy-smart-contract! :district-registry (merge default-opts {:gas 1000000})))

(defn deploy-param-change-registry! [default-opts]
  (deploy-smart-contract! :param-change-registry (merge default-opts {:gas 1700000})))

(defn deploy-district-registry-fwd! [default-opts]
  (deploy-smart-contract! :district-registry-fwd (merge default-opts {:gas 500000
                                                                      :placeholder-replacements
                                                                      {forwarder-target-placeholder :district-registry}})))

(defn deploy-param-change-registry-fwd! [default-opts]
  (deploy-smart-contract! :param-change-registry-fwd (merge default-opts
                                                       {:gas 500000
                                                        :placeholder-replacements
                                                        {forwarder-target-placeholder :param-change-registry}})))

(defn deploy-district! [default-opts]
  (deploy-smart-contract! :district (merge default-opts {:gas 7999990
                                                         :arguments
                                                         [(contract-address :DNT)]
                                                         :placeholder-replacements
                                                         {
                                                          dnt-placeholder :DNT
                                                          registry-placeholder :district-registry-fwd
                                                          ;; district-config-placeholder :district-config
                                                          ;; district-token-placeholder :district-token
                                                          }})))
#_
(defn deploy-tsrb! [default-opts]
  (deploy-smart-contract! :tsrb (merge default-opts {:gas 10000000000
                                                     #_
                                                     :placeholder-replacements
                                                     #_
                                                     {
                                                      ;; dank-token-placeholder :DNT
                                                      ;; registry-placeholder :district-registry-fwd
                                                      ;; district-config-placeholder :district-config
                                                      ;; district-token-placeholder :district-token
                                                      }})))

(defn deploy-param-change! [default-opts]
  (deploy-smart-contract! :param-change (merge default-opts {:gas 5700000
                                                             :placeholder-replacements
                                                             {dnt-placeholder :DNT
                                                              registry-placeholder :param-change-registry-fwd}})))


(defn deploy-district-factory! [default-opts]
  (deploy-smart-contract! :district-factory (merge default-opts {:gas 1000000
                                                                 :arguments [(contract-address :district-registry-fwd)
                                                                             (contract-address :DNT)]
                                                                 :placeholder-replacements
                                                                 {forwarder-target-placeholder :district}})))

(defn deploy-param-change-factory! [default-opts]
  (deploy-smart-contract! :param-change-factory (merge default-opts {:gas 1000000
                                                                     :arguments [(contract-address :param-change-registry-fwd)
                                                                                 (contract-address :DNT)]
                                                                     :placeholder-replacements
                                                                     {forwarder-target-placeholder :param-change}})))

(defn deploy [{:keys [:write? :initial-registry-params transfer-dnt-to-accounts
                      :use-n-account-as-deposit-collector :use-n-account-as-cut-collector]
               :as deploy-opts}]
  (let [accounts (web3-eth/accounts @web3)
        deploy-opts (merge {:from (last accounts)
                            ;; this keys are to make testing simpler
                            ;; :deposit-collector (nth accounts (or use-n-account-as-deposit-collector 0))
                            ;; :district-auction-cut-collector (nth accounts (or use-n-account-as-cut-collector 0))
                            ;; :district-auction-cut 0

                            }
                      deploy-opts)]

    (deploy-ds-guard! deploy-opts)
    ;; make deployed :ds-guard its own autority
    (ds-auth/set-authority :ds-guard (contract-address :ds-guard) deploy-opts)

    (deploy-minime-token-factory! deploy-opts)
    (deploy-dnt! deploy-opts)
    ;; (deploy-district-config! deploy-opts)
    ;; (ds-auth/set-authority :district-config (contract-address :ds-guard) deploy-opts)

    (deploy-district-registry-db! deploy-opts)
    (deploy-param-change-registry-db! deploy-opts)

    (deploy-district-registry! deploy-opts)
    (deploy-param-change-registry! deploy-opts)

    (deploy-district-registry-fwd! deploy-opts)

    (deploy-param-change-registry-fwd! deploy-opts)

    (registry/construct [:district-registry :district-registry-fwd]
      {:db (contract-address :district-registry-db)}
      deploy-opts)

    (registry/construct [:param-change-registry :param-change-registry-fwd]
      {:db (contract-address :param-change-registry-db)}
      deploy-opts)

    ;; Allow :param-change-registry-fwd to grand permissions to other contracts (for ParamChanges to apply changes)
    (ds-guard/permit {:src (contract-address :param-change-registry-fwd)
                      :dst (contract-address :ds-guard)
                      :sig ds-guard/ANY}
      deploy-opts)


    ;; (deploy-district-token! deploy-opts)

    (deploy-district! deploy-opts)

    (deploy-param-change! deploy-opts)

    (deploy-district-factory! deploy-opts)

    (deploy-param-change-factory! deploy-opts)

    (eternal-db/set-uint-values :district-registry-db (:district-registry initial-registry-params) deploy-opts)
    (eternal-db/set-uint-values :param-change-registry-db (:param-change-registry initial-registry-params) deploy-opts)

    ;; make :ds-guard authority of both :district-registry-db and :param-change-registry-db
    (ds-auth/set-authority :district-registry-db (contract-address :ds-guard) deploy-opts)
    (ds-auth/set-authority :param-change-registry-db (contract-address :ds-guard) deploy-opts)
    ;; After authority is set, we can clean owner. Not really essential, but extra safety measure
    (ds-auth/set-owner :district-registry-db 0 deploy-opts)
    (ds-auth/set-owner :param-change-registry-db 0 deploy-opts)

    ;; Allow :district-registry-fwd to make changes into :district-registry-db
    (ds-guard/permit {:src (contract-address :district-registry-fwd)
                      :dst (contract-address :district-registry-db)
                      :sig ds-guard/ANY}
      deploy-opts)

    ;; Allow :param-change-registry-fwd to make changes into :district-registry-db (to apply ParamChanges)
    (ds-guard/permit {:src (contract-address :param-change-registry-fwd)
                      :dst (contract-address :district-registry-db)
                      :sig ds-guard/ANY}
      deploy-opts)
    
    ;; Allow :param-change-registry-fwd to make changes into :param-change-registry-db
    (ds-guard/permit {:src (contract-address :param-change-registry-fwd)
                      :dst (contract-address :param-change-registry-db)
                      :sig ds-guard/ANY}
      deploy-opts)

    (registry/set-factory [:district-registry :district-registry-fwd]
      {:factory (contract-address :district-factory) :factory? true}
      deploy-opts)

    (registry/set-factory [:param-change-registry :param-change-registry-fwd]
      {:factory (contract-address :param-change-factory) :factory? true}
      deploy-opts)

    (when (pos? transfer-dnt-to-accounts)
      (doseq [account (take transfer-dnt-to-accounts accounts)]
        (dnt/transfer {:to account :amount (web3/to-wei 15000 :ether)}
          ;; this is the deployer of dnt so it owns the initial amount
          {:from (last accounts)})))

    (when write?
      (write-smart-contracts!))

    (let [district (district-factory/approve-and-create-district
                     {:info-hash "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH"
                      :amount (web3/to-wei 10 :ether)}
                     {:from (first accounts)})
          reg-entry (-> district
                      district-registry/registry-entry-event-in-tx
                      :args
                      :registry-entry)]

      (prn "dnt" (dnt/balance-of (first accounts)))
      (prn "district token" (district/balance-of reg-entry (first accounts)))
      (prn "staked dnt" (district/total-staked-for reg-entry (first accounts)))

      (prn "staked"
        (district/approve-and-stake
          {:district reg-entry
           :amount (web3/to-wei 1 :ether)
           :data 0}
          {:from (first accounts)}))

      (prn "dnt" (dnt/balance-of (first accounts)))
      (prn "district token" (district/balance-of reg-entry (first accounts)))
      (prn "staked dnt" (district/total-staked-for reg-entry (first accounts)))

      (prn "unstaked"
        (district/unstake reg-entry (web3/to-wei 1 :ether) 0))

      (prn "dnt" (dnt/balance-of (first accounts)))
      (prn "district token" (district/balance-of reg-entry (first accounts)))
      (prn "staked dnt" (district/total-staked-for reg-entry (first accounts)))

      (prn
        "create challenge"
        (registry-entry/approve-and-create-challenge
          reg-entry
          {:amount (web3/to-wei 10 :ether)
           :meta-hash "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH"}
          {:from (last accounts)}))

      (prn "commit vote"
        (registry-entry/approve-and-commit-vote
          reg-entry
          {:amount (web3/to-wei 1 :ether)
           :salt "abc"
           :vote-option :vote.option/include}
          {:from (first accounts)}))

      (prn "commit vote"
        (registry-entry/approve-and-commit-vote
          reg-entry
          {:amount (web3/to-wei 2 :ether)
           :salt "abc"
           :vote-option :vote.option/exclude}
          {:from (last accounts)}))

      (prn "staked"
        (district/approve-and-stake
          {:district reg-entry
           :amount (web3/to-wei 1 :ether)
           :data 0}
          {:from (last accounts)}))

      (prn "increased time 2 mins"
        (web3-evm/increase-time! @web3 [(inc (t/in-seconds (t/minutes 2)))]))

      (prn "reveal vote"
        (registry-entry/reveal-vote
          reg-entry
          {:vote-option :vote.option/include
           :salt "abc"}
          {:from (first accounts)}))

      (prn "reveal vote"
        (registry-entry/reveal-vote
          reg-entry
          {:vote-option :vote.option/exclude
           :salt "abc"}
          {:from (last accounts)}))

      (prn "increased time 1 min"
        (web3-evm/increase-time! @web3 [(inc (t/in-seconds (t/minutes 1)))]))

      (prn "vote reward"
        (registry-entry/vote-reward
          reg-entry
          (first accounts)))

      (prn "vote reward"
        (registry-entry/vote-reward
          reg-entry
          (last accounts)))

      (prn "claim vote reward"
        (registry-entry/claim-vote-reward
          reg-entry
          {:from (first accounts)}))

      (prn "claim vote reward"
        (registry-entry/claim-vote-reward
          reg-entry
          {:from (last accounts)}))

      (prn
        "create challenge 2"
        (registry-entry/approve-and-create-challenge
          reg-entry
          {:amount (web3/to-wei 10 :ether)
           :meta-hash "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH"}
          {:from (first accounts)}))

      )))
