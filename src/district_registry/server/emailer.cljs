(ns district-registry.server.emailer
  (:require
    [bignumber.core :as bn]
    [cljs-time.coerce :as time-coerce]
    [cljs-time.core :as t]
    [district-registry.server.contract.district0x-emails :as district0x-emails]
    [district-registry.server.db :as db]
    [district-registry.server.emailer.templates :as templates]
    [district-registry.server.syncer]
    [district.encryption :as encryption]
    [district.format :as format]
    [district.sendgrid :refer [send-email]]
    [district.server.config :as config]
    [district.server.config :refer [config]]
    [district.server.logging]
    [district.server.web3-events :refer [register-callback! unregister-callbacks!]]
    [district.shared.async-helpers :refer [promise->]]
    [district.shared.error-handling :refer [try-catch try-catch-throw]]
    [district.time :as time]
    [goog.format.EmailAddress :as email-address]
    [mount.core :as mount :refer [defstate]]
    [taoensso.timbre :as log]))

(defn validate-email [base64-encrypted-email]
  (if (get-in @config/config [:emailer :private-key])
    (when-not (empty? base64-encrypted-email)
      (let [email (encryption/decode-decrypt (get-in @config/config [:emailer :private-key]) base64-encrypted-email)]
        (when (email-address/isValidAddress email)
          email)))
    (do
      (log/error "Private key for emailer wasn't configured")
      nil)))


(defn send-challenge-created-email-handler
  [{:keys [from to
           name
           district-url
           button-url
           time-remaining
           on-success on-error
           template-id
           api-key
           print-mode?]}]
  (send-email
    {:from from
     :to to
     :subject (str name " was challenged!")
     :content (templates/challenge-created-email-body {:district/name name
                                                       :district-url district-url
                                                       :time-remaining time-remaining})
     :substitutions {:header (str name " was challenged")
                     :button-title "Vote Now"
                     :button-href button-url}
     :on-success on-success
     :on-error on-error
     :template-id template-id
     :api-key api-key
     :print-mode? print-mode?}))


(defn send-challenge-created-email [{:keys [:registry-entry :challenger :commit-period-end :reward-pool :metahash :timestamp :version] :as ev}]
  (let [{:keys [:reg-entry/creator]} (db/get-registry-entry {:reg-entry/address registry-entry} [:reg-entry/creator])
        {:keys [:district/name]} (db/get-district {:reg-entry/address registry-entry} [:district/name])
        {:keys [:from :template-id :api-key :print-mode?]} (get-in @config/config [:emailer])
        root-url (format/ensure-trailing-slash (get-in @config/config [:ui :root-url]))
        district-url (str root-url "detail/" registry-entry)
        [unit value] (time/time-remaining-biggest-unit (t/now)
                                                       (-> commit-period-end time/epoch->long time-coerce/from-long))
        time-remaining (format/format-time-units {unit value})]
    (promise-> (district0x-emails/get-email {:district0x-emails/address creator})
               #(validate-email %)
               (fn [to] (if to
                          (do
                            (log/info "Sending district challenged email" ev ::send-challenge-created-email)
                            (send-challenge-created-email-handler
                              {:from from
                               :to to
                               :name name
                               :district-url district-url
                               :button-url district-url
                               :time-remaining time-remaining
                               :on-success #(log/info "Success sending district challenged email"
                                                      {:to to :reg-entry/address registry-entry :district/name name}
                                                      ::send-challenge-created-email)
                               :on-error #(log/error "Error when sending district challenged email"
                                                     {:error % :event ev :district/name name :to to}
                                                     ::send-challenge-created-email)
                               :template-id template-id
                               :api-key api-key
                               :print-mode? print-mode?}))
                          (log/info "No email found for challenged district creator"
                                    {:event ev :district/name name :reg-entry/address registry-entry}
                                    ::send-challenge-created-email))))))


(defn send-vote-reward-claimed-email-handler
  [{:keys [to from
           name option
           amount
           district-url
           button-url
           on-success
           on-error
           template-id
           api-key
           print-mode?]}]
  (send-email {:from from
               :to to
               :subject "You received a vote reward"
               :content (templates/vote-reward-claimed-email-body {:district/name name
                                                                   :vote/option (case option
                                                                                  1 "YES"
                                                                                  2 "NO")
                                                                   :amount amount
                                                                   :district-url district-url})
               :substitutions {:header "Vote Reward"
                               :button-title "Go to District"
                               :button-href button-url}
               :on-success on-success
               :on-error on-error
               :template-id template-id
               :api-key api-key
               :print-mode? print-mode?}))


