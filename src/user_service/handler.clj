(ns user-service.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [crypto.password.bcrypt :as bcrypt]
            [ring.util.response :refer [response status content-type]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.cors :refer [wrap-cors]]
            [clojure.java.jdbc :as sql]))

(def db-uri (or (System/getenv "DB_URI")
                "postgresql://localhost:5432/users"))

(defn text-response [message status-code]
  (status (-> (response message)
              (content-type "text/plain"))
          status-code))

(defn home [] (text-response "Hello, world!" 200))

(defn all-users-without-password []
  (map #(dissoc % :password_hash)
       (sql/query db-uri "select * from users")))

(defn user-by-username [username]
  (first (sql/query db-uri
                    ["select * from users where username = ?" username])))

(defn user-by-username-without-password [username]
  (dissoc (user-by-username username) :password_hash))

(defn save-user [user]
  (let [username (:username user)]
    (if (not (user-by-username username))
      (let [password-hash (bcrypt/encrypt (:password user))]
        (sql/insert! db-uri
                     :users (assoc (dissoc user :password) :password_hash password-hash))
        (status (response (user-by-username-without-password username)) 201))
      (text-response (str "User with username '" (:username user) "' already exists.") 400))))

(defn authenticate-user [{:keys [username password]}]
  (let [user (user-by-username username)]
    (if (nil? user)
      (text-response (str "No user exists with username " username) 401)
      (if (bcrypt/check password (:password_hash user))
        (response (dissoc user :password_hash))
        (status (response "Wrong username or password") 401)))))

(defroutes app-routes
           (GET "/" [] (home))
           (GET "/users" [] (response (all-users-without-password)))
           (GET "/users/:username" [username]
             (let [user (user-by-username-without-password username)]
               (if (nil? user)
                 (text-response (str "No user exists with username '" username "'") 404)
                 (response user))))
           (POST "/users" request (save-user (:body request)))
           (POST "/authenticate" request (authenticate-user (:body request)))
           (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-cors :access-control-allow-origin "*" :access-control-allow-methods "*")
      (wrap-json-response)
      (wrap-json-body {:keywords? true :bigdecimals? true})
      (wrap-defaults (assoc site-defaults :security false))))
