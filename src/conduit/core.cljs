(ns conduit.core
  (:require [steroid.rn.core :as rn]
            [conduit.ui.views :as views]
            conduit.events
            conduit.subs
            conduit.fx))

;; -- Entry Point -------------------------------------------------------------
;; shadow-cljs.edn       :init-fn          conduit.core/init

(defn init []
  (rn/register-comp "conduitrn" views/root-stack))