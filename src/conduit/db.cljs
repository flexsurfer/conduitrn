(ns conduit.db)

;; -- Default app-db Value  ---------------------------------------------------
;;
;; When the application first starts, this will be the value put in app-db
;; Look in: `events.cljs` for the registration of :initialise-app handler
;;
(def default-db {:active-page       :home
                 :global-articles   #{}
                 :feed-articles     #{}
                 :filtered-articles #{}})                   ;; what gets put into app-db by default.