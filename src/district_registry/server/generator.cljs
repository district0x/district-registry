(ns district-registry.server.generator
  (:require
   [bignumber.core :as bn]
   [cljs-ipfs-api.files :as ipfs-files]
   [cljs-time.core :as t]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.evm :as web3-evm]
   [cljs-web3.utils :refer [js->cljkk camel-case]]
   [district.format :as format]
   [district-registry.server.contract.district :as district]
   [district-registry.server.contract.district-factory :as district-factory]
   [district-registry.server.contract.district-registry :as district-registry]
   [district-registry.server.contract.dnt :as dnt]
   [district-registry.server.contract.eternal-db :as eternal-db]
   [district-registry.server.contract.minime-token :as minime-token]
   [district-registry.server.contract.param-change :as param-change]
   [district-registry.server.contract.param-change-factory :as param-change-factory]
   [district-registry.server.contract.param-change-registry :as param-change-registry]
   [district-registry.server.contract.registry :as registry]
   [district-registry.server.contract.registry-entry :as registry-entry]
   [district-registry.server.deployer]
   [district.cljs-utils :refer [rand-str]]
   [district.server.config :refer [config]]
   [district.server.smart-contracts :refer [contract-address contract-call contract-event-in-tx instance]]
   [district.server.web3 :refer [web3]]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]))

(def fs (js/require "fs"))

(declare start)
(defstate ^{:on-reload :noop} generator
  :start (try
           (start (merge
                    (:generator @config)
                    (:generator (mount/args))))
           (catch :default e
             (log/error (str e))))
  :stop (constantly nil))

