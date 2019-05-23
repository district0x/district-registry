(ns district-registry.ui.components.nav
  (:require
   [district.ui.router.utils :as router-utils]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]))

(defn a [{:as props
          :keys [route]}
         & children]
  (into [:a
         (r/merge-props
           (dissoc props :route)
             {:on-click (fn [e]
                          (.preventDefault e)
                          (dispatch (->> route
                                      (cons :district.ui.router.events/navigate)
                                      vec)))
              :href (apply router-utils/resolve route)})]
    children))

(defn div [{:as props
            :keys [route]}
           & children]
  (into [:div.cursor-pointer
         (r/merge-props
           (dissoc props :route)
           {:on-click (fn [e]
                        (dispatch (->> route
                                    (cons :district.ui.router.events/navigate)
                                    vec)))})]
    children))
