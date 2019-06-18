(ns district-registry.ui.submit.page
  (:require
    [district-registry.ui.components.app-layout :refer [app-layout]]
    [district-registry.ui.components.district-form :refer [district-form]]
    [district.ui.component.page :refer [page]]
    [print.foo :refer [look] :include-macros true]
    [re-frame.core :refer [subscribe dispatch]]))


(defmethod page :route/submit []
  [app-layout
   [district-form]])