(defn get-scenarios [{:keys [:accounts :use-accounts :items-per-account :scenarios]}]
  (when (and
          (pos? use-accounts)
          (pos? items-per-account)
          (seq scenarios))
    (let [accounts-repeated (flatten (for [x accounts]
                                       (repeat items-per-account x)))
          scenarios (map #(if (keyword? %) {:scenario-type %} %) scenarios)
          scenarios-repeated (take (* use-accounts items-per-account) (cycle scenarios))]
      (partition 2 (interleave accounts-repeated scenarios-repeated)))))

(defn generate-districts [{:keys [:accounts :districts/use-accounts :districts/items-per-account :districts/scenarios]}]
  (let [[max-total-supply max-auction-duration deposit commit-period-duration reveal-period-duration]
        (->> (eternal-db/get-uint-values :district-registry-db [:max-total-supply :max-auction-duration :deposit :commit-period-duration
                                                                :reveal-period-duration])
          (map bn/number))]
    (doseq [[account {:keys [:scenario-type]}] (get-scenarios {:accounts accounts
                                                               :use-accounts use-accounts
                                                               :items-per-account items-per-account
                                                               :scenarios scenarios})]
      (let [meta-hash "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH"
            total-supply (inc (rand-int max-total-supply))
            auction-duration (+ 60 (rand-int (- max-auction-duration 60)))]

        (let [tx-hash (district-factory/approve-and-create-district {:meta-hash meta-hash
                                                                     :total-supply total-supply
                                                                     :amount deposit}
                        {:from account})]

          (when-not (= :scenario/create scenario-type)
            (let [{{:keys [:registry-entry]} :args} (district-registry/registry-entry-event-in-tx tx-hash)]
              (when-not registry-entry
                (throw (js/Error. "Registry Entry wasn't found")))

              (registry-entry/approve-and-create-challenge registry-entry
                {:meta-hash meta-hash
                 :amount deposit}
                {:from account})

              (when-not (= :scenario/challenge scenario-type)
                (let [{:keys [:reg-entry/creator]} (registry-entry/load-registry-entry registry-entry)
                      balance (dnt/balance-of creator)]

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
                      {:db (contract-address (or param-change-db :district-registry-db))
                       :key :deposit
                       :value (web3/to-wei 800 :ether)
                       :amount deposit}
                      {:from account})

            {:keys [:registry-entry]} (:args (param-change-registry/registry-entry-event-in-tx tx-hash))]

        (when-not (= scenario-type :scenario/create)
          (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])

          (param-change-registry/apply-param-change registry-entry {:from account}))))))

(defn upload-image [file-path]
  (log/info "Uploading" file-path ::upload-image)
  (js/Promise.
    (fn [resolve reject]
      (.readFile fs
        file-path
        (fn [err data]
          (if err
            (reject err)
            (ipfs-files/add
              data
              (fn [err {image-hash :Hash}]
                (if err
                  (reject err)
                  (do
                    (log/info (str "Uploaded " file-path " received") {:image-hash image-hash} ::upload-meme)
                    (resolve image-hash)))))))))))

(defn upload-data [data]
  (log/info "Uploading data" {:data data} ::upload-data)
  (js/Promise.
    (fn [resolve reject]
      (ipfs-files/add
        (-> data clj->js js/JSON.stringify js/Buffer.from)
        (fn [err {hash :Hash}]
          (if err
            (log/error "ifps error" {:error err} ::upload-data)
            (do
              (log/info "Uploaded data received " {:hash hash} ::upload-data)
              (resolve hash))))))))
#_
(prn
  (contract-event-in-tx
    "0xda687280ce3a1bc8a3c5633a3d10220f8b29192381e3607f5affc952f7323546"
    :district-registry
    :DistrictConstructedEvent
    ))
#_
(district-factory/approve-and-create-district
  {:meta-hash "QmbgUT4MgkHuJoTJspTNwMPBNyNRp96MxqHwhfacF4rRgV"
   :dnt-weight 333333
   :amount (web3/to-wei 10000 :wei)}
  {:from "0x90F8bf6A479f320ead074411a4B0e7944Ea8c9C1"})
(defn generate-district-and-challenges [{:keys [district-hashes
                                                challenge-hashes]}]
  (prn "district hashes" district-hashes)
  (prn "challenge hashes" challenge-hashes)

  (let [accounts (web3-eth/accounts @web3)
        reg-entry-from-tx (fn [tx-hash]
                            (-> tx-hash
                              (contract-event-in-tx
                                [:district-registry :district-registry-fwd]
                                :DistrictConstructedEvent)
                              :args
                              :registry-entry))
        reg-entry (reg-entry-from-tx
                    (district-factory/approve-and-create-district
                      {:meta-hash (first district-hashes)
                       :dnt-weight 333333
                       :amount (web3/to-wei 1000 :wei)}
                      {:from (first accounts)}))
        reg-entry2 (reg-entry-from-tx
                     (district-factory/approve-and-create-district
                       {:meta-hash (second district-hashes)
                        :dnt-weight 1000000
                        :amount (web3/to-wei 1000 :wei)}
                       {:from (first accounts)}))]

    (prn "dnt" (dnt/balance-of (first accounts)))
    (prn "reg entry" reg-entry "acct" (first accounts))
    (prn "district token" (district/balance-of reg-entry (first accounts)))

    (prn "staked"
      (district/approve-and-stake
        {:district reg-entry
         :amount (web3/to-wei 1000 :wei)}
        {:from (first accounts)}))

    (prn "dnt" (dnt/balance-of (first accounts)))
    (prn "district token" (district/balance-of reg-entry (first accounts)))

    (prn "staked 2"
      (district/approve-and-stake
        {:district reg-entry
         :amount (web3/to-wei 1000 :wei)}
        {:from (first accounts)}))

    (prn "dnt" (dnt/balance-of (first accounts)))
    (prn "district token" (district/balance-of reg-entry (first accounts)))

    (prn "unstaked"
      (district/unstake reg-entry (web3/to-wei 10 :wei) {:from (first accounts)}))

    (prn "dnt" (dnt/balance-of (first accounts)))
    (prn "district token" (district/balance-of reg-entry (first accounts)))

    (prn "dnt last" (dnt/balance-of (last accounts)))

    (prn
      "create challenge"
      "hash" (first challenge-hashes)
      (registry-entry/approve-and-create-challenge
        reg-entry
        {:amount (web3/to-wei 1000 :wei)
         :meta-hash (first challenge-hashes)}
        {:from (last accounts)}))

    (prn "commit vote"
      (registry-entry/approve-and-commit-vote
        reg-entry
        {:index 0
         :amount (web3/to-wei 100 :wei)
         :salt "abc"
         :vote-option :vote.option/include}
        {:from (first accounts)}))

    (prn "commit vote"
      (registry-entry/approve-and-commit-vote
        reg-entry
        {:index 0
         :amount (web3/to-wei 20 :wei)
         :salt "abc"
         :vote-option :vote.option/exclude}
        {:from (last accounts)}))

    (prn "staked"
      (district/approve-and-stake
        {:district reg-entry
         :amount (web3/to-wei 10 :wei)}
        {:from (last accounts)}))

    (prn "increased time 2 mins"
      (web3-evm/increase-time! @web3 [(inc (t/in-seconds (t/minutes 2)))]))

    (prn "reveal vote"
      (registry-entry/reveal-vote
        reg-entry
        {:index 0
         :vote-option :vote.option/include
         :salt "abc"}
        {:from (first accounts)}))

    (prn "reveal vote"
      (registry-entry/reveal-vote
        reg-entry
        {:index 0
         :vote-option :vote.option/exclude
         :salt "abc"}
        {:from (last accounts)}))

    (prn "increased time 1 min"
      (web3-evm/increase-time! @web3 [(inc (t/in-seconds (t/minutes 1)))]))

    (prn "claim vote reward"
      (registry-entry/claim-vote-reward
        reg-entry
        {:index 0}
        {:from (first accounts)}))

    (prn
      "create challenge 2"
      (registry-entry/approve-and-create-challenge
        reg-entry
        {:amount (web3/to-wei 1000 :wei)
         :meta-hash (first challenge-hashes)}
        {:from (last accounts)}))

    (prn "commit vote"
      (registry-entry/approve-and-commit-vote
        reg-entry
        {:index 1
         :amount (web3/to-wei 100 :wei)
         :salt "abc"
         :vote-option :vote.option/exclude}
        {:from (last accounts)}))


    (prn "increased time 2 mins"
      (web3-evm/increase-time! @web3 [(inc (t/in-seconds (t/minutes 2)))]))

    (prn "reveal vote"
      (registry-entry/reveal-vote
        reg-entry
        {:index 1
         :vote-option :vote.option/exclude
         :salt "abc"}
        {:from (last accounts)}))

    (prn "increased time 1 min"
      (web3-evm/increase-time! @web3 [(inc (t/in-seconds (t/minutes 1)))]))

    (prn "claim vote reward"
      (registry-entry/claim-vote-reward
        reg-entry
        {:index 1}
        {:from (last accounts)}))

    ))

(defn start [opts]
  (let [opts (assoc opts :accounts (web3-eth/accounts @web3))]
    #_
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
                          {:db (contract-address (or param-change-db :district-registry-db))
                           :key :deposit
                           :value (web3/to-wei 800 :ether)
                           :amount deposit}
                          {:from account})

                {:keys [:registry-entry]} (:args (param-change-registry/registry-entry-event-in-tx tx-hash))]

            (when-not (= scenario-type :scenario/create)
              (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])

              (param-change-registry/apply-param-change registry-entry {:from account}))))))

    ;; TODO: Parameterize district creation and remove printing
    (-> #js [(upload-image "resources/dev/logo.png")
             (upload-image "resources/dev/background.jpg")]
      js/Promise.all
      (.then (fn [arr]
               (->> (range 2)
                 (mapcat (fn [i]
                           [(upload-data
                              {:name (str "District #" i)
                               :description (str "Description for District #" i)
                               :url (str "https://example.com/district" i)
                               :github-url (str "https://github.com/district0x/district" i)
                               :logo-image-hash (aget arr 0)
                               :background-image-hash (aget arr 1)})
                            (upload-data {:comment (str "Challenge for District #" i)})]))
                 into-array
                 js/Promise.all)))
      (.then (fn [arr]
               (let [district-hash->challenge-hash (apply hash-map (js->clj arr))]
                 (generate-district-and-challenges
                   (assoc opts
                     :district-hashes (keys district-hash->challenge-hash)
                     :challenge-hashes (vals district-hash->challenge-hash))))))
      (.catch (fn [err]
                (log/error err))))
    #_
    (generate-districts opts)
    #_
    (generate-param-changes opts)))
