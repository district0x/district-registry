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
        errors (reaction {:local (let [{:keys [name
                                               description
                                               url
                                               github-url
                                               logo-file-info
                                               background-file-info
                                               dnt-weight]} @form-data]
                                   (cond-> {}
                                     (empty? name)
                                     (assoc-in [:name :error] "District title is mandatory")

                                     (empty? description)
                                     (assoc-in [:description :error] "District description is mandatory")

                                     (and
                                       (seq url)
                                       (not (spec/check ::spec/url url)))
                                     (assoc-in [:url :error] "URL is not valid")

                                     (and
                                       (seq github-url)
                                       (not (re-find #"http://github.com/.+" github-url)))
                                     (assoc-in [:github-url :error] "GitHub URL is not valid")

                                     (not logo-file-info) (assoc-in [:logo-file-info :error] "No logo file selected")
                                     (not background-file-info) (assoc-in [:background-file-info :error] "No background file selected")))})]
    (fn []
      (let []
        [app-layout
         [:div.submit-page
          [:section.upload
           [:div.image-panel
            [file-drag-input {:form-data form-data
                              :id :logo-file-info
                              :errors errors
                              :label "Upload a logo"
                              :file-accept-pred (fn [{:keys [name type size] :as props}]
                                                  (or
                                                    (= type "image/png")
                                                    (= type "image/jpg")
                                                    (= type "image/jpeg")))
                              :on-file-accepted (fn [{:keys [name type size array-buffer] :as props}]
                                                  (prn "Accepted " props))
                              :on-file-rejected (fn [{:keys [name type size] :as props}]
                                                  (prn "Rejected " props))}]]
           [file-drag-input {:form-data form-data
                             :id :background-file-info
                             :errors errors
                             :label "Upload a background"
                             :file-accept-pred (fn [{:keys [name type size] :as props}]
                                                 (or
                                                   (= type "image/png")
                                                   (= type "image/jpg") ))
                             :on-file-accepted (fn [{:keys [name type size array-buffer] :as props}]
                                                 (prn "Accepted " props))
                             :on-file-rejected (fn [{:keys [name type size] :as props}]
                                                 (prn "Rejected " props))}]
           [:div.form-panel
            [:div (str (:local @errors))]
            [text-input {:form-data form-data
                         :placeholder "Name"
                         :errors errors
                         :id :name}]
            [textarea-input {:form-data form-data
                             :placeholder "Description"
                             :errors errors
                             :id :description}]
            [text-input {:form-data form-data
                         :placeholder "URL"
                         :errors errors
                         :id :url}]
            [text-input {:form-data form-data
                         :placeholder "GitHub URL"
                         :errors errors
                         :id :github-url}]
            [with-label "Bonding Curve Ratio"
             [select-input {:form-data form-data
                            :id :dnt-weight
                            :errors errors
                            :options [{:key 333333 :value "1/3"}
                                      {:key 500000 :value "1/2"}
                                      {:key 1000000 :value "1/1"}]}]]
            [:div.submit
             [:button {:on-click (fn []
                                   (dispatch [::events/add-district-logo (->> @deposit-query
                                                                           :search-param-changes
                                                                           :items
                                                                           first
                                                                           :param-change/value
                                                                           (assoc @form-data :deposit))]))
                       :disabled (-> @errors :local seq)}
              "Submit"]]]]]]))))
