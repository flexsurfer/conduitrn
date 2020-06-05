(ns conduit.subs
  (:require [re-frame.core :as re-frame]
            [steroid.subs :as subs]
            [conduit.utils :as utils]))

;; reg-root-subs is a syntax sugar for registering subscriptions for root keys
;; equal to (re-frame/reg-sub :active-page (fn [db _] (get db :active-page)))

(subs/reg-root-subs #{:active-page :articles-count :tags :profile :loading :filter ;; usage: (subscribe [:active-page])
                      :errors :user})
(subs/reg-root-sub :articles-root :articles) ;; usage: (subscribe [:articles-root])
(subs/reg-root-sub :comments-root :comments)
(subs/reg-root-sub :feed-articles-root :feed-articles)
(subs/reg-root-sub :filtered-articles-root :filtered-articles)

(re-frame/reg-sub
 :articles                              ;; usage: (subscribe [:articles])
 :<- [:articles-root]
 (fn [articles]                             ;; db is the (map) value stored in the app-db atom
   (->> articles                ;; ->> is a thread last macro - pass atricles as last arg of:
        (vals)                          ;; vals, just as we would write (vals articles), then pass the result to:
        (sort-by :epoch utils/reverse-cmp)))) ;; sort-by epoch in reverse order

(re-frame/reg-sub
 :feed-articles                              ;; usage: (subscribe [:feed-articles])
 :<- [:feed-articles-root]
 (fn [articles]                             ;; db is the (map) value stored in the app-db atom
   (->> articles                ;; ->> is a thread last macro - pass atricles as last arg of:
        (vals)                          ;; vals, just as we would write (vals articles), then pass the result to:
        (sort-by :epoch utils/reverse-cmp)))) ;; sort-by epoch in reverse order

(re-frame/reg-sub
 :filtered-articles                              ;; usage: (subscribe [:filtered-articles])
 :<- [:filtered-articles-root]
 (fn [articles]                             ;; db is the (map) value stored in the app-db atom
   (->> articles                ;; ->> is a thread last macro - pass atricles as last arg of:
        (vals)                          ;; vals, just as we would write (vals articles), then pass the result to:
        (sort-by :epoch utils/reverse-cmp)))) ;; sort-by epoch in reverse order

(re-frame/reg-sub
 :active-article ;; usage (subscribe [:active-article])
 (fn [db _]
   (or (get-in db [:articles (:active-article db)])
       (get-in db [:feed-articles (:active-article db)])
       (get-in db [:filtered-articles (:active-article db)]))))

(re-frame/reg-sub
 :comments ;; usage: (subscribe [:comments])
 :<- [:comments-root]
 (fn [comments]
   (->> comments
        (vals)
        (sort-by :epoch utils/reverse-cmp))))
