(ns conduit.api
  (:require [clojure.string :as string]))

(def api-url "https://conduit.productionready.io/api")

(defn endpoint
  "Concat any params to api-url separated by /"
  [& params]
  (string/join "/" (concat [api-url] params)))

(defn auth-header
  "Get user token and format for API authorization"
  [db]
  (when-let [token (get-in db [:user :token])]
    {"Authorization" (str "Token " token)}))