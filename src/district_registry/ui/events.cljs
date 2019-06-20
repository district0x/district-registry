(ns district-registry.ui.events
  (:require
    [cljsjs.buffer]
    [clojure.pprint :refer [pprint]]
    [district-registry.ui.contract.district-factory :as district-factory]
    [district-registry.ui.contract.registry-entry :as registry-entry]
    [print.foo :refer [look] :include-macros true]
    [re-frame.core :as re-frame]
    [medley.core :as medley]
    [clojure.string :as string]))

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
