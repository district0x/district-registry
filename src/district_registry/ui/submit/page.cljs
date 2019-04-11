(ns district-registry.ui.submit.page
  (:require
   [cljs-web3.core :as web3]
   [district-registry.ui.components.app-layout :refer [app-layout]]
   [district-registry.ui.events :as events]
   [district-registry.ui.spec :as spec]
   [district.format :as format]
   [district.graphql-utils :as graphql-utils]
   [district.ui.component.form.input :refer [index-by-type
                                             file-drag-input
                                             with-label
                                             chip-input
                                             text-input
                                             textarea-input
                                             select-input
                                             int-input]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [reagent.core :as r]
   [reagent.ratom :refer [reaction]]))

(defn param-search-query [param]
  [:search-param-changes {:key (graphql-utils/kw->gql-name param)
                          :db (graphql-utils/kw->gql-name :district-registry-db)
                          :group-by :param-changes.group-by/key
                          :order-by :param-changes.order-by/applied-on}
   [[:items [:param-change/value :param-change/key]]]])

(defmethod page :route/submit []
  (let [deposit-query (subscribe [::gql/query {:queries [(param-search-query :deposit)]}])
        form-data (r/atom {:dnt-weight 1000000})
        dnt-weight-on-change (fn [weight]
                               #(swap! form-data assoc :dnt-weight weight))]
    (fn []
      (let [deposit-wei (-> @deposit-query
                          :search-param-changes
                          :items
                          first
                          :param-change/value)
            {:keys [name
                    description
                    url
                    github-url
                    logo-file-info
                    background-file-info
                    dnt-weight]} @form-data
            errors (cond-> []
                     (empty? name) (conj "District title is required")
                     (empty? description) (conj "District description is required")
                     (empty? url) (conj "URL is required")
                     (empty? github-url) (conj "GitHub URL is required")
                     (and (seq url) (not (spec/check ::spec/url url))) (conj "URL is not valid")
                     (and (seq github-url) (not (re-find #"https?://github.com/.+" github-url))) (conj "GitHub URL is not valid")
                     (not logo-file-info) (conj "A logo file is required")
                     (not background-file-info) (conj "A background file is required"))]
        [app-layout
         [:section#main
          [:div.container
           [:div.box-wrap
            [:div.hero
             [:div.container
              [:div.header-image.sized
               [:img {:src "/images/submit-header@2x.png"}]]
              [:div.page-title [:h1 "Submit"]]]]
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
                                     :label "Upload Logo"
                                     :file-accept-pred (fn [{:keys [name type size] :as props}]
                                                         (or
                                                           (= type "image/png")
                                                           (= type "image/jpg")
                                                           (= type "image/jpeg")))
                                     :on-file-accepted (fn [{:keys [name type size array-buffer] :as props}]
                                                         (prn "Accepted " props))
                                     :on-file-rejected (fn [{:keys [name type size] :as props}]
                                                         (prn "Rejected " props))}]
                   [:p "Size 256 x 256"]]
                  [:div.btn-wrap
                   [file-drag-input {:form-data form-data
                                     :id :background-file-info
                                     :label "Upload Background"
                                     :file-accept-pred (fn [{:keys [name type size] :as props}]
                                                         (or
                                                           (= type "image/png")
                                                           (= type "image/jpg")
                                                           (= type "image/jpeg")))
                                     :on-file-accepted (fn [{:keys [name type size array-buffer] :as props}]
                                                         (prn "Accepted " props))
                                     :on-file-rejected (fn [{:keys [name type size] :as props}]
                                                         (prn "Rejected " props))}]
                   [:p "Size 1024 x 325"]]]]]]
              [:div.h-line]
              [:h2 "Voting Token Issuance Curve"]
              [:form.voting
               [:div.radio-boxes
                [:div.radio-box
                 [:fieldset
                  [:input#r1 {:name "radio-group"
                              :type "radio"
                              :checked (= (:dnt-weight @form-data) 333333)
                              :on-change (dnt-weight-on-change 333333)}]
                  [:label {:for "r1"} "Curve Option 1/3"]]
                 [:img.radio-img {:src "/images/curve-graph-333333-m.svg"}]
                 [:p
                  "Lorem ipsum dolor sit amet, consec tetur adipiscing elit, sed do eiusmod."]]
                [:div.radio-box
                 [:fieldset
                  [:input#r2 {:name "radio-group"
                              :type "radio"
                              :checked (= (:dnt-weight @form-data) 500000)
                              :on-change (dnt-weight-on-change 500000)}]
                  [:label {:for "r2"} "Curve Option 1/2"]]
                 [:img.radio-img {:src "/images/curve-graph-500000-m.svg"}]
                 [:p
                  "Lorem ipsum dolor sit amet, consec tetur adipiscing elit, sed do eiusmod."]]
                [:div.radio-box
                 [:fieldset
                  [:input#r3 {:name "radio-group"
                              :type "radio"
                              :checked (= (:dnt-weight @form-data) 1000000)
                              :on-change (dnt-weight-on-change 1000000)}]
                  [:label {:for "r3"} "Curve Option 1/1"]]
                 [:img.radio-img {:src "/images/curve-graph-1000000-m.svg"}]
                 [:p
                  "Lorem ipsum dolor sit amet, consec tetur adipiscing elit, sed do eiusmod."]]]
               [:div.form-btns
                [:p (-> deposit-wei
                      (web3/from-wei :ether)
                      format/format-dnt)]
                [:button.cta-btn
                 {:disabled (-> errors empty? not)
                  :on-click (fn [e]
                              (.preventDefault e)
                              (when (empty? errors)
                                (dispatch [::events/add-district-logo (assoc @form-data :deposit deposit-wei)])))
                  :type "submit"}
                 "Submit"]]]]]]]]]))))
