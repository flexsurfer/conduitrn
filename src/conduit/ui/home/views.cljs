(ns conduit.ui.home.views
  (:require [re-frame.core :as re-frame]
            [conduit.ui.components :as ui]
            [steroid.rn.core :as rn]
            [steroid.rn.components.touchable :as touchable]
            [steroid.rn.components.list :as list]
            [steroid.rn.components.other :as other]
            [conduit.utils :as utils]
            [steroid.rn.navigation.safe-area :as safe-area]))

(defn home-list-item
  [{:keys [description slug createdAt title author favoritesCount favorited]
    :or   {slug "" author {:username ""}}}]
  [rn/view {:style {:padding-left 20 :padding-bottom 40}}
   [rn/view {:style {:flex-direction :row}}
    [touchable/touchable-opacity
     {:on-press #(do (re-frame/dispatch [:set-active-page {:page :article
                                                           :slug slug}])
                     (re-frame/dispatch [:navigate-to :article]))
      :style    {:flex 1}}
     [rn/view {:style {:flex-direction :row :flex 1}}
      [rn/view {:style {:flex 1}}
       [rn/text {:style           {:font-weight "600" :font-size 22}
                 :number-of-lines 3}
        title " - " description]
       [rn/view {:style {:flex-direction  :row :flex 1 :align-items :center
                         :margin-vertical 2}}
        [ui/userpic (:image author) 14]
        [rn/text {:style {:color :gray}}
         " " (:username author) " · " (utils/format-date createdAt)]]
       [rn/text {:style {:color :gray :font-size 20 :font-weight "700"}}
        "..."]]]]
    [touchable/touchable-opacity
     {:on-press #(re-frame/dispatch [:toggle-favorite-article slug])
      :style    {:padding-horizontal 20}}
     [rn/text {:style {:color (if favorited "#5cb85c" :gray) :font-size 18}}
      [ui/ion-icons {:name "md-heart" :size 18}]
      " "
      favoritesCount]]]])

(defn screen [{:keys [title data loading? on-refresh on-end back? accesories]}]
  [rn/view {:style {:flex 1}}
   [rn/view {:style {:flex-direction :row :align-items :center :justify-content :space-between}}
    (if back?
      [ui/back-button title]
      [rn/text {:style {:font-size   22 :margin-horizontal 20 :margin-top 20
                        :font-weight "600" :margin-bottom 10}}
       title])
    (when accesories
      [ui/square-button "md-search" accesories])]
   [rn/view {:style {:height            1 :background-color :gray
                     :margin-horizontal 20
                     :opacity           0.2}}]
   [list/flat-list
    {:key-fn                :slug
     :data                  data
     :header                [rn/view {:style {:height 40}}]
     :footer                [rn/view {:style {:align-items :center :justify-content :center}}
                             (when loading?
                               [other/activity-indicator])]
     :render-fn             home-list-item
     :onEndReachedThreshold 0.4
     :refresh-control       (ui/refresh-control
                             {:refreshing loading?
                              :onRefresh  on-refresh})
     :on-end-reached        on-end}]])

(defn book []
  (let [loading  @(re-frame/subscribe [:loading])
        articles @(re-frame/subscribe [:feed-articles])]
    [ui/safe-area-consumer
     [screen
      {:title      "Your feed"
       :data       articles
       :loading?   (:feed-articles loading)
       :on-refresh #(re-frame/dispatch [:get-feed-articles])
       :on-end     #(re-frame/dispatch [:get-more-feed-articles])}]]))

(defn user-articles []
  (let [loading  @(re-frame/subscribe [:loading])
        articles @(re-frame/subscribe [:filtered-articles])]
    [safe-area/safe-area-view {:style {:flex             1
                                       :background-color :white}}
     [screen
      {:title    "My articles"
       :data     articles
       :loading? (:articles loading)
       :back?    true}]]))

(defn fav []
  (let [loading  @(re-frame/subscribe [:loading])
        articles @(re-frame/subscribe [:filtered-articles])]
    [safe-area/safe-area-view {:style {:flex             1
                                       :background-color :white}}
     [screen
      {:title    "Favorited articles"
       :data     articles
       :loading? (:articles loading)
       :back?    true}]]))

(defn tag []
  (let [loading  @(re-frame/subscribe [:loading])
        filter  @(re-frame/subscribe [:filter])
        articles @(re-frame/subscribe [:filtered-articles])]
    [safe-area/safe-area-view {:style {:flex             1
                                       :background-color :white}}
     [screen
      {:title    (str "#" (:tag filter))
       :data     articles
       :loading? (:articles loading)
       :on-refresh #(re-frame/dispatch [:get-articles {:tag (:tag filter)}])
       :on-end     #(re-frame/dispatch [:get-more-articles (:tag filter)])
       :back?    true}]]))

(defn home []
  (let [loading  @(re-frame/subscribe [:loading])
        articles @(re-frame/subscribe [:global-articles])]
    [ui/safe-area-consumer
     [screen
      {:title      "Global feed"
       :data       articles
       :loading?   (:articles loading)
       :accesories #(re-frame/dispatch [:set-active-page {:page :tags}])
       :on-refresh #(re-frame/dispatch [:get-articles])
       :on-end     #(re-frame/dispatch [:get-more-articles])}]]))