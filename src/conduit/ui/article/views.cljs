(ns conduit.ui.article.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [conduit.utils :as utils]
            [steroid.rn.core :as rn]
            [conduit.ui.components :as ui]
            [steroid.rn.navigation.safe-area :as safe-area]
            [steroid.rn.components.touchable :as touchable]))

(defn like [slug favorited favoritesCount]
  [touchable/touchable-opacity
   {:on-press #(re-frame/dispatch [:toggle-favorite-article slug])
    :style    {:padding-horizontal 20}}
   [rn/text {:style {:color (if favorited "#5cb85c" :gray) :font-size 18}}
    [ui/ion-icons {:name "md-heart" :size 18}]
    " "
    favoritesCount]])

(defn follow [username profile]
  [rn/view {:style {:height 25}}
   (when profile
     (let [{:keys [following]} profile]
       [touchable/touchable-opacity
        {:on-press #(re-frame/dispatch [:toggle-follow-user username])}
        [rn/view {:style {:align-items  :center :justify-content :center
                          :align-self   :flex-start :padding-horizontal 4 :border-radius 2
                          :border-width 1 :border-color "rgba(100,100,100,0.4)"}}
         [rn/text (if following " - Unfollow" " + Follow")]]]))])


(defn edit [slug]
  [touchable/touchable-opacity
   {:on-press #(do (re-frame/dispatch [:set-active-page {:page :editor :slug slug}])
                   (re-frame/dispatch [:navigate-to :edit]))}
   [rn/view {:style {:align-items  :center :justify-content :center
                     :align-self   :flex-start :padding-horizontal 4 :border-radius 2
                     :border-width 1 :border-color "rgba(100,100,100,0.4)"}}
    [rn/text "Edit"]]])

(defn delete [id]
  [touchable/touchable-opacity
   {:on-press #(re-frame/dispatch [:delete-comment id])}
   [rn/view {:style {:align-items  :center :justify-content :center
                     :margin-left  10
                     :align-self   :flex-start :padding-horizontal 4 :border-radius 2
                     :border-width 1 :border-color "rgba(100,100,100,0.4)"}}
    [rn/text "Delete"]]])

(defn post-comment [comment default]
  (re-frame/dispatch [:post-comment {:body (get @comment :body)}])
  (reset! comment default))

(defn comments-view [comments username]
  (let [comment (reagent/atom {:body ""})]
    (fn [comments username]
      [rn/view
       [rn/text {:style {:margin-top 50 :margin-bottom 20}} "Comments"]
       (for [{:keys [id createdAt body author]} comments]
         ^{:key id}
         [rn/view
          [rn/text body]
          [rn/view {:style {:flex-direction  :row :flex 1 :align-items :center
                            :margin-vertical 15}}
           [ui/userpic (:image author) 18]
           [rn/text {:style {:font-size 13}}
            " " (:username author)
            [rn/text {:style {:color :gray}} " · " (utils/format-date createdAt)]]
           (when (= username (:username author))
             [delete id])]])
       [ui/text-input {:style          {:margin-vertical 10
                                        :height          100}
                       :default-value  (:body @comment)
                       :on-change-text #(swap! comment assoc :body %)
                       :multiline      true
                       :placeholder    "Write a comment..."}]
       [ui/button {:title "Post Comment" :on-press #(post-comment comment {:body ""})}]])))

(defn tags-list [tagsList]
  [rn/view {:style {:flex-direction :row :flex-wrap :wrap}}
   (for [tag tagsList]
     ^{:key tag}
     [rn/view {:style {:align-items  :center :justify-content :center
                       :margin-right 5 :margin-top 5
                       :align-self   :flex-start :padding-horizontal 4 :border-radius 8
                       :border-width 1 :border-color "rgba(100,100,100,0.4)"}}
      [rn/text tag]])])

(defn article []
  (let [{:keys [title description author createdAt body slug favorited favoritesCount tagList]}
        @(re-frame/subscribe [:active-article])
        user     @(re-frame/subscribe [:user])
        comments @(re-frame/subscribe [:comments])
        profile  @(re-frame/subscribe [:profile])
        username (:username author)]
    [safe-area/safe-area-view {:style {:flex             1
                                       :background-color :white}}
     [ui/back-button "" #(do
                           (re-frame/dispatch [:reset-active-article])
                           (re-frame/dispatch [:navigate-back]))]
     [rn/scroll-view {:style                     {:flex 1}
                      :keyboardShouldPersistTaps :always}
      [rn/view {:style {:margin-horizontal 20 :flex 1}}
       [rn/view {:style {:flex-direction :row :flex 1 :align-items :center}}
        [rn/text {:style           {:font-size 30 :flex 1}
                  :number-of-lines 3}
         title " - " description]
        [like slug favorited favoritesCount]]
       [tags-list tagList]
       [rn/view {:style {:flex-direction  :row :flex 1 :align-items :center
                         :margin-vertical 15}}
        [ui/userpic (:image author) 20]
        [rn/text {:style {:font-size 16}}
         " " username
         [rn/text {:style {:color :gray}} " · " (utils/format-date createdAt)]]]
       (if (= username (:username user))
         [edit slug]
         [follow username profile])
       [rn/text {:style {:font-size 20 :margin-top 30}} body]
       [comments-view comments (:username user)]]]]))