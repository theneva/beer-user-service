(ns user-service.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [crypto.password.bcrypt :as bcrypt]
            [ring.util.response :refer [response status]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.cors :refer [wrap-cors]]))

(def users (atom [{:id       0
                   :username "theneva"
                   :password (bcrypt/encrypt "hello")}
                  {:id       1
                   :username "reservedeler"
                   :password (bcrypt/encrypt "blah")}]))

(defn home [] {:message "Hello, there!"})

(defn all-users [] @users)

(defn user-by-username [username]
  (first
    (filter #(= (:username %) username)
            @users)))

(defn save-user [user]
  (if (not-any? #(= (:username %) (:username user)) @users)
    (do
      (swap! users conj (assoc user :id (count user)))      ; Add the user with incremental ID
      (status
        (response (str "Saved user with username: " (:username user)))
        201))
    (status
      (response (str "User with username '" (:username user) "' already exists"))
      400)))

(defn authenticate-user [{:keys [username password]}]
  (let [user (user-by-username username)]
    (if (bcrypt/check password (:password user))
      (response (dissoc user :password))
      (status (response "Wrong username or password") 401))))

(defroutes app-routes
           (GET "/" [] (response (home)))
           (GET "/users" [] (response (all-users)))
           (GET "/users/:username" [username] (response (user-by-username username)))
           (POST "/users" request (save-user (:body request)))
           (POST "/authenticate" request (authenticate-user (:body request)))
           (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-cors :access-control-allow-origin "*" :access-control-allow-methods "*")
      (wrap-json-response)
      (wrap-json-body {:keywords? true :bigdecimals? true})
      (wrap-defaults (assoc site-defaults :security false))))