(defn send-vote-reward-claimed-email [{:keys [:registry-entry :timestamp :index :version :voter :amount] :as ev}]
  (let [{:keys [:district/name]} (db/get-district {:reg-entry/address registry-entry} [:district/name])
        {:keys [:vote/option]} (db/get-vote {:reg-entry/address registry-entry
                                             :vote/voter voter
                                             :challenge/index (bn/number index)} [:vote/option])
        {:keys [:from :template-id :api-key :print-mode?]} (get-in @config/config [:emailer])
        root-url (format/ensure-trailing-slash (get-in @config/config [:ui :root-url]))
        district-url (str root-url "detail/" registry-entry)]
    (promise-> (district0x-emails/get-email {:district0x-emails/address voter})
               #(validate-email %)
               (fn [to]
                 (if to
                   (do
                     (log/info "Sending vote reward received email" ev ::send-vote-reward-claimed-email)
                     (send-vote-reward-claimed-email-handler
                       {:to to
                        :from from
                        :name name
                        :option option
                        :amount amount
                        :district-url district-url
                        :button-url district-url
                        :on-success #(log/info "Success sending vote reward email"
                                               {:to to :reg-entry/address registry-entry :district/name name}
                                               ::send-vote-reward-claimed-email)
                        :on-error #(log/error "Error when sending vote reward email"
                                              {:error % :event ev :reg-entry/address registry-entry :to to}
                                              ::send-vote-reward-claimed-email)
                        :template-id template-id
                        :api-key api-key
                        :print-mode? print-mode?}))
                   (log/info "No email found for voter" {:event ev :reg-entry/address registry-entry} ::send-vote-reward-claimed-email))))))


(defn send-challenger-reward-claimed-email-handler
  [{:keys [to from
           name
           amount
           district-url
           button-url
           on-success
           on-error
           template-id
           api-key
           print-mode?]}]
  (send-email {:from from
               :to to
               :subject "You received a challenge reward"
               :content (templates/challenger-reward-claimed-email-body {:amount amount
                                                                         :district/name name
                                                                         :district-url district-url})
               :substitutions {:header "Challenge Reward"
                               :button-title "Go to District"
                               :button-href button-url}
               :on-success on-success
               :on-error on-error
               :template-id template-id
               :api-key api-key
               :print-mode? print-mode?}))


(defn send-challenger-reward-claimed-email [{:keys [:registry-entry :timestamp :version :challenger :amount] :as ev}]
  (let [{:keys [:district/name]} (db/get-district {:reg-entry/address registry-entry} [:district/name])
        {:keys [:from :template-id :api-key :print-mode?]} (get-in @config/config [:emailer])
        root-url (format/ensure-trailing-slash (get-in @config/config [:ui :root-url]))
        district-url (str root-url "detail/" registry-entry)]
    (promise-> (district0x-emails/get-email {:district0x-emails/address challenger})
               #(validate-email %)
               (fn [to]
                 (if to
                   (do
                     (log/info "Sending challenger chalenge reward received email" ev ::send-challenger-reward-claimed-email)
                     (send-challenger-reward-claimed-email-handler
                       {:from from
                        :to to
                        :name name
                        :amount amount
                        :district-url district-url
                        :button-url district-url
                        :on-success #(log/info "Success sending challenge reward claimed email"
                                               {:to to :reg-entry/address registry-entry :district/name name}
                                               ::send-challenger-reward-claimed-email)
                        :on-error #(log/error "Error when sending challenge reward claimed email"
                                              {:error % :event ev :reg-entry/address registry-entry :to to}
                                              ::send-challenger-reward-claimed-email)
                        :template-id template-id
                        :api-key api-key
                        :print-mode? print-mode?}))
                   (log/info "No email found for challenger" {:event ev :reg-entry/address registry-entry} ::send-challenger-reward-claimed-email))))))


(defn send-creator-reward-claimed-email-handler
  [{:keys [to from
           name
           amount
           district-url
           button-url
           on-success
           on-error
           template-id
           api-key
           print-mode?]}]
  (send-email {:from from
               :to to
               :subject "You received a challenge reward"
               :content (templates/creator-reward-claimed-email-body {:amount amount
                                                                      :district/name name
                                                                      :district-url district-url})
               :substitutions {:header "Challenge Reward"
                               :button-title "Go to District"
                               :button-href button-url}
               :on-success on-success
               :on-error on-error
               :template-id template-id
               :api-key api-key
               :print-mode? print-mode?}))


