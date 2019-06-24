(ns district-registry.ui.components.district-form
  (:require
    [cljs-web3.core :as web3]
    [district-registry.ui.components.app-layout :refer [app-layout]]
    [district-registry.ui.events :as events]
    [district-registry.ui.spec :as spec]
    [district.format :as format]
    [district.graphql-utils :as graphql-utils]
    [district.ui.component.form.input :refer [index-by-type file-drag-input with-label chip-input text-input textarea-input select-input int-input]]
    [district.ui.component.page :refer [page]]
    [district.ui.component.tx-button :refer [tx-button]]
    [district.ui.graphql.subs :as gql]
    [district.ui.web3-tx-id.subs :as tx-id-subs]
    [print.foo :refer [look] :include-macros true]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [reagent.ratom :refer [reaction]]
    [district.web3-utils :as web3-utils])
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


(defn- file-acceptable? [{:keys [type]}]
  (contains? #{"image/png" "image/jpg" "image/jpeg"} type))


(def default-form-data
  (merge {:dnt-weight 1000000}
         (when (= "dev" (get-environment))
           {:name "Name Bazaar"
            :url "https://namebazaar.io/"
            :github-url "https://github.com/district0x/name-bazaar"
            :facebook-url "https://www.facebook.com/district0x/"
            :twitter-url "https://twitter.com/NameBazaar0x"
            :description "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, masuada non rutrum ut, ornare ac orci. Lorem ipsum dolor sit amet, consectetur adipiscing. Lorem augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non."})))


(defn- dnt-weight-changed [form-data weight]
  #(swap! form-data assoc :dnt-weight weight))


(defn- issuance-curve-radio [form-data]
  [:div.radio-boxes
   [:div.radio-box
    [:fieldset
     (let [dnt-weight 1000000]
       [:input#r3 {:name "radio-group"
                   :type "radio"
                   :checked (= (:dnt-weight @form-data) dnt-weight)
                   :on-change (dnt-weight-changed form-data dnt-weight)}])
     [:label {:for "r3"} "Curve Option 1/1"]]
    [:img.radio-img {:src "/images/curve-graph-1000000-m.svg"}]
    [:p
     "Lorem ipsum dolor sit amet, consec tetur adipiscing elit, sed do eiusmod."]]
   [:div.radio-box
    [:fieldset
     (let [dnt-weight 500000]
       [:input#r2 {:name "radio-group"
                   :type "radio"
                   :checked (= (:dnt-weight @form-data) dnt-weight)
                   :on-change (dnt-weight-changed form-data dnt-weight)}])
     [:label {:for "r2"} "Curve Option 1/2"]]
    [:img.radio-img {:src "/images/curve-graph-500000-m.svg"}]
    [:p
     "Lorem ipsum dolor sit amet, consec tetur adipiscing elit, sed do eiusmod."]]
   [:div.radio-box
    [:fieldset
     (let [dnt-weight 333333]
       [:input#r1 {:name "radio-group"
                   :type "radio"
                   :checked (= (:dnt-weight @form-data) dnt-weight)
                   :on-change (dnt-weight-changed form-data dnt-weight)}])
     [:label {:for "r1"} "Curve Option 1/3"]]
    [:img.radio-img {:src "/images/curve-graph-333333-m.svg"}]
    [:p "Lorem ipsum dolor sit amet, consec tetur adipiscing elit, sed do eiusmod."]]])


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


(defn district-form [{:keys [:form-data :edit?]}]
  (let [deposit-query (when-not edit?
                        (subscribe [::gql/query {:queries [(param-search-query :deposit)]}]))
        form-data (r/atom (or form-data default-form-data))]
    (fn [{:keys [:reg-entry/address]}]
      (let [deposit (when-not edit?
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
                    :logo-file-info
                    :background-file-info]} @form-data
            errors (cond-> []
                     (empty? name) (conj "District title is required")
                     (empty? description) (conj "District description is required")
                     (empty? url) (conj "URL is required")
                     (empty? github-url) (conj "GitHub URL is required")
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
             [:p.intro-text
              "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, masuada non rutrum ut, ornare ac orci. Lorem ipsum dolor sit amet, consectetur adipiscing. Lorem augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non."]
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
                [:div.submit-errors
                 (doall
                   (for [e errors]
                     [:div.error {:key e} "*" e]))]]
               [:div.col.right
                [textarea-input {:form-data form-data
                                 :placeholder "Description"
                                 :id :description}]
                [:div.form-btns
                 [:div.btn-wrap
                  [file-drag-input {:form-data form-data
                                    :id :logo-file-info
                                    :label [upload-image-button-label "Upload Logo"]
                                    :file-accept-pred file-acceptable?
                                    :on-file-accepted (fn [props]
                                                        (prn "Accepted " props))
                                    :on-file-rejected (fn [props]
                                                        (prn "Rejected " props))}]
                  [:p "Size 256 x 256"]]
                 [:div.btn-wrap
                  [file-drag-input {:form-data form-data
                                    :id :background-file-info
                                    :label [upload-image-button-label "Upload Background"]
                                    :file-accept-pred file-acceptable?
                                    :on-file-accepted (fn [props]
                                                        (prn "Accepted " props))
                                    :on-file-rejected (fn [props]
                                                        (prn "Rejected " props))}]
                  [:p "Size 1120 x 800"]]]]]]
             (if edit?
               [save-button
                {:reg-entry/address address
                 :form-data form-data
                 :errors errors}]
               [:<>
                [:div.h-line]
                [:h2 "Voting Token Issuance Curve"]
                [:form.voting
                 [issuance-curve-radio form-data]
                 [submit-button
                  {:form-data form-data
                   :deposit deposit
                   :errors errors}]]])]]]]]))))