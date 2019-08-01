(ns district-registry.ui.my-account.page
  (:require
    [bignumber.core :as bn]
    [district-registry.ui.components.app-layout :refer [app-layout]]
    [district-registry.ui.components.nav :as nav]
    [district-registry.ui.events :as events]
    [district-registry.ui.subs :as subs]
    [district.ui.component.form.input :refer [text-input file-drag-input]]
    [district.ui.component.page :refer [page]]
    [district.ui.component.tx-button :refer [tx-button]]
    [district.ui.component.tx-log :refer [tx-log]]
    [district.ui.router.subs :as router-subs]
    [district.ui.web3-accounts.subs :as accounts-subs]
    [district.ui.web3-tx-id.subs :as tx-id-subs]
    [goog.format.EmailAddress :as email-address]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [reagent.ratom :refer [reaction]]))


(defn valid-email? [s & [{:keys [:allow-empty?]}]]
  (let [valid? (email-address/isValidAddress s)]
    (if allow-empty?
      (or (empty? s) valid?)
      valid?)))


(defn email-section []
  (let [form-data (r/atom {:email ""})
        has-email? (subscribe [::subs/active-account-has-email?])
        active-account (subscribe [::accounts-subs/active-account])
        errors (reaction {:local (cond-> {}
                                   (not (valid-email? (or (:email @form-data) "") {:allow-empty? true}))
                                   (assoc-in [:email :error] "Invalid email format"))})]
    (fn []
      (let [tx-pending? @(subscribe [::tx-id-subs/tx-pending? {:set-email @active-account}])]
        [:<>
         [:h1.email "Email"]
         [:p "The email associated with your address will be encrypted and stored on a public blockchain. Only our email server will be able to decrypt it. We'll use it to send you automatic notifications about your activity, as well as important website updates. You can unsubscribe at any time by saving a blank email address above. You can view our privacy policy here."]
         [:p "We use the service Sendgrid to generate these emails. You can also always unsubscribe from this service by opting out through their provided links at the bottom of every email."]
         (when @has-email?
           [:p.success "You already associated an email with your Ethereum address."])
         [:form
          [text-input {:form-data form-data
                       :placeholder "Email"
                       :class "email"
                       :errors errors
                       :id :email}]
          [:div.form-btns
           [tx-button
            {:class "cta-btn"
             :disabled (-> @errors :local empty? not)
             :pending-text "Submitting..."
             :pending? tx-pending?
             :on-click (fn [e]
                         (js-invoke e "preventDefault")
                         (dispatch [::events/set-email (:email @form-data)]))}
            "Submit"]]]]))))


(defn vote-secrets-section []
  (let [form-data (r/atom {})
        imported? (r/atom false)]
    (fn []
      (let []
        [:<>
         [:h1.email "Vote Secrets"]
         [:p "In order to ensure integrity of voting, every time you vote, a generated secret is stored in your browser.
         This secret is used to reveal your vote later. In case you clear your browser's storage, you won't be able to reveal your votes.
         Therefore we recommend you to backup your vote secrets regularly, so you can import them back if necessary."]
         [:form
          [:button
           {:class "cta-btn backup-vote-secrets-btn"
            :on-click (fn [e]
                        (js-invoke e "preventDefault")
                        (dispatch [::events/backup-vote-secrets]))}
           "Backup Vote Secrets"]
          [file-drag-input {:form-data form-data
                            :class "cta-btn"
                            :id :vote-secrets-file
                            :label "Import Vote Secrets"
                            :on-file-accepted (fn [{:keys [:file]}]
                                                (let [reader (js/FileReader.)]
                                                  (aset reader "onload"
                                                        (fn [ev]
                                                          (dispatch [::events/import-vote-secrets (aget ev "target" "result")])
                                                          (reset! imported? true)))
                                                  (js-invoke reader "readAsText" file)))}]
          (when @imported?
            [:p "Vote secrets have been successfully imported."])]]))))


(defn- navigation-item []
  (let [active-page-params (subscribe [::router-subs/active-page-params])]
    (fn [{:keys [:tab]} text]
      [:li
       {:class (when (= (:tab @active-page-params) tab)
                 "on")}
       [nav/a {:route [:route/my-account {:tab tab}]
               :class "cta-btn"}
        text]])))


(defn my-account []
  (let [active-page-params (subscribe [::router-subs/active-page-params])]
    (fn []
      [:section#main
       [:div.container
        [:nav.subnav
         [:ul
          [navigation-item
           {:tab "activity"}
           "Activity"]
          [navigation-item
           {:tab "email"}
           "Email"]
          [navigation-item
           {:tab "vote-secrets"}
           "Vote Secrets"]]]
        [:div.box-wrap
         [:div.body-text
          [:div.container
           (condp = (:tab @active-page-params)
             "email" [email-section]
             "vote-secrets" [vote-secrets-section]
             [tx-log {:header-props {:text [:h1 "Activity"]}
                      :settings-el [:div]
                      :tx-cost-currency :USD}])]]]]])))


(defmethod page :route/my-account []
  (fn []
    [app-layout
     [my-account]]))
