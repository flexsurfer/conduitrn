(ns conduit.subs
  (:require [re-frame.core :as re-frame]
            [steroid.subs :as subs]
            [conduit.utils :as utils]))

;; reg-root-subs is a syntax sugar for registering subscriptions for root keys
;; equal to (re-frame/reg-sub :active-page (fn [db _] (get db :active-page)))

(subs/reg-root-subs #{:articles :active-page :articles-count :tags :profile :loading :filter ;; usage: (subscribe [:active-page])
                      :errors :user})
(subs/reg-root-sub :comments-root :comments)                ;; usage: (subscribe [:comments-root])
(subs/reg-root-sub :feed-articles-root :feed-articles)
(subs/reg-root-sub :filtered-articles-root :filtered-articles)
(subs/reg-root-sub :global-articles-root :global-articles)

(re-frame/reg-sub
 :global-articles                                           ;; usage: (subscribe [:global-articles])
 :<- [:articles]
 :<- [:global-articles-root]
 (fn [[articles global-articles]]
   (vals (select-keys articles global-articles))))          ;;we don't sort by epoch because its broken on server

(re-frame/reg-sub
 :feed-articles                                             ;; usage: (subscribe [:feed-articles])
 :<- [:articles]
 :<- [:feed-articles-root]
 (fn [[articles feed-articles]]
   (vals (select-keys articles feed-articles))))

(re-frame/reg-sub
 :filtered-articles                                         ;; usage: (subscribe [:filtered-articles])
 :<- [:articles]
 :<- [:filtered-articles-root]
 (fn [[articles filtered-articles]]
   (vals (select-keys articles filtered-articles))))

(re-frame/reg-sub
 :active-article                                            ;; usage (subscribe [:active-article])
 (fn [db _]
   (get-in db [:articles (:active-article db)])))

(re-frame/reg-sub
 :edit-article                                              ;; usage (subscribe [:edit-article])
 (fn [db _]
   (get-in db [:articles (:edit-article db)])))

(re-frame/reg-sub
 :comments                                                  ;; usage: (subscribe [:comments])
 :<- [:comments-root]
 (fn [comments]
   (->> comments
        (vals)
        (sort-by :epoch utils/reverse-cmp))))
