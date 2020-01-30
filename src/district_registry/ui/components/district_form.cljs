(ns district-registry.ui.components.district-form
  (:require
    [cljs-web3.core :as web3]
    [district-registry.shared.utils :refer [debounce]]
    [district-registry.ui.components.app-layout :refer [app-layout]]
    [district-registry.ui.contract.ens :as ens]
    [district-registry.ui.events :as events]
    [district-registry.ui.spec :as spec]
    [district-registry.ui.subs :as subs]
    [district.format :as format]
    [district.graphql-utils :as graphql-utils]
    [district.ui.component.form.input :refer [index-by-type file-drag-input with-label chip-input text-input textarea-input select-input int-input assoc-by-path]]
    [district.ui.component.page :refer [page]]
    [district.ui.component.tx-button :refer [tx-button]]
    [district.ui.graphql.subs :as gql]
    [district.ui.web3-tx-id.subs :as tx-id-subs]
    [district.web3-utils :as web3-utils]
    [print.foo :refer [look] :include-macros true]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [reagent.ratom :refer [reaction]]
    [clojure.string :as str])
  (:require-macros [district-registry.shared.macros :refer [get-environment]]))


(defn param-search-query [param]
  [:search-param-changes {:key (graphql-utils/kw->gql-name param)
                          :db (graphql-utils/kw->gql-name :district-registry-db)
                          :group-by :param-changes.group-by/key
                          :order-by :param-changes.order-by/applied-on}
   [[:items [:param-change/value :param-change/key]]]])


(defn upload-image-button-label [text]
  [:div.upload-image-button-label
   [:img {:src "/images/svg/upload.svg"}]
   [:div text]])


(defn- file-acceptable? [{:keys [:expected-width :expected-height]} {:keys [:type :file :size]}]
  (js/Promise. (fn [resolve reject]
                 (let [URL (or (aget js/window "URL") (aget js/window "webkitURL"))
                       Image (js/document.createElement "img")]
                   (aset Image "onload" (fn []
                                          (this-as this
                                            (cond
                                              (or (not= (aget this "width") expected-width)
                                                  (not= (aget this "height") expected-height))
                                              (reject {:error :image-size})

                                              (not (contains? #{"image/png" "image/jpg" "image/jpeg"} type))
                                              (reject {:error :file-type})

                                              (> size 1000000)
                                              (reject {:error :file-size})

                                              :else
                                              (resolve)))))
                   (aset Image "src" (js-invoke URL "createObjectURL" file))))))


(def default-form-data
  (when (= "dev" (get-environment))
    {:name "Name Bazaar"
     :url "https://namebazaar.io/"
     :github-url "https://github.com/district0x/name-bazaar"
     :facebook-url "https://www.facebook.com/district0x/"
     :twitter-url "https://twitter.com/NameBazaar0x"
     :aragon-id "namebazaar"
     :description "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, masuada non rutrum ut, ornare ac orci. Lorem ipsum dolor sit amet, consectetur adipiscing. Lorem augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non."}))


(defn submit-button []
  (let [tx-id (random-uuid)
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:approve-and-create-district tx-id}])]
    (fn [{:keys [:deposit :form-data :errors]}]
      [:div.form-btns
       [:p (-> deposit
             web3-utils/wei->eth-number
             format/format-dnt)]
       [tx-button
        {:class "cta-btn"
         :disabled (-> errors empty? not)
         :pending-text "Submitting..."
         :pending? @tx-pending?
         :on-click (fn [e]
                     (js-invoke e "preventDefault")
                     (when (empty? errors)
                       (dispatch [::events/add-district-logo-image (assoc @form-data
                                                                     :deposit deposit
                                                                     :tx-id tx-id)])))
         :type "submit"}
        "Submit"]])))


(defn save-button [{:keys [:form-data :errors :reg-entry/address]}]
  (let [tx-pending? (subscribe [::tx-id-subs/tx-pending? {:set-meta-hash {:reg-entry/address address}}])]
    [:div.form-btns.save-button
     [tx-button
      {:class "cta-btn"
       :disabled (-> errors empty? not)
       :pending-text "Saving..."
       :pending? @tx-pending?
       :on-click (fn [e]
                   (js-invoke e "preventDefault")
                   (when (empty? errors)
                     (dispatch [::events/add-district-logo-image (merge @form-data
                                                                        {:edit? true
                                                                         :reg-entry/address address})])))
       :type "submit"}
      "Save"]]))

(defn- aragon-full-name [aragon-id]
  (str aragon-id ".aragonid.eth"))

(def debounced-check-availability
  (debounce
    (fn [value]
      (dispatch [::ens/check-availability {:aragon-id value}]))
    500))


(defn image-input [{:keys [:form-data :width :height :id]} text]
  (let [submit-error (r/atom false)]
    (fn []
      [:div.btn-wrap
       [file-drag-input {:form-data form-data
                         :id id
                         :label [upload-image-button-label text]
                         :file-accept-pred (partial file-acceptable? {:expected-width width :expected-height height})
                         :on-file-accepted (fn []
                                             (reset! submit-error false))
                         :on-file-rejected (fn [_ {:keys [:error]}]
                                             (reset! submit-error error))}]
       [:p
        (when @submit-error {:class "error"})
        (condp = @submit-error
          :image-size [:<> "Invalid Size (" width " x " height ")"]
          :file-type [:<> "Invalid File Type"]
          :file-size [:<> "Image is too large (max 1MB)"]
          [:<> "Size " width " x " height])]])))


