(ns conduit.utils
  (:require [re-frame.interop :as interop]
            [reagent.impl.batching :as batching]))

(set! interop/next-tick js/setTimeout)
(set! batching/fake-raf #(js/setTimeout % 0))

(defn format-date
  [date]
  (.toDateString (js/Date. date)))

(defn add-epoch
  "Takes date identifier and adds :epoch (cljs-time.coerce/to-long) timestamp to coll"
  [date coll]
  (map (fn [item] (assoc item :epoch (.getTime (js/Date.) date))) coll))

(defn index-by
  "Transform a coll to a map with a given key as a lookup value"
  [key coll]
  (into {} (map (juxt key identity) (add-epoch :createdAt coll))))

(defn reverse-cmp                                           ;; https://clojure.org/guides/comparators
  "Sort numbers in decreasing order, i.e.: calls compare with the arguments in the opposite order"
  [a b]
  (compare b a))