(defn send-creator-reward-claimed-email [{:keys [:registry-entry :timestamp :version :creator :amount] :as ev}]
  (let [{:keys [:district/name]} (db/get-district {:reg-entry/address registry-entry} [:district/name])
        {:keys [:from :template-id :api-key :print-mode?]} (get-in @config/config [:emailer])
        root-url (format/ensure-trailing-slash (get-in @config/config [:ui :root-url]))
        district-url (str root-url "detail/" registry-entry)]
    (promise-> (district0x-emails/get-email {:district0x-emails/address creator})
               #(validate-email %)
               (fn [to]
                 (if to
                   (do
                     (log/info "Sending creator chalenge reward received email" ev ::send-creator-reward-claimed-email)
                     (send-creator-reward-claimed-email-handler
                       {:from from
                        :to to
                        :name name
                        :amount amount
                        :district-url district-url
                        :button-url district-url
                        :on-success #(log/info "Success sending creator challenge reward claimed email"
                                               {:to to :reg-entry/address registry-entry :district/name name}
                                               ::send-creator-reward-claimed-email)
                        :on-error #(log/error "Error when sending creator challenge reward claimed email"
                                              {:error % :event ev :reg-entry/address registry-entry :to to}
                                              ::send-creator-reward-claimed-email)
                        :template-id template-id
                        :api-key api-key
                        :print-mode? print-mode?}))
                   (log/info "No email found for creator" {:event ev :reg-entry/address registry-entry} ::send-creator-reward-claimed-email))))))


(defn send-reveal-reminder-email-handler
  [{:keys [to from
           name
           district-url
           button-url
           on-success
           on-error
           template-id
           api-key
           print-mode?]}]
  (send-email {:from from
               :to to
               :subject "It's time to reveal your vote"
               :content (templates/reveal-reminder-email-body {:district/name name
                                                               :district-url district-url})
               :substitutions {:header "It's time to reveal your vote"
                               :button-title "Go to District"
                               :button-href button-url}
               :on-success on-success
               :on-error on-error
               :template-id template-id
               :api-key api-key
               :print-mode? print-mode?}))


(defn send-reveal-reminder-email [{:keys [:registry-entry :timestamp :version :voter :commit-period-end] :as ev}]
  (let [{:keys [:district/name]} (db/get-district {:reg-entry/address registry-entry} [:district/name])
        {:keys [:from :template-id :api-key :print-mode?]} (get-in @config/config [:emailer])
        root-url (format/ensure-trailing-slash (get-in @config/config [:ui :root-url]))
        district-url (str root-url "detail/" registry-entry)]
    (promise-> (district0x-emails/get-email {:district0x-emails/address voter})
               #(validate-email %)
               (fn [to]
                 (if to
                   (do
                     (log/info "Sending reveal reminder email" ev ::send-reveal-reminder-email)
                     (send-reveal-reminder-email-handler
                       {:from from
                        :to to
                        :name name
                        :district-url district-url
                        :button-url district-url
                        :on-success #(log/info "Success sending reveal reminder email"
                                               {:to to :reg-entry/address registry-entry :district/name name}
                                               ::send-reveal-reminder-email)
                        :on-error #(log/error "Error when sending challenge reward claimed email"
                                              {:error % :event ev :reg-entry/address registry-entry :to to}
                                              ::send-reveal-reminder-email)
                        :template-id template-id
                        :api-key api-key
                        :print-mode? print-mode?}))
                   (log/info "No email found for voter" {:event ev :reg-entry/address registry-entry} ::send-reveal-reminder-email))))))


(defn- dispatcher [callback]
  (fn [_ {:keys [:latest-event? :args]}]
    (if (= callback send-reveal-reminder-email)
      (let [now (quot (.getTime (js/Date.)) 1000)
            {:keys [:commit-period-end]} args]
        (when (< now commit-period-end)
          (let [ms-to-reveal-period (* (- commit-period-end now) 1000)]
            (log/info (str "Will send reveal reminder email in " ms-to-reveal-period "ms.") ::send-reveal-reminder-email)
            (js/setTimeout #(callback args) ms-to-reveal-period))))
      (when latest-event?
        (callback args)))))


(defn start [opts]
  (let [callback-ids
        [(register-callback! :district-registry/challenge-created-event (dispatcher send-challenge-created-email))
         (register-callback! :district-registry/vote-reward-claimed-event (dispatcher send-vote-reward-claimed-email))
         (register-callback! :district-registry/challenger-reward-claimed-event (dispatcher send-challenger-reward-claimed-email))
         (register-callback! :district-registry/creator-reward-claimed-event (dispatcher send-creator-reward-claimed-email))
         (register-callback! :district-registry/vote-committed-event (dispatcher send-reveal-reminder-email))]]
    (assoc opts :callback-ids callback-ids)))


(defn stop [emailer]
  (unregister-callbacks! (:callback-ids @emailer)))


(defstate ^{:on-reload :noop} emailer
  :start (start (merge (:pinner @config)
                       (:pinner (mount/args))))
  :stop (stop emailer))

