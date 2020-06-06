(ns conduit.ui.components
  (:require [steroid.rn.core :as rn]
            [steroid.rn.components.platform :as platform]
            [steroid.rn.components.other :as other]
            ["react-native-vector-icons/Ionicons" :default ion-icons-class]
            ["react-native" :as react-native]
            [reagent.core :as reagent]
            [steroid.rn.navigation.safe-area :as safe-area]
            [clojure.string :as string]
            [steroid.rn.components.touchable :as touchable]
            [re-frame.core :as re-frame]
            [steroid.rn.components.ui :as ui]))

(def ion-icons (reagent/adapt-react-class ion-icons-class))
(def refresh-control-class (reagent/adapt-react-class react-native/RefreshControl))

(defn text-input [{:keys [style] :as props}]
  [rn/text-input (assoc props
                   :autoCapitalize :none
                   :style (merge {:height             50 :border-width 1
                                  :border-color "rgba(100,100,100,0.4)"
                                  :padding-horizontal 10}
                                 style))])

(defn button [props]
  [rn/view {:style {:align-items :center :margin-vertical 5}}
   [ui/button (merge {:color "#5cb85c"} props)]])

(defn text-button [title handler]
  [rn/view {:style {:align-items :center :margin-vertical 5}}
   [touchable/touchable-opacity {:on-press handler}
    [rn/view {:style {:padding-horizontal 20 :padding-vertical 10}}
     [rn/text {:style {:color "#5cb85c"}} title]]]])

(defn square-button [icon handler]
  [touchable/touchable-opacity {:on-press handler}
   [rn/view {:style {:width 40 :height 40 :justify-content :center}}
    [ion-icons {:name icon :size 30}]]])

(defn back-button
  ([title] (back-button title #(re-frame/dispatch [:navigate-back])))
  ([title handler]
   [rn/view {:style {:flex-direction :row :align-items :center :margin-horizontal 20
                     :margin-vertical 10}}
    [square-button "ios-arrow-back" handler]
    [rn/text {:style {:font-size 20 :margin-left 10}} title]]))

(defn keyboard-avoiding-view [props & children]
  (into [other/keyboard-avoiding-view
         (merge {:style {:flex 1}}
                (when platform/ios? {:behavior :padding})
                props)]
        children))

(defn errors-list [errors]
  [rn/view {:style {:margin-horizontal 20 :margin-top 10}}
   (for [[key [val]] errors]
     ^{:key key} [rn/text {:style {:color "#b85c5c"}}
                  (str (name key) " " val)])])

(defn userpic [image size]
  (let [d size
        r (/ d 2)]
    (if (and (not (string/blank? image)) (string/starts-with? image "http"))
      [rn/image {:style  {:width d :height d :border-radius r}
                 :source {:uri image}}]
      [rn/view {:style {:width        d :height d :border-radius r
                        :border-color :gray
                        :border-width 1}}])))

(defn safe-area-consumer [& children]
  [safe-area/safe-area-consumer
   (fn [insets]
     (reagent/as-element
      (into [rn/view {:style {:flex             1 :padding-top (.-top insets)
                              :background-color :white}}]
            children)))])

(defn refresh-control [props]
  (reagent/as-element [refresh-control-class props]))
