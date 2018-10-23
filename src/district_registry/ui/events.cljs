(ns district-registry.ui.events
  (:require
   [cljsjs.buffer]
   [clojure.pprint :refer [pprint]]
   [district-registry.ui.contract.district-factory :as district-factory]
   [district-registry.ui.contract.registry-entry :as registry-entry]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame]))

(defn- build-challenge-meta-string [{:keys [comment] :as data}]
  (-> {:comment comment}
    clj->js
    js/JSON.stringify))

;; Adds the challenge to ipfs and if successfull dispatches ::create-challenge
(re-frame/reg-event-fx
  ::add-challenge
  (fn [{:keys [db]} [_ {:keys [:reg-entry/address :comment] :as data}]]
    (let [challenge-meta (build-challenge-meta-string {:comment comment})
          buffer-data (js/buffer.Buffer.from challenge-meta)]
      (prn "Uploading challenge meta " challenge-meta)
      {:ipfs/call {:func "add"
                   :args [buffer-data]
                   :on-success [::registry-entry/approve-and-create-challenge data]
                   :on-error ::error}})))

(re-frame/reg-event-fx
  ::add-district-logo
  (fn [_ [_ {:keys [logo-file-info] :as data}]]
    {:ipfs/call {:func "add"
                 :args [(:file logo-file-info)]
                 :on-success [::add-district-background data]
                 :on-error ::error}}))

(re-frame/reg-event-fx
  ::add-district-background
  (fn [_ [_ {:keys [background-file-info] :as data} logo-hash]]
    {:ipfs/call {:func "add"
                 :args [(:file background-file-info)]
                 :on-success [::add-district-info data logo-hash]
                 :on-error ::error}}))

(defn- build-district-info-string [data logo background]
  (->> [:name
        :description
        :url
        :github-url]
    (select-keys data)
    (merge {:logo-image-hash (:Hash logo)
            :background-image-hash (:Hash background)})
    (into (sorted-map))
    clj->js
    js/JSON.stringify))

(re-frame/reg-event-fx
  ::add-district-info
  (fn [_ [_ data logo background]]
    (let [district-info (build-district-info-string data logo background)
          buffer-data (js/buffer.Buffer.from district-info)]
      {:ipfs/call {:func "add"
                   :args [buffer-data]
                   :on-success [::district-factory/approve-and-create-district data]
                   :on-error ::error}})))

(re-frame/reg-event-db
  ::pprint
  (fn [_ args]
    (pprint args)))
