(ns conduit.events
  (:require
   [conduit.db :as db]
   [re-frame.core :as re-frame]
   [conduit.api :as api]
   [conduit.utils :as utils]
   superstructor.re-frame.fetch-fx
   steroid.rn.navigation.events))

(re-frame/reg-event-fx                                      ;; usage: (dispatch [:initialise-app])
 :initialise-app                                            ;; gets user from localstore, and puts into coeffects arg
 ;; the event handler (function) being registered
 (fn [_ _]                                                  ;; take 2 vals from coeffects. Ignore event vector itself.
   {:db               db/default-db                         ;; what it returns becomes the new application state}))
    :get-user-from-ls #(re-frame/dispatch [:set-user-from-storage %])})) ;; get user from ls

(re-frame/reg-event-fx
 :set-user-from-storage
 (fn [{db :db} [_ user]]
   (merge (if user                                          ;; if user signed in we can get user data from ls, in that case we navigate to home
            {:db       (assoc db :user user)
             :dispatch [:set-active-page {:page :home}]}
            {:dispatch [:navigate-to :sign-in]})            ;; overwise open sig-in modal screen
          {:hide-splash nil})))

(re-frame/reg-event-fx                                      ;; usage: (dispatch [:set-active-page {:page :home})
 :set-active-page                                           ;; triggered when the user clicks on a link that redirects to a another page
 (fn [{:keys [db]} [_ {:keys [page slug profile favorited tag]}]] ;; destructure 2nd parameter to obtain keys
   (let [set-page (assoc db :active-page page)]
     (case page
       ;; -- HOME --------------------------------------------------------
       :home {:db         set-page
              :dispatch-n [[:get-articles {:limit 10}]      ;; is NOT logged in we display all articles
                           [:get-feed-articles {:limit 10}]
                           [:get-tags]]} ;; otherwiser we get her/his feed articles

       ;; -- TAGS --------------------------------------------------------
       :tags {:db         set-page
              :dispatch-n [[:get-tags]
                           [:navigate-to :tags]]}           ;; get tags

       :tag {:db         (dissoc db :filtered-articles)
             :dispatch-n [[:get-articles {:tag tag}]
                          [:navigate-to :tag]]}
       ;; -- EDITOR --------------------------------------------------
       :editor {:db (cond-> set-page
                            slug                            ;; When we click article to edit we need
                            (assoc :edit-article slug))}

       ;; -- ARTICLE -------------------------------------------
       :article {:db         (-> set-page
                                 (dissoc :comments)
                                 (dissoc :profile)
                                 (assoc :active-article slug))
                 :dispatch-n [[:get-article-comments {:slug slug}]
                              [:get-user-profile {:profile (get-in db [:articles slug :author :username])}]
                              [:navigate-to :article]]}

       ;; -- PROFILE -------------------------------------------
       :profile {:db         (assoc set-page :active-article slug)
                 :dispatch-n [[:get-user-profile {:profile profile}] ;; again for dispatching multiple
                              [:get-articles {:author profile}]]} ;; events we can use :dispatch-n
       :user-articles {:db         (dissoc db :filtered-articles)
                       :dispatch-n [[:get-articles {:author profile}]
                                    [:navigate-to :user-articles]]}
       :user-favorite {:db         (dissoc db :filtered-articles) ;; even though we are at :favorited we still
                       :dispatch-n [[:get-articles {:favorited favorited}]
                                    [:navigate-to :user-favorite]]})))) ;; display :profile with :favorited articles

(re-frame/reg-event-fx                                      ;; usage: (dispatch [:reset-active-article])
 :reset-active-article                                      ;; triggered when the user enters new-article i.e. editor without slug
 (fn [{db :db} _]                                           ;; 1st paramter in -db events is db, 2nd paramter not important therefore _
   {:db (dissoc db :active-article)}))                      ;; compute and return the new state

(re-frame/reg-event-fx                                      ;; usage: (dispatch [:set-active-article slug])
 :set-active-article
 (fn [{:keys [db]} [_ slug]]                                ;; 1st parameter in -fx events is no longer just db. It is a map which contains a :db key.
   {:db         (assoc db :active-article slug)             ;; The handler is returning a map which describes two side-effects:
    :dispatch-n [[:get-article-comments {:slug slug}]       ;; changne to app-state :db and future event in this case :dispatch-n
                 [:get-user-profile {:profile (get-in db [:articles slug :author :username])}]]}))

(re-frame/reg-event-fx                                      ;; usage: (dispatch [:reset-active-article])
 :reset-edit-article                                        ;; triggered when the user enters new-article i.e. editor without slug
 (fn [{db :db} _]                                           ;; 1st paramter in -db events is db, 2nd paramter not important therefore _
   {:db (dissoc db :edit-article)}))                        ;; compute and return the new state

;; -- GET Articles @ /api/articles --------------------------------------------
;;
(re-frame/reg-event-fx                                      ;; usage (dispatch [:get-articles {:limit 10 :tag "tag-name" ...}])
 :get-articles                                              ;; triggered every time user request articles with differetn params
 (fn [{:keys [db]} [_ params]]                              ;; params = {:limit 10 :tag "tag-name" ...}
   {:fetch {:method                 :get
            :url                    (api/endpoint "articles") ;; evaluates to "api/articles/"
            :params                 params                  ;; include params in the request
            :headers                (api/auth-header db)    ;; get and pass user token obtained during login
            :response-content-types {#"application/.*json" :json} ;; json response and all keys to keywords
            :on-success             [:get-articles-success params] ;; trigger get-articles-success event
            :on-failure             [:api-request-error :articles]} ;; trigger api-request-error with :get-articles
    :db    (-> db
               (assoc-in [:loading :articles] true)
               (assoc-in [:filter :tag] (:tag params))      ;; so that we can easily show and hide
               (assoc-in [:filter :author] (:author params)) ;; appropriate ui components
               (assoc-in [:filter :favorites] (:favorited params)))}))

(re-frame/reg-event-fx
 :get-articles-success
 (fn [{db :db} [_ params {body :body}]]
   (let [{articles :articles articles-count :articlesCount} body
         articles  (utils/index-by :slug articles)
         filtered? (or (:author params) (:favorited params) (:tag params))]
     {:db (-> db
              (assoc-in [:loading :articles] false)         ;; turn off loading flag for this event
              (update :articles merge articles)             ;; all articles, which we index-by slug
              (update (if filtered? :filtered-articles :global-articles) #(apply conj % (keys articles)))
              (assoc (if filtered? :filtered-articles-count :global-articles-count) articles-count))})))

(re-frame/reg-event-fx                                      ;; usage (dispatch [:get-more-articles {:tag "tag-name" ...}])
 :get-more-articles                                         ;; triggered every time user request more articles with differetn params
 (fn [{{:keys [global-articles global-articles-count loading filtered-articles-count] :as db} :db} [_ tag]]
   (let [articles-curr-count (count global-articles)
         articles-count      (if tag filtered-articles-count global-articles-count)]
     (when (and (< articles-curr-count articles-count) (not (:articles loading)))
       {:db       (assoc-in db [:loading :articles] true)
        :dispatch [:get-articles
                   {:limit  (min 10 (- articles-count articles-curr-count))
                    :offset articles-curr-count
                    :tag    tag}]}))))

;; -- GET Feed Articles @ /api/articles/feed ----------------------------------
;;

(re-frame/reg-event-fx                                      ;; usage (dispatch [:get-feed-articles {:limit 10 :offset 0 ...}])
 :get-feed-articles                                         ;; triggered when Your Feed tab is loaded
 (fn [{:keys [db]} [_ params]]                              ;; params = {:offset 0 :limit 10}
   {:fetch {:method                 :get
            :url                    (api/endpoint "articles" "feed") ;; evaluates to "api/articles/feed"
            :params                 params                  ;; include params in the request
            :headers                (api/auth-header db)    ;; get and pass user token obtained during login
            :response-content-types {#"application/.*json" :json} ;; json response and all keys to keywords
            :on-success             [:get-feed-articles-success] ;; trigger get-articles-success event
            :on-failure             [:api-request-error :feed-articles]} ;; trigger api-request-error with :get-feed-articles
    :db    (assoc-in db [:loading :feed-articles] true)}))

(re-frame/reg-event-fx
 :get-feed-articles-success
 (fn [{db :db} [_ {body :body}]]
   (let [{articles :articles articles-count :articlesCount} body
         articles (utils/index-by :slug articles)]
     {:db (-> db
              (assoc-in [:loading :feed-articles] false)
              (update :articles merge articles)
              (assoc :feed-articles-count articles-count)
              (update :feed-articles #(apply conj % (keys articles))))})))

(re-frame/reg-event-fx                                      ;; usage (dispatch [:get-articles {:limit 10 :tag "tag-name" ...}])
 :get-more-feed-articles                                    ;; triggered every time user request articles with differetn params
 (fn [{{:keys [feed-articles feed-articles-count loading] :as db} :db} _]
   (let [articles-curr-count (count feed-articles)]
     (when (and (< articles-curr-count feed-articles-count) (not (:feed-articles loading)))
       {:db       (assoc-in db [:loading :feed-articles] true)
        :dispatch [:get-feed-articles
                   {:limit  (min 10 (- feed-articles-count articles-curr-count))
                    :offset articles-curr-count}]}))))

;; -- GET Article @ /api/articles/:slug ---------------------------------------
;;
(re-frame/reg-event-fx                                      ;; usage (dispatch [:get-article {:slug "slug"}])
 :get-article                                               ;; triggered when a user upserts article i.e. is redirected to article page after saving an article
 (fn [{:keys [db]} [_ params]]                              ;; params = {:slug "slug"}
   {:fetch {:method                 :get
            :url                    (api/endpoint "articles" (:slug params)) ;; evaluates to "api/articles/:slug"
            :headers                (api/auth-header db)    ;; get and pass user token obtained during login
            :response-content-types {#"application/.*json" :json} ;; json response and all keys to keywords
            :on-success             [:get-article-success]  ;; trigger get-article-success event
            :on-failure             [:api-request-error :get-article]} ;; trigger api-request-error with :get-articles
    :db    (assoc-in db [:loading :article] true)}))

(re-frame/reg-event-fx
 :get-article-success
 (fn [{db :db} [_ {body :body}]]
   (let [{article :article} body]
     {:db (-> db
              (assoc-in [:loading :article] false)
              (assoc-in [:articles (:slug article)] article))})))

;; -- POST/PUT Article @ /api/articles(/:slug) --------------------------------
;;
(re-frame/reg-event-fx                                      ;; usage (dispatch [:upsert-article article])
 :upsert-article                                            ;; when we update or insert (upsert) we are sending the same shape of information
 (fn [{:keys [db]} [_ params cb]]                           ;; params = {:slug "article-slug" :article {:body "article body"} }
   {:db    (assoc-in db [:loading :article] true)
    :fetch {:method                 (if (:slug params) :put :post) ;; when we get a slug we'll update (:put) otherwise insert (:post)
            :url                    (if (:slug params)      ;; Same logic as above but we go with different
                                      (api/endpoint "articles" (:slug params)) ;; endpoint - one with :slug to update
                                      (api/endpoint "articles")) ;; and another to insert
            :headers                (api/auth-header db)    ;; get and pass user token obtained during login
            :body                   {:article (:article params)}
            :request-content-type   :json                   ;; make sure we are doing request format wiht json
            :response-content-types {#"application/.*json" :json} ;; json response and all keys to keywords
            :on-success             [:upsert-article-success cb] ;; trigger upsert-article-success event
            :on-failure             [:api-request-error :upsert-article]}})) ;; trigger api-request-error with :upsert-article

(re-frame/reg-event-fx
 :upsert-article-success
 (fn [{:keys [db]} [_ cb {body :body}]]
   (let [{article :article} body]
     (cb)
     {:db       (-> db
                    (assoc-in [:loading :article] false)
                    (dissoc :errors)                        ;; clean up any erros that we might have in db
                    (assoc-in [:articles (:slug article)] article))
      :dispatch [:set-active-page {:page :article
                                   :slug (:slug article)}]}))) ;; of the article and comments from the server

;; -- DELETE Article @ /api/articles/:slug ------------------------------------
;;
(re-frame/reg-event-fx                                      ;; usage (dispatch [:delete-article slug])
 :delete-article                                            ;; triggered when a user deletes an article
 (fn [{:keys [db]} [_ slug]]                                ;; slug = {:slug "article-slug"}
   {:db    (assoc-in db [:loading :article] true)
    :fetch {:method                 :delete
            :url                    (api/endpoint "articles" slug) ;; evaluates to "api/articles/:slug"
            :headers                (api/auth-header db)    ;; get and pass user token obtained during login
            :body                   slug                    ;; pass the article slug to delete
            :request-content-type   :json                   ;; make sure we are doing request format wiht json
            :response-content-types {#"application/.*json" :json} ;; json response and all keys to keywords
            :on-success             [:delete-article-success] ;; trigger get-articles-success
            :on-failure             [:api-request-error :delete-article]}})) ;; trigger api-request-error with :delete-article

(re-frame/reg-event-fx
 :delete-article-success
 (fn [{:keys [db]} _]
   {:db       (-> db
                  (update-in [:articles] dissoc (:active-article db))
                  (assoc-in [:loading :article] false))
    :dispatch [:set-active-page {:page :home}]}))

;; -- GET Tags @ /api/tags ----------------------------------------------------
;;
(re-frame/reg-event-fx                                      ;; usage (dispatch [:get-tags])
 :get-tags                                                  ;; triggered when the home page is loaded
 (fn [{:keys [db]} _]                                       ;; second parameter is not important, therefore _
   {:db    (assoc-in db [:loading :tags] true)
    :fetch {:method                 :get
            :url                    (api/endpoint "tags")   ;; evaluates to "api/tags"
            :response-content-types {#"application/.*json" :json} ;; json response and all keys to keywords
            :on-success             [:get-tags-success]     ;; trigger get-tags-success event
            :on-failure             [:api-request-error :get-tags]}})) ;; trigger api-request-error with :get-tags

(re-frame/reg-event-fx
 :get-tags-success
 (fn [{db :db} [_ {body :body}]]
   (let [{tags :tags} body]
     {:db (-> db
              (assoc-in [:loading :tags] false)
              (assoc :tags tags))})))

;; -- GET Comments @ /api/articles/:slug/comments -----------------------------
;;
(re-frame/reg-event-fx                                      ;; usage (dispatch [:get-article-comments {:slug "article-slug"}])
 :get-article-comments                                      ;; triggered when the article page is loaded
 (fn [{:keys [db]} [_ params]]                              ;; params = {:slug "article-slug"}
   {:db    (assoc-in db [:loading :comments] true)
    :fetch {:method                 :get
            :url                    (api/endpoint "articles" (:slug params) "comments") ;; evaluates to "api/articles/:slug/comments"
            :headers                (api/auth-header db)    ;; get and pass user token obtained during login
            :response-content-types {#"application/.*json" :json} ;; json response and all keys to keywords
            :on-success             [:get-article-comments-success] ;; trigger get-article-comments-success
            :on-failure             [:api-request-error :get-article-comments]}})) ;; trigger api-request-error with :get-article-comments

(re-frame/reg-event-fx
 :get-article-comments-success
 (fn [{db :db} [_ {body :body}]]
   (let [{comments :comments} body]
     {:db (-> db
              (assoc-in [:loading :comments] false)
              (assoc :comments (utils/index-by :id comments)))}))) ;; another index-by, this time by id

;; -- POST Comments @ /api/articles/:slug/comments ----------------------------
;;
(re-frame/reg-event-fx                                      ;; usage (dispatch [:post-comment comment])
 :post-comment                                              ;; triggered when a user submits a comment
 (fn [{:keys [db]} [_ body]]                                ;; body = {:body "body" }
   {:db    (assoc-in db [:loading :comments] true)
    :fetch {:method                 :post
            :url                    (api/endpoint "articles" (:active-article db) "comments") ;; evaluates to "api/articles/:slug/comments"
            :headers                (api/auth-header db)    ;; get and pass user token obtained during login
            :body                   {:comment body}
            :request-content-type   :json                   ;; make sure we are doing request format wiht json
            :response-content-types {#"application/.*json" :json} ;; json response and all keys to keywords
            :on-success             [:post-comment-success] ;; trigger get-articles-success
            :on-failure             [:api-request-error :comments]}})) ;; trigger api-request-error with :comments

(re-frame/reg-event-fx
 :post-comment-success
 (fn [{:keys [db]} [_ {comment :body}]]
   {:db       (-> db
                  (assoc-in [:loading :comments] false)
                  (assoc-in [:articles (:active-article db) :comments] comment)
                  (update-in [:errors] dissoc :comments))   ;; clean up errors, if any
    :dispatch [:get-article-comments {:slug (:active-article db)}]}))

;; -- DELETE Comments @ /api/articles/:slug/comments/:comment-id ----------------------
;;
(re-frame/reg-event-fx                                      ;; usage (dispatch [:delete-comment comment-id])
 :delete-comment                                            ;; triggered when a user deletes an article
 (fn [{:keys [db]} [_ comment-id]]                          ;; comment-id = 1234
   {:db    (do
             (assoc-in db [:loading :comments] true)
             (assoc db :active-comment comment-id))
    :fetch {:method                 :delete
            :url                    (api/endpoint "articles" (:active-article db) "comments" comment-id) ;; evaluates to "api/articles/:slug/comments/:comment-id"
            :headers                (api/auth-header db)    ;; get and pass user token obtained during login
            :request-content-type   :json                   ;; make sure we are doing request format wiht json
            :response-content-types {#"application/.*json" :json} ;; json response and all keys to keywords
            :on-success             [:delete-comment-success] ;; trigger delete-comment-success
            :on-failure             [:api-request-error :delete-comment]}})) ;; trigger api-request-error with :delete-comment

(re-frame/reg-event-fx
 :delete-comment-success
 (fn [{db :db} _]
   {:db (-> db
            (update-in [:comments] dissoc (:active-comment db)) ;; we could do another fetch of comments
            (dissoc :active-comment)                        ;; but instead we just remove it from app-db
            (assoc-in [:loading :comment] false))}))        ;; which gives us much snappier ui

;; -- GET Profile @ /api/profiles/:username -----------------------------------
;;
(re-frame/reg-event-fx                                      ;; usage (dispatch [:get-user-profile {:profile "profile"}])
 :get-user-profile                                          ;; triggered when the profile page is loaded
 (fn [{:keys [db]} [_ params]]                              ;; params = {:profile "profile"}
   {:db    (assoc-in db [:loading :profile] true)
    :fetch {:method                 :get
            :url                    (api/endpoint "profiles" (:profile params)) ;; evaluates to "api/profiles/:profile"
            :headers                (api/auth-header db)    ;; get and pass user token obtained during login
            :response-content-types {#"application/.*json" :json} ;; json response and all keys to keywords
            :on-success             [:get-user-profile-success] ;; trigger get-user-profile-success
            :on-failure             [:api-request-error :get-user-profile]}})) ;; trigger api-request-error with :get-user-profile

(re-frame/reg-event-fx
 :get-user-profile-success
 (fn [{db :db} [_ {body :body}]]
   (let [{profile :profile} body]
     {:db (-> db
              (assoc-in [:loading :profile] false)
              (assoc :profile profile))})))

;; -- POST Login @ /api/users/login -------------------------------------------
;;
(re-frame/reg-event-fx                                      ;; usage (dispatch [:login user])
 :login                                                     ;; triggered when a users submits login form
 (fn [{:keys [db]} [_ credentials]]                         ;; credentials = {:email ... :password ...}
   {:db    (assoc-in db [:loading :login] true)
    :fetch {:method                 :post
            :url                    (api/endpoint "users" "login") ;; evaluates to "api/users/login"
            :body                   {:user credentials}     ;; {:user {:email ... :password ...}}
            :request-content-type   :json                   ;; make sure it's json
            :response-content-types {#"application/.*json" :json} ;; json response and all keys to keywords
            :on-success             [:login-success]        ;; trigger login-success
            :on-failure             [:api-request-error :login]}})) ;; trigger api-request-error with :login

(re-frame/reg-event-fx
 :login-success
 ;; The event handler function.
 ;; The "path" interceptor in `set-user-interceptor` means 1st parameter is the
 ;; value at `:user` path within `db`, rather than the full `db`.
 ;; And, further, it means the event handler returns just the value to be
 ;; put into `:user` path, and not the entire `db`.
 ;; So, a path interceptor makes the event handler act more like clojure's `update-in`
 (fn [{db :db} [_ {body :body}]]
   (let [{props :user} body
         user (merge (:user db) props)]
     {:db               (assoc db :user user)
      :store-user-in-ls user
      :dispatch-n       [[:set-active-page {:page :home}]
                         [:navigate-back]]})))

;; -- POST Registration @ /api/users ------------------------------------------
;;
(re-frame/reg-event-fx                                      ;; usage (dispatch [:register-user registration])
 :register-user                                             ;; triggered when a users submits registration form
 (fn [{:keys [db]} [_ registration]]                        ;; registration = {:username ... :email ... :password ...}
   {:db    (assoc-in db [:loading :register-user] true)
    :fetch {:method                 :post
            :url                    (api/endpoint "users")  ;; evaluates to "api/users"
            :body                   {:user registration}    ;; {:user {:username ... :email ... :password ...}}
            :request-content-type   :json                   ;; make sure it's json
            :response-content-types {#"application/.*json" :json} ;; json response and all keys to keywords
            :on-success             [:register-user-success] ;; trigger login-success
            :on-failure             [:api-request-error :register-user]}})) ;; trigger api-request-error with :login-success

(re-frame/reg-event-fx
 :register-user-success
 ;; The event handler function.
 ;; The "path" interceptor in `set-user-interceptor` means 1st parameter is the
 ;; value at `:user` path within `db`, rather than the full `db`.
 ;; And, further, it means the event handler returns just the value to be
 ;; put into `:user` path, and not the entire `db`.
 ;; So, a path interceptor makes the event handler act more like clojure's `update-in`
 (fn [{db :db} [_ {body :body}]]
   (let [{props :user} body
         user (merge (:user db) props)]
     {:db               (assoc db :user user)
      :store-user-in-ls user
      :dispatch-n       [[:set-active-page {:page :home}]
                         [:navigate-back]]})))

;; -- PUT Update User @ /api/user ---------------------------------------------
;;
(re-frame/reg-event-fx                                      ;; usage (dispatch [:update-user user])
 :update-user                                               ;; triggered when a users updates settgins
 (fn [{:keys [db]} [_ user]]                                ;; user = {:img ... :username ... :bio ... :email ... :password ...}
   {:db    (assoc-in db [:loading :update-user] true)
    :fetch {:method                 :put
            :url                    (api/endpoint "user")   ;; evaluates to "api/user"
            :body                   {:user user}            ;; {:user {:img ... :username ... :bio ... :email ... :password ...}}
            :headers                (api/auth-header db)    ;; get and pass user token obtained during login
            :request-content-type   :json                   ;; make sure our request is json
            :response-content-types {#"application/.*json" :json} ;; json response and all keys to keywords
            :on-success             [:update-user-success]  ;; trigger update-user-success
            :on-failure             [:api-request-error :update-user]}})) ;; trigger api-request-error with :update-user

(re-frame/reg-event-fx
 :update-user-success
 ;; The event handler function.
 ;; The "path" interceptor in `set-user-interceptor` means 1st parameter is the
 ;; value at `:user` path within `db`, rather than the full `db`.
 ;; And, further, it means the event handler returns just the value to be
 ;; put into `:user` path, and not the entire `db`.
 ;; So, a path interceptor makes the event handler act more like clojure's `update-in`
 (fn [{db :db} [_ {body :body}]]
   (let [{props :user} body
         user (merge (:user db) props)]
     {:db               (-> db
                            (assoc :user user)
                            (assoc-in [:loading :update-user] false))
      :store-user-in-ls user})))

;; -- Toggle follow user @ /api/profiles/:username/follow ---------------------
;;
(re-frame/reg-event-fx                                      ;; usage (dispatch [:toggle-follow-user username])
 :toggle-follow-user                                        ;; triggered when user clicks follow/unfollow button on profile page
 (fn [{:keys [db]} [_ username]]                            ;; username = :username
   {:db    (assoc-in db [:loading :toggle-follow-user] true)
    :fetch {:method                 (if (get-in db [:profile :following]) :delete :post) ;; check if we follow if yes DELETE, no POST
            :url                    (api/endpoint "profiles" username "follow") ;; evaluates to "api/profiles/:username/follow"
            :headers                (api/auth-header db)    ;; get and pass user token obtained during login
            :request-content-type   :json                   ;; make sure it's json
            :response-content-types {#"application/.*json" :json} ;; json response and all keys to keywords
            :on-success             [:toggle-follow-user-success] ;; trigger toggle-follow-user-success
            :on-failure             [:api-request-error :login]}})) ;; trigger api-request-error with :update-user-success

(re-frame/reg-event-fx                                      ;; usage: (dispatch [:toggle-follow-user-success])
 :toggle-follow-user-success
 (fn [{db :db} [_ {body :body}]]
   (let [{profile :profile} body]
     {:db       (-> db
                    (assoc-in [:loading :toggle-follow-user] false)
                    (assoc-in [:profile :following] (:following profile)))
      :dispatch [:get-feed-articles {:limit 10}]})))

;; -- Toggle favorite article @ /api/articles/:slug/favorite ------------------
;;
(defn update-fav [db key slug favorited article]
  (-> db
      (assoc-in [key slug :favorited] favorited)
      (assoc-in [key slug :favoritesCount] (if favorited
                                             (:favoritesCount article inc)
                                             (:favoritesCount article dec)))))

(defn preupdate-fav [db key slug]
  (update-in db [key slug]
             #(-> %
                  (update :favoritesCount (if (:favorited %) dec inc))
                  (update :favorited not))))

(re-frame/reg-event-fx                                      ;; usage (dispatch [:toggle-favorite-article slug])
 :toggle-favorite-article                                   ;; triggered when user clicks favorite/unfavorite button on profile page
 (fn [{:keys [db]} [_ slug]]                                ;; slug = :slug
   {:db    (-> db
               (assoc-in [:loading :toggle-favorite-article] true)
               (preupdate-fav :articles slug))
    :fetch {:method                 (if (get-in db [:articles slug :favorited])
                                      :delete :post)        ;; check if article is already favorite: yes DELETE, no POST
            :url                    (api/endpoint "articles" slug "favorite") ;; evaluates to "api/articles/:slug/favorite"
            :headers                (api/auth-header db)    ;; get and pass user token obtained during login
            :request-content-type   :json                   ;; make sure it's json
            :response-content-types {#"application/.*json" :json} ;; json response and all keys to keywords
            :on-success             [:toggle-favorite-article-success] ;; trigger toggle-favorite-article-success
            :on-failure             [:api-request-error :login]}})) ;; trigger api-request-error with :toggle-favorite-article

(re-frame/reg-event-fx                                      ;; usage: (dispatch [:toggle-favorite-article-success])
 :toggle-favorite-article-success
 (fn [{db :db} [_ {body :body}]]
   (let [{article :article} body
         slug      (:slug article)
         favorited (:favorited article)]
     {:db (-> db
              (assoc-in [:loading :toggle-favorite-article] false)
              (update-fav :articles slug favorited article))})))

;; -- Logout ------------------------------------------------------------------
;;
(re-frame/reg-event-fx                                      ;; usage (dispatch [:logout])
 :logout
 ;; The event handler function removes the user from
 ;; app-state = :db and sets the url to "/".
 (fn [{:keys [db]} _]
   {:db                  db/default-db
    :remove-user-from-ls nil
    :dispatch            [:navigate-to :sign-in]}))

;; -- Request Handlers -----------------------------------------------------------
;;
(re-frame/reg-event-fx
 :api-request-error                                         ;; triggered when we get request-error from the server
 (fn [{db :db} [_ request-type response]]                   ;; destructure to obtain request-type and response
   {:db (-> db                                              ;; when we complete a request we need to clean so that our ui is nice and tidy
            (assoc-in [:errors request-type] (get-in response [:body :errors]))
            (assoc-in [:loading request-type] false))}))
