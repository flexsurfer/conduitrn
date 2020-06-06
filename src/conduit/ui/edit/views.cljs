(ns conduit.ui.edit.views
  (:require [re-frame.core :as re-frame]
            [clojure.string :as string]
            [conduit.ui.components :as ui]
            [steroid.rn.core :as rn]))

(defn reset-fields [content refs]
  (fn []
    (re-frame/dispatch [:reset-edit-article])
    (reset! content {})
    (doseq [ref @refs]
      (.clear ^js ref))))

(defn upsert-article [default content-atom refs slug]
  (let [content (merge default @content-atom)]
    (re-frame/dispatch
     [:upsert-article
      {:slug    slug
       :article {:title       (string/trim (or (:title content) ""))
                 :description (string/trim (or (:description content) ""))
                 :body        (string/trim (or (:body content) ""))
                 :tagList     (string/split (:tagList content) #" ")}}
      (reset-fields content-atom refs)])))

(defn editor []
  (let [content (atom {})
        refs (atom #{})]
    (fn []
      (let [{:keys [title description body tagList slug] :as active-article}
            @(re-frame/subscribe [:edit-article])
            tagList (string/join " " tagList)]
        [ui/keyboard-avoiding-view {}
         [ui/safe-area-consumer
          [rn/scroll-view {:style                     {:flex 1}
                           :keyboardShouldPersistTaps :always}
           [ui/text-input {:ref #(when % (swap! refs conj %))
                           :style          {:margin-horizontal 20 :margin-vertical 10}
                           :on-change-text #(swap! content assoc :title %)
                           :placeholder    "Article Title"
                           :default-value  title}]
           [ui/text-input {:ref #(when % (swap! refs conj %))
                           :style          {:margin-horizontal 20 :margin-vertical 10}
                           :on-change-text #(swap! content assoc :description %)
                           :placeholder    "What's this article about?"
                           :default-value  description}]
           [ui/text-input {:ref #(when % (swap! refs conj %))
                           :style          {:margin-horizontal 20 :margin-vertical 10
                                            :height            300}
                           :on-change-text #(swap! content assoc :body %)
                           :placeholder    "Write your article (in markdown)"
                           :default-value  body}]
           [ui/text-input {:ref #(when % (swap! refs conj %))
                           :style          {:margin-horizontal 20 :margin-top 10
                                            :margin-bottom     40}
                           :on-change-text #(swap! content assoc :tagList %)
                           :placeholder    "Enter tags"
                           :default-value  (str tagList)}]
           [ui/button {:on-press #(upsert-article {:title title :description description
                                                   :body body :tagList tagList :slug slug}
                                                  content
                                                  refs
                                                  slug)
                       :title    (if active-article
                                   "Update Article"
                                   "Publish Article")}]
           (when (or active-article (seq @content))
             [rn/view {:style {:margin-top 40}}
              [ui/button {:on-press (reset-fields content refs)
                          :color    "#b85c5c"
                          :title    "Cancel"}]])]]]))))
