(ns conduit.ui.signin.views
  (:require [reagent.core :as reagent]
            [steroid.rn.core :as rn]
            [steroid.rn.navigation.safe-area :as safe-area]
            [conduit.ui.components :as ui]
            [re-frame.core :as re-frame]
            [clojure.string :as string]))

(defn login
  [sign-in?]
  (let [default     {:email "" :password ""}
        credentials (reagent/atom default)]
    (fn []
      (let [{:keys [email password]} @credentials
            loading @(re-frame/subscribe [:loading])
            errors  @(re-frame/subscribe [:errors])]
        [rn/view {:style {:flex 1}}
         [rn/text {:style {:font-size 30 :align-self :center}} "Sign in"]
         [ui/text-button "Need an account?" #(swap! sign-in? not)]
         (when (:login errors)
           [ui/errors-list (:login errors)])
         [ui/text-input {:style          {:margin-horizontal 20 :margin-vertical 10}
                         :on-change-text #(swap! credentials assoc :email %)
                         :editable       (not (:login loading))
                         :placeholder    "Email"}]
         [ui/text-input {:style           {:margin-horizontal 20 :margin-vertical 10}
                         :on-change-text  #(swap! credentials assoc :password %)
                         :secureTextEntry true
                         :editable        (not (:login loading))
                         :placeholder     "Password"}]
         [ui/button {:title    "Sign in"
                     :on-press #(re-frame/dispatch [:login @credentials])
                     :disabled (or (:login loading)
                                   (string/blank? email)
                                   (string/blank? password))}]]))))

(defn register
  [sign-in?]
  (let [default      {:username "" :email "" :password ""}
        registration (reagent/atom default)]
    (fn []
      (let [{:keys [username email password]} @registration
            loading @(re-frame/subscribe [:loading])
            errors  @(re-frame/subscribe [:errors])]
        [rn/view {:style {:flex 1}}
         [rn/text {:style {:font-size 30 :align-self :center}} "Sign up"]
         [ui/text-button "Have an account?" #(swap! sign-in? not)]
         (when (:register-user errors)
           [ui/errors-list (:register-user errors)])
         [ui/text-input {:style          {:margin-horizontal 20 :margin-vertical 10}
                         :on-change-text #(swap! registration assoc :username %)
                         :editable       (not (:register-user loading))
                         :placeholder    "Your Name"}]
         [ui/text-input {:style          {:margin-horizontal 20 :margin-vertical 10}
                         :on-change-text #(swap! registration assoc :email %)
                         :editable       (not (:register-user loading))
                         :placeholder    "Email"}]
         [ui/text-input {:style           {:margin-horizontal 20 :margin-vertical 10}
                         :on-change-text  #(swap! registration assoc :password %)
                         :editable        (not (:register-user loading))
                         :secureTextEntry true
                         :placeholder     "Password"}]
         [ui/button {:title    "Sign up"
                     :on-press #(re-frame/dispatch [:register-user @registration])
                     :disabled (or (:register-user loading)
                                   (string/blank? username)
                                   (string/blank? email)
                                   (string/blank? password))}]]))))

(defn sign-in-modal []
  (let [sign-in? (reagent/atom true)]
    (fn []
      [rn/view {:style {:flex 1 :background-color :white}}
       [safe-area/safe-area-view {:style {:flex 1}}
        [ui/keyboard-avoiding-view {}
         [rn/scroll-view {:style                     {:flex 1}
                          :keyboardShouldPersistTaps :always}
          [rn/view {:style {:align-items :center :margin-bottom 100}}
           [rn/text {:style {:font-size 25 :color "#5cb85c" :font-weight "700"}} "conduit"]]
          (if @sign-in?
            [login sign-in?]
            [register sign-in?])]]]])))