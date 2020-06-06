(ns conduit.ui.tags.views
  (:require [conduit.ui.components :as ui]
            [re-frame.core :as re-frame]
            [steroid.rn.navigation.safe-area :as safe-area]
            [steroid.rn.components.list :as list]
            [steroid.rn.core :as rn]
            [steroid.rn.components.touchable :as touchable]))

(defn tags-list-item [tag]
  [touchable/touchable-opacity {:on-press #(re-frame/dispatch [:set-active-page {:page :tag
                                                                                 :tag  tag}])}
   [rn/view {:style {:margin-horizontal 20 :margin-vertical 10}}
    [rn/text {:style {:font-weight "700" :color "#5cb85c" :font-size 18}} (str "#" tag)]]])

(defn tags []
  (let [tags @(re-frame/subscribe [:tags])]
    [ui/keyboard-avoiding-view {}
     [safe-area/safe-area-view {:style {:flex             1
                                        :background-color :white}}
      [ui/back-button "Tags"]
      [list/flat-list
       {:key-fn    identity
        :data      tags
        :render-fn tags-list-item}]]]))