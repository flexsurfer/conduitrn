(ns conduit.ui.edit.views
  (:require [re-frame.core :as re-frame]
            [clojure.string :as string]
            [reagent.core :as reagent]
            [conduit.ui.components :as ui]
            [steroid.rn.core :as rn]))

(defn upsert-article [content slug]
  (re-frame/dispatch
   [:upsert-article
    {:slug    slug
     :article {:title       (string/trim (or (:title content) ""))
               :description (string/trim (or (:description content) ""))
               :body        (string/trim (or (:body content) ""))
               :tagList     (string/split (:tagList content) #" ")}}]))

(defn editor []
  (let [{:keys [title description body tagList]}
        @(re-frame/subscribe [:active-article])
        default {:title title :description description :body body :tagList tagList}
        content (reagent/atom default)]
    (fn []
      (let [{:keys [title description body tagList slug] :as active-article}
            @(re-frame/subscribe [:active-article])
            tagList (string/join " " tagList)]
        [ui/keyboard-avoiding-view {}
         [ui/safe-area-consumer
          [rn/scroll-view {:style                     {:flex 1}
                           :keyboardShouldPersistTaps :always}
           [ui/text-input {:style          {:margin-horizontal 20 :margin-vertical 10}
                           :on-change-text #(swap! content assoc :title %)
                           :placeholder    "Article Title"
                           :default-value  title}]
           [ui/text-input {:style          {:margin-horizontal 20 :margin-vertical 10}
                           :on-change-text #(swap! content assoc :description %)
                           :placeholder    "What's this article about?"
                           :default-value  description}]
           [ui/text-input {:style          {:margin-horizontal 20 :margin-vertical 10
                                            :height            300}
                           :on-change-text #(swap! content assoc :body %)
                           :placeholder    "Write your article (in markdown)"
                           :default-value  body}]
           [ui/text-input {:style          {:margin-horizontal 20 :margin-top 10
                                            :margin-bottom     40}
                           :on-change-text #(swap! content assoc :tagList %)
                           :placeholder    "Enter tags"
                           :default-value  tagList}]
           [ui/button {:on-press #(upsert-article @content slug)
                       :title    (if active-article
                                   "Update Article"
                                   "Publish Article")}]
           (when active-article
             [rn/view {:style {:margin-top 40}}
              [ui/button {:on-press #(re-frame/dispatch [:reset-active-article])
                          :color    "#b85c5c"
                          :title    "Cancel"}]])]]]))))
