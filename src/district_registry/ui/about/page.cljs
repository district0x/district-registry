(ns district-registry.ui.about.page
  (:require
   [district-registry.ui.components.app-layout :refer [app-layout]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [re-frame.core :refer [subscribe]]))

(defmethod page :route/about []
  (fn []
    [app-layout
     [:div#main
      [:div.container
       [:div.box-wrap
        [:div.hero
         [:div.container
          [:div.header-image.sized
           [:img {:src "images/about-header@2x.png"}]]
          [:div.page-title
           [:h1 "About"]]]]
        [:div.body-text
         [:div.container
          [:p.intro-text
           "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, masuada non rutrum ut, ornare ac orci. Lorem ipsum dolor sit amet, consectetur adipiscing. Lorem augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non."]
          [:div.cols
           [:p "Morbi nec odio et massa aliquet vehicula in non nisi. Integer commodo massa quis lacus semper venenatis. Aliquam at lectus sit amet lectus maximus commodo in porttitor nulla. Quisque interdum auctor lectus non mattis. Nullam efficitur nisl in lacus varius, nec cursus metus pretium. Donec suscipit lacus tincidunt, malesuada tortor tristique, volutpat metus. Suspendisse ut accumsan neque. In quis dignissim diam. Maecenas accumsan lorem at venenatis placerat. Suspendisse sed velit tempus, ullamcorper diam et, tempus."]
           [:p "Morbi nec odio et massa aliquet vehicula in non nisi. Integer commodo massa quis lacus semper venenatis. Aliquam at lectus sit amet lectus maximus commodo in porttitor nulla. Quisque interdum auctor lectus non mattis. Nullam efficitur nisl in lacus varius, nec cursus metus pretium. Donec suscipit lacus tincidunt, malesuada tortor tristique, volutpat metus. Suspendisse ut accumsan neque. In quis dignissim diam. Maecenas accumsan lorem at venenatis placerat. Suspendisse sed velit tempus, ullamcorper diam et, tempus."]
           [:p "Morbi nec odio et massa aliquet vehicula in non nisi. Integer commodo massa quis lacus semper venenatis. Aliquam at lectus sit amet lectus maximus commodo in porttitor nulla. Quisque interdum auctor lectus non mattis. Nullam efficitur nisl in lacus varius, nec cursus metus pretium. Donec suscipit lacus tincidunt, malesuada tortor tristique, volutpat metus. Suspendisse ut accumsan neque. In quis dignissim diam. Maecenas accumsan lorem at venenatis placerat. Suspendisse sed velit tempus, ullamcorper diam et, tempus."]
           [:p "Morbi nec odio et massa aliquet vehicula in non nisi. Integer commodo massa quis lacus semper venenatis. Aliquam at lectus sit amet lectus maximus commodo in porttitor nulla. Quisque interdum auctor lectus non mattis. Nullam efficitur nisl in lacus varius, nec cursus metus pretium. Donec suscipit lacus tincidunt, malesuada tortor tristique, volutpat metus. Suspendisse ut accumsan neque. In quis dignissim diam. Maecenas accumsan lorem at venenatis placerat. Suspendisse sed velit tempus, ullamcorper diam et, tempus."]]]]]]]]))
