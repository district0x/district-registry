(ns district-registry.ui.components.donut-chart
  (:require
    [cljsjs.d3]
    [reagent.core :as r]))

(defn donut-chart [{:keys [:reg-entry/address :challenge/index :challenge/votes-include :challenge/votes-exclude]}]
  (r/create-class
    {:reagent-render (fn [{:keys [:reg-entry/address :challenge/index :challenge/votes-include :challenge/votes-exclude]}]
                       [:div.donut-chart {:id (str "donutchart-" address "-" index)
                                          :key (str "donutchart-" address "-" index)}])
     :component-did-mount (fn []
                            (let [width 150
                                  height 150
                                  icon-width 50
                                  icon-height 50
                                  data [{:challenge/votes :for :value votes-include}
                                        {:challenge/votes :against :value votes-exclude}]
                                  outer-radius (/ (min width height) 2)
                                  inner-radius (/ outer-radius 2)
                                  arc (-> js/d3
                                        .arc
                                        (.outerRadius outer-radius)
                                        (.innerRadius inner-radius))
                                  pie (-> js/d3
                                        .pie
                                        (.value (fn [d] (aget d "value"))))
                                  color-scale (-> js/d3
                                                .scaleOrdinal
                                                (.range (clj->js ["#23fdd8" "#2c398f"])))]
                              (-> js/d3
                                (.select (str "#donutchart-" address "-" index))
                                (.append "svg")
                                (.attr "class" (str "chart-" address "-" index))
                                (.attr "width" width)
                                (.attr "height" height)
                                (.append "g")
                                (.attr "transform" (str "translate(" (/ width 2) "," (/ height 2) ")"))
                                (.selectAll ".arc")
                                (.data (pie (clj->js data)))
                                (.enter)
                                (.append "g")
                                (.attr "class" "arc")
                                (.append "path")
                                (.attr "d" arc)
                                (.style "fill" (fn [d]
                                                 (color-scale
                                                   (aget d "data" "votes")))))

                              (-> js/d3
                                (.select (str ".chart-" address "-" index))
                                (.append "g")
                                (.attr "transform" (str "translate(" (/ width 2) "," (/ height 2) ")"))
                                (.append "image")
                                (.attr "width" icon-width)
                                (.attr "height" icon-height)
                                (.attr "x" (unchecked-negate (/ icon-width 2)))
                                (.attr "y" (unchecked-negate (/ icon-height 2)))
                                (.attr "xlink:href" "/images/district0x-planet.png"))))}))