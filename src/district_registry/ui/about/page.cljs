(ns district-registry.ui.about.page
  (:require
   [district-registry.ui.components.app-layout :refer [app-layout]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [re-frame.core :refer [subscribe]]))

(defmethod page :route/about []
  (fn []
    [app-layout
     [:section#main
      [:div.container
       [:div.box-wrap
        [:div.hero
         [:div.container
          [:div.header-image.sized
           [:img {:src "images/about-header@2x.png"}]]
          [:div.page-title [:h1 "About"]]]]
        [:div.body-text
         [:div.container
          [:p.intro-text
           "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, masuada non rutrum ut, ornare ac orci. Lorem ipsum dolor sit amet, consectetur adipiscing. Lorem augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non."]
          [:div.cols
           [:p
            "Morbi nec odio et massa aliquet vehicula in non nisi. Integer commodo massa quis lacus semper venenatis. Aliquam at lectus sit amet lectus maximus commodo in porttitor nulla. Quisque interdum auctor lectus non mattis. Nullam efficitur nisl in lacus varius, nec cursus metus pretium. Donec suscipit lacus tincidunt, malesuada tortor tristique, volutpat metus. Suspendisse ut accumsan neque. In quis dignissim diam. Maecenas accumsan lorem at venenatis placerat. Suspendisse sed velit tempus, ullamcorper diam et, tempus."]
           [:p
            "Morbi ut lectus dolor. Cras quis gravida mauris, at vulputate felis. Maecenas varius tellus eget nisi placerat, eu efficitur felis luctus. Vestibulum nec sapien a neque aliquet semper quis non risus. Suspendisse et risus gravida purus vehicula sollicitudin vitae id odio. Vestibulum quis felis non enim dapibus posuere id vel quam. Etiam molestie justo non quam dictum, eget hendrerit lacus eleifend. Maecenas ut elit et lorem pharetra faucibus. Vivamus porta nisl a mi maximus sagittis. In ac egestas nulla. Duis lorem metus, posuere id consectetur eget, porttitor ornare metus."]
           [:p
            "Quisque interdum auctor lectus non mattis. Nullam efficitur nisl in lacus varius, nec cursus metus pretium. Donec suscipit lacus  tincidunt, malesuada tortor tristique, volutpat metus. Suspendisse ut accumsan neque. In quis dignissim diam. Maecenas accumsan lorem at venenatis placerat. Suspendisse sed velit tempus, ullamcorper diam et, tempus."]
           [:p
            "Morbi ut lectus dolor. Cras quis gravida mauris, at vulputate felis. Maecenas varius tellus eget nisi placerat, eu efficitur felis luctus. Vestibulum nec sapien a neque aliquet semper quis non risus. Suspendisse et risus gravida purus vehicula sollicitudin vitae id odio. Vestibulum quis felis non enim dapibus posuere id vel quam. Etiam molestie justo non quam dictum, eget hendrerit lacus eleifend. Maecenas ut elit et lorem pharetra faucibus. Vivamus porta nisl a mi maximus sagittis. In ac egestas nulla. Duis lorem metus, posuere id consectetur eget, porttitor ornare metus. Nullam at magna sit amet ipsum vulputate consequat sit amet vel quam. Suspendisse efficitur eget lectus in rutrum. Phasellus eget sagittis est. Suspendisse quis eros id dui porta."]]]]]]]]))
