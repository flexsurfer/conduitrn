(ns conduit.fx
  (:require [re-frame.core :as re-frame]
            [steroid.rn.components.async-storage :as async-storage]
            ["react-native-splash-screen" :default splash-screen]))

;; ALL SIDE EFFECTS ARE HERE

;; -- Async Storage  ----------------------------------------------------------
;;
;; Part of the conduit challenge is to store a user in Async Storage, and
;; on app startup, reload the user from when the program was last run.
;;

(def conduit-user-key "conduit-user")  ;;  key

(re-frame/reg-fx
 :store-user-in-ls
 (fn [user]
   (async-storage/set-item conduit-user-key user)))

(re-frame/reg-fx
 :remove-user-from-ls
 (fn []
   (async-storage/remove-item conduit-user-key)))

(re-frame/reg-fx
 :get-user-from-ls
 (fn [cb]
   (async-storage/get-item conduit-user-key cb)))

;; -- Splash screen  ----------------------------------------------------------
;;
;; when app is ready we can hide splash screen
;; see :set-user-from-storage event in events.cljs

(re-frame/reg-fx
 :hide-splash
 (fn []
   (.hide splash-screen)))