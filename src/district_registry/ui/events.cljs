(ns district-registry.ui.events
  (:require
    [cljs.reader :as reader]
    [cljsjs.buffer]
    [clojure.string :as string]
    [district-registry.shared.utils :as shared-utils]
    [district-registry.ui.config :as config]
    [district-registry.ui.contract.district-factory :as district-factory]
    [district-registry.ui.contract.registry-entry :as registry-entry]
    [district.cljs-utils :as cljs-utils]
    [district.encryption :as encryption]
    [district.ui.logging.events :as logging]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as account-queries]
    [district.ui.web3-tx.events :as tx-events]
    [district.ui.web3.queries :as web3-queries]
    [medley.core :as medley]
    [print.foo :refer [look] :include-macros true]
    [re-frame.core :as re-frame]
    [taoensso.timbre :as log]))

(def interceptors [re-frame/trim-v])

(defn- build-challenge-meta-string [{:keys [comment] :as data}]
  (-> {:comment comment}
    clj->js
    js/JSON.stringify))

;; Adds the challenge to ipfs and if successfull dispatches ::create-challenge
(re-frame/reg-event-fx
  ::add-challenge
  interceptors
  (fn [{:keys [:db]} [{:keys [:reg-entry/address :comment] :as data}]]
    (let [challenge-meta (build-challenge-meta-string {:comment (string/trim comment)})
          buffer-data (js/buffer.Buffer.from challenge-meta)]
      (prn "Uploading challenge meta " challenge-meta)
      {:ipfs/call {:func "add"
                   :args [buffer-data]
                   :on-success [::registry-entry/approve-and-create-challenge data]
                   :on-error ::error}})))


(re-frame/reg-event-fx
  ::add-district-logo-image
  interceptors
  (fn [_ [{:keys [:logo-file-info :edit?] :as data}]]
    (if (and edit? (not logo-file-info))
      {:dispatch [::add-district-bg-image data {:Hash (:district/logo-image-hash data)}]}
      {:ipfs/call {:func "add"
                   :args [(:file logo-file-info)]
                   :on-success [::add-district-bg-image data]
                   :on-error ::error}})))


(re-frame/reg-event-fx
  ::add-district-bg-image
  interceptors
  (fn [_ [{:keys [:background-file-info :edit?] :as data} logo-hash]]
    (if (and edit? (not background-file-info))
      {:dispatch [::add-district-meta data logo-hash {:Hash (:district/background-image-hash data)}]}
      {:ipfs/call {:func "add"
                   :args [(:file background-file-info)]
                   :on-success [::add-district-meta data logo-hash]
                   :on-error ::error}})))


(defn- safe-trim [s]
  (if (string? s)
    (string/trim s)
    s))


(defn- build-district-info-string [data logo background]
  (->> [:name
        :description
        :url
        :github-url
        :facebook-url
        :twitter-url]
    (select-keys data)
    (medley/remove-vals nil?)
    (medley/map-vals safe-trim)
    (merge {:logo-image-hash (:Hash logo)
            :background-image-hash (:Hash background)})
    (into (sorted-map))
    clj->js
    js/JSON.stringify))


(re-frame/reg-event-fx
  ::add-district-meta
  interceptors
  (fn [_ [{:keys [:edit?] :as data} logo bg-img]]
    (let [district-meta (build-district-info-string data logo bg-img)
          buffer-data (js/buffer.Buffer.from district-meta)]
      {:ipfs/call {:func "add"
                   :args [buffer-data]
                   :on-success (if edit?
                                 [::registry-entry/set-meta-hash data]
                                 [::district-factory/approve-and-create-district data])
                   :on-error ::error}})))


(re-frame/reg-event-fx
  ::load-email-settings
  (fn [{:keys [db]} _]
    (let [active-account (account-queries/active-account db)
          instance (contract-queries/instance db :district0x-emails)]
      (when (and active-account instance)
        {:web3/call {:web3 (web3-queries/web3 db)
                     :fns [{:instance instance
                            :fn :get-email
                            :args [active-account]
                            :on-success [::encrypted-email-found active-account]
                            :on-error [::logging/error "Error loading user encrypted email"
                                       {:user {:id active-account}}
                                       ::load-email-settings]}]}}))))


(re-frame/reg-event-db
  ::encrypted-email-found
  interceptors
  (fn [db [address encrypted-email]]
    (if (or (not encrypted-email)
            (string/blank? encrypted-email))
      (do
        (log/info "No encrypted email found for user" {:user {:id address}
                                                       :encrypted-email encrypted-email})
        db)
      (do (log/info "Loaded user encrypted email" {:user {:id address}
                                                   :encrypted-email encrypted-email})
          (assoc-in db [:district-registry.ui.my-account address :encrypted-email] encrypted-email)))))


(re-frame/reg-event-fx
  ::set-email
  interceptors
  (fn [{:keys [:db]} [email]]
    (let [public-key (:district0x-emails-public-key config/config-map)
          encrypted-email (if (empty? email)
                            ""
                            (encryption/encrypt-encode public-key email))
          active-account (account-queries/active-account db)]
      (log/debug (str "Encypted " email " with " public-key) ::save-settings)
      {:dispatch [::tx-events/send-tx
                  {:instance (contract-queries/instance db :district0x-emails)
                   :fn :set-email
                   :args [encrypted-email]
                   :tx-opts {:from active-account}
                   :tx-id {:set-email active-account}
                   :tx-log {:name (if (empty? email)
                                    "Erase email"
                                    (str "Set email to " email)) :related-href {:name :route/my-account}}
                   :on-tx-success [::set-email-success active-account encrypted-email]
                   :on-tx-hash-error [::logging/error [::set-email email]]
                   :on-tx-error [::logging/error [::set-email-error]]}]})))


(re-frame/reg-event-fx
  ::set-email-success
  [interceptors (re-frame/inject-cofx :store)]
  (fn [{:keys [store db]} [account encrypted-email]]
    {:store (assoc-in store [:district-registry.ui.my-account account :encrypted-email] encrypted-email)
     :db (assoc-in db [:district-registry.ui.my-account account :encrypted-email] encrypted-email)}))


(re-frame/reg-event-fx
  ::backup-vote-secrets
  [interceptors (re-frame/inject-cofx :store)]
  (fn [{:keys [:store]} [{:keys [:file/filename]}]]
    (let [filename "district_registry_vote_secrets.edn"
          votes (str (:district-registry.ui.core/votes store))]
      {:file/write [filename votes]})))


(re-frame/reg-event-fx
  ::import-vote-secrets
  [interceptors (re-frame/inject-cofx :store)]
  (fn [{:keys [:db :store]} [data-string]]
    (let [votes (reader/read-string data-string)]
      {:store (update store :district-registry.ui.core/votes cljs-utils/merge-in votes)
       :db (update db :district-registry.ui.core/votes cljs-utils/merge-in votes)})))


(re-frame/reg-fx
  :file/write
  (fn [[filename content]]
    (shared-utils/file-write filename content)))