(defn district-form [{:keys [:form-data :edit?]}]
  (let [deposit-query (when-not edit?
                        (subscribe [::gql/query {:queries [(param-search-query :deposit)]}]))
        form-data (r/atom (or form-data default-form-data))]
    (when (and (= "dev" (get-environment))                  ;; dirty, but only for dev, so please forgive me
               (not edit?))
      (js/setTimeout #(dispatch [::ens/check-availability (select-keys @form-data [:aragon-id])]) 1000))
    (fn [{:keys [:reg-entry/address]}]
      (let [aragon-id-available? @(subscribe [::subs/aragon-id-available? (:aragon-id @form-data)])
            deposit (when-not edit?
                      (-> @deposit-query
                        :search-param-changes
                        :items
                        first
                        :param-change/value))
            {:keys [:name
                    :description
                    :url
                    :github-url
                    :facebook-url
                    :twitter-url
                    :aragon-id
                    :logo-file-info
                    :background-file-info]} @form-data
            errors (cond-> []
                     (empty? name) (conj "District title is required")
                     (empty? description) (conj "District description is required")
                     (empty? url) (conj "URL is required")
                     (empty? github-url) (conj "GitHub URL is required")
                     (and (not edit?) (empty? aragon-id)) (conj "Aragon ID is required")
                     (and (not edit?) (not (empty? aragon-id)) (false? aragon-id-available?)) (conj "Aragon ID is not available")
                     (and (seq url) (not (spec/check ::spec/url url))) (conj "URL is not valid")
                     (and (seq github-url) (not (re-find #"https?://github.com/.+" github-url))) (conj "GitHub URL is not valid")
                     (and (seq facebook-url) (not (re-find #"https?://(www\.)?facebook.com/.+" facebook-url))) (conj "Facebook URL is not valid")
                     (and (seq twitter-url) (not (re-find #"https?://twitter.com/.+" twitter-url))) (conj "Twitter URL is not valid")
                     (and (not edit?) (not logo-file-info)) (conj "A logo file is required")
                     (and (not edit?) (not background-file-info)) (conj "A background file is required")
                     (< 15000 (count description)) (conj "District description is too long")
                     (< 100 (count github-url)) (conj "GitHub URL is too long")
                     (< 100 (count facebook-url)) (conj "Facebook URL is too long")
                     (< 100 (count twitter-url)) (conj "Twitter URL is too long")
                     (< 100 (count url)) (conj "URL is too long")
                     (< 50 (count name)) (conj "District title is too long"))]
        [:section#main
         [:div.container
          [:div.box-wrap
           [:div.hero
            [:div.container
             [:div.header-image.sized
              [:img {:src "/images/submit-header@2x.png"}]]
             [:div.page-title [:h1 (if edit? "Edit" "Submit")]]]]
           [:div.body-text
            [:div.container
             (when-not edit?
               [:p.intro-text
                "Below you can fill out all the parameters required to submit your district for a place in the District Registry. These items can be altered if a district is not currently challenged or blacklisted. Further down you can see available options for token issuance curves, with descriptions for each. Currently, only the flat issuance curve is supported. These curves determine how many DNT need to be staked to earn votes for successive stakers to the district. Token issuance curve cannot be altered once submitted. You can read more "
                [:a {:href "https://education.district0x.io/district0x-specific-topics/understanding-distict0x/the-district-registry/" :target :_blank} "here"] "."])
             [:form.image-upload
              [:div.row.spaced
               [:div.col.left
                [text-input {:form-data form-data
                             :placeholder "Name"
                             :id :name}]
                [text-input {:form-data form-data
                             :placeholder "URL"
                             :id :url}]
                [text-input {:form-data form-data
                             :placeholder "GitHub URL"
                             :id :github-url}]
                [text-input {:form-data form-data
                             :placeholder "Facebook URL"
                             :id :facebook-url}]
                [text-input {:form-data form-data
                             :placeholder "Twitter URL"
                             :id :twitter-url}]
                (when-not edit?
                  (let [aragon-id (:aragon-id @form-data)
                        hint (when-not (str/blank? aragon-id)
                               (condp = aragon-id-available?
                                 true (str (aragon-full-name aragon-id) " is available")
                                 false (str (aragon-full-name aragon-id) " is taken")
                                 "Checking availability..."))]
                    [text-input {:form-data form-data
                                 :placeholder "Aragon ID"
                                 :id :aragon-id
                                 :class "aragon-id"
                                 :group-class (condp = aragon-id-available?
                                                true "available"
                                                false "taken"
                                                "checking")
                                 :errors {:local {:aragon-id {:hint hint}}}
                                 :on-change (fn [value]
                                              (if (re-matches #"[a-z0-9]{0,100}" value)
                                                (debounced-check-availability value)
                                                (swap! form-data assoc-by-path :aragon-id aragon-id)))}]))
                [:div.submit-errors
                 (doall
                   (for [e errors]
                     [:div.error {:key e} "*" e]))]]
               [:div.col.right
                [textarea-input {:form-data form-data
                                 :placeholder "Description"
                                 :id :description}]
                [:div.form-btns
                 [image-input
                  {:form-data form-data
                   :id :logo-file-info
                   :width 256
                   :height 256}
                  "Upload Logo"]
                 [image-input
                  {:form-data form-data
                   :id :background-file-info
                   :width 1120
                   :height 800}
                  "Upload Background"]]]]]
             (if edit?
               [save-button
                {:reg-entry/address address
                 :form-data form-data
                 :errors errors}]
               [:<>
                [:form.voting
                 [submit-button
                  {:form-data form-data
                   :deposit deposit
                   :errors errors}]]])]]]]]))))