(ns conduit.ui.settings.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [conduit.ui.components :as ui]
            [steroid.rn.core :as rn]))

(defn settings []
  (let [{:keys [bio email image username]} @(re-frame/subscribe [:user])
        default     {:bio bio :email email :image image :username username}
        user-update (reagent/atom default)]
    (fn []
      (let [{:keys [bio email image username]} @(re-frame/subscribe [:user])
            loading @(re-frame/subscribe [:loading])]
        [ui/keyboard-avoiding-view {}
         [ui/safe-area-consumer
          [ui/back-button "Edit profile"]
          [rn/scroll-view {:style                     {:flex 1}
                           :keyboardShouldPersistTaps :always}
           [ui/text-input {:style          {:margin-horizontal 20 :margin-vertical 10}
                           :on-change-text #(swap! user-update assoc :image %)
                           :placeholder    "URL of profile picture"
                           :default-value  image}]
           [ui/text-input {:style          {:margin-horizontal 20 :margin-vertical 10}
                           :on-change-text #(swap! user-update assoc :username %)
                           :placeholder    "Your Name"
                           :default-value  username}]
           [ui/text-input {:style          {:margin-horizontal 20 :margin-vertical 10
                                            :height            150}
                           :on-change-text #(swap! user-update assoc :bio %)
                           :multiline      true
                           :placeholder    "Short bio about you"
                           :default-value  bio}]
           [ui/text-input {:style          {:margin-horizontal 20 :margin-vertical 10}
                           :on-change-text #(swap! user-update assoc :email %)
                           :placeholder    "Email"
                           :default-value  email}]
           [ui/text-input {:style           {:margin-horizontal 20 :margin-top 10
                                             :margin-bottom 30}
                           :on-change-text  #(swap! user-update assoc :password %)
                           :secureTextEntry true
                           :placeholder     "Password"}]
           [ui/button {:disabled (:update-user loading)
                       :on-press #(re-frame/dispatch [:update-user @user-update])
                       :title    "Update"}]]]]))))