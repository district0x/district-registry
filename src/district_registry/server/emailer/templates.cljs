(ns district-registry.server.emailer.templates
  (:require
    [bignumber.core :as bn]
    [clojure.string :as str]
    [district.web3-utils :as web3-utils]))

(defn- format-token-amount [amount]
  (-> amount web3-utils/wei->eth-number (bn/fixed 3)))

(defn format-link [url text]
  (str "<a href=\"" url "\" >" text "</a>"))

(defn challenge-created-email-body [{:keys [:district/name :district-url :time-remaining]}]
  (let [link (format-link district-url name)]
    (str "Your district " link " was just challenged. Hurry, you have "
         (str/lower-case time-remaining) " to visit the website and vote to keep your district in the District Registry!")))

(defn vote-reward-claimed-email-body [{:keys [:amount :district/name :district-url :vote/option]}]
  (let [link (format-link district-url name)
        amount (format-token-amount amount)]
    (str "You received " amount " DNT as a reward for voting " option " for " link)))

(defn challenger-reward-claimed-email-body [{:keys [:amount :district/name :district-url]}]
  (let [link (format-link district-url name)
        amount (format-token-amount amount)]
    (str "You received " amount " DNT as a reward for challenging " link ".")))

(defn creator-reward-claimed-email-body [{:keys [:amount :district/name :district-url]}]
  (let [link (format-link district-url name)
        amount (format-token-amount amount)]
    (str "You received " amount " DNT as a reward for withstanding challenge for " link ".")))

(defn reveal-reminder-email-body [{:keys [:district/name :district-url]}]
  (let [link (format-link district-url name)]
    (str "You voted about destiny of " link ". Now voting has entered reveal phase. Please reveal your vote, so it counts!")))
