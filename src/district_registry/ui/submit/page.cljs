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
        form-data (r/atom {})
        dnt-weight-on-change (fn [weight]
                               #(swap! form-data assoc :dnt-weight weight))
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
         [:section#main
          [:div.container
           [:div.box-wrap
            [:div.hero
             [:div.container
              [:div.header-image.sized
               [:img {:src "images/submit-header@2x.png"}]]
              [:div.page-title [:h1 "Submit"]]
              ]]
            [:div.body-text
             [:div.container
              [:p.intro-text
               "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, masuada non rutrum ut, ornare ac orci. Lorem ipsum dolor sit amet, consectetur adipiscing. Lorem augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non."]
              [:div (str (:local @errors))]
              [:form.image-upload
               [:div.row.spaced
                [:div.col.left
                 [text-input {:form-data form-data
                              :placeholder "Name"
                              :errors errors
                              :id :name}]
                 [text-input {:form-data form-data
                              :placeholder "URL"
                              :errors errors
                              :id :url}]
                 [text-input {:form-data form-data
                              :placeholder "GitHub URL"
                              :errors errors
                              :id :github-url}]]
                [:div.col.right
                 [textarea-input {:form-data form-data
                                  :placeholder "Description"
                                  :errors errors
                                  :id :description}]]]
               [:div.form-btns
                [:div.btn-wrap
                 [:button.cta-btn.hasIcon
                  [:span.icon.icon-upload]
                  [file-drag-input {:form-data form-data
                                    :id :logo-file-info
                                    :errors errors
                                    :label "Upload Logo"
                                    :file-accept-pred (fn [{:keys [name type size] :as props}]
                                                        (or
                                                          (= type "image/png")
                                                          (= type "image/jpg")
                                                          (= type "image/jpeg")))
                                    :on-file-accepted (fn [{:keys [name type size array-buffer] :as props}]
                                                        (prn "Accepted " props))
                                    :on-file-rejected (fn [{:keys [name type size] :as props}]
                                                        (prn "Rejected " props))}]]
                 [:p "Size 256 x 256"]]
                [:div.btn-wrap
                 [:button.cta-btn.hasIcon
                  [:span.icon.icon-upload]
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
                                                        (prn "Rejected " props))}]]
                 [:p "Size 1024 x 325"]]]]
              [:div.h-line]
              [:h2 "Voting Token Issuance Curve"]
              [:form.voting
               [:div.radio-boxes

                [:div.radio-box
                 [:fieldset
                  [:input#r1 {:name "radio-group", :type "radio"
                              :on-change (dnt-weight-on-change 333333)}]
                  [:label {:for "r1"} "Curve Option 1/3"]]
                 [:img.radio-img {:src "images/voting-graph@2x.png"}]
                 [:p
                  "Lorem ipsum dolor sit amet, consec tetur adipiscing elit, sed do eiusmod."]]
                [:div.radio-box
                 [:fieldset
                  [:input#r2 {:name "radio-group", :type "radio"
                              :on-change (dnt-weight-on-change 500000)}]
                  [:label {:for "r2"} "Curve Option 1/2"]]
                 [:img.radio-img {:src "images/voting-graph@2x.png"}]
                 [:p
                  "Lorem ipsum dolor sit amet, consec tetur adipiscing elit, sed do eiusmod."]]
                [:div.radio-box
                 [:fieldset
                  [:input#r3 {:name "radio-group", :type "radio"
                              :on-change (dnt-weight-on-change 1000000)}]
                  [:label {:for "r3"} "Curve Option 1/1"]]
                 [:img.radio-img {:src "images/voting-graph@2x.png"}]
                 [:p
                  "Lorem ipsum dolor sit amet, consec tetur adipiscing elit, sed do eiusmod."]]]
               (when-not (-> @errors :local seq)
                 [:div.form-btns
                  [:p (->> @deposit-query
                        :search-param-changes
                        :items
                        first
                        :param-change/value
                        ;; FIXME: No deposit param change, why?
                        )
                   "10,000 DNT"]
                  [:button.cta-btn
                   {:on-click (fn [e]
                                (.preventDefault e)
                                (dispatch [::events/add-district-logo (->> @deposit-query
                                                                        :search-param-changes
                                                                        :items
                                                                        first
                                                                        :param-change/value
                                                                        (assoc @form-data :deposit))]))
                    :type "submit"}
                   "Submit"]])]]]]]]]))))
