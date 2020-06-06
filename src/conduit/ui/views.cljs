(ns conduit.ui.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [steroid.rn.navigation.safe-area :as safe-area]
            [conduit.ui.components :as ui]
            [conduit.ui.signin.views :as signin]
            [conduit.ui.home.views :as home]
            [cljs-bean.core :as bean]
            [steroid.rn.navigation.bottom-tabs :as bottom-tabs]
            [steroid.rn.navigation.core :as rnn]
            [steroid.rn.navigation.stack :as stack]
            [steroid.rn.navigation.safe-area :as safe-area]
            [conduit.ui.profile.views :as profile]
            [conduit.ui.edit.views :as editor]
            [conduit.ui.settings.views :as settings]
            [conduit.ui.article.views :as article]
            [conduit.ui.tags.views :as tags]
            [steroid.rn.components.platform :as platform]
            [steroid.rn.components.status-bar :as status-bar]))

(when platform/android?
  (status-bar/set-bar-style "dark-content")
  (status-bar/set-translucent true))

(def tab-icons
  {"home"    "md-home"
   "book"    "md-bookmark"
   "edit"    "md-create"
   "profile" "md-person"})

(defn screen-options [options]
  (let [{:keys [route]} (bean/->clj options)]
    #js {:tabBarIcon
         (fn [data]
           (let [{:keys [color]} (bean/->clj data)
                 icon (get tab-icons (:name route))]
             (reagent/as-element
              [ui/ion-icons {:name icon :color color :size 30}])))}))

(defn tabs []
  [bottom-tabs/bottom-tab
   {:screenOptions screen-options
    :tabBarOptions {:activeTintColor   "#5cb85c"
                    :inactiveTintColor :black
                    :showLabel         false}}
   [{:name      :home
     :component home/home}
    {:name      :book
     :component home/book}
    {:name      :edit
     :component editor/editor}
    {:name      :profile
     :component profile/profile}]])

(defn root-stack []
  [safe-area/safe-area-provider
   [(rnn/create-navigation-container-reload                 ;; navigation container with shadow-cljs hot reload
     {:on-ready #(re-frame/dispatch [:initialise-app])}     ;; when navigation initialized and mounted initialize the app
     [stack/stack {:mode :modal :header-mode :none}
      [{:name      :main
        :component tabs}
       {:name      :sign-in
        :component signin/sign-in-modal
        :options   {:gestureEnabled false}}
       {:name      :settings
        :component settings/settings}
       {:name      :article
        :component article/article}
       {:name      :user-favorite
        :component home/fav}
       {:name      :user-articles
        :component home/user-articles}
       {:name      :tags
        :component tags/tags}
       {:name      :tag
        :component home/tag}]])]])