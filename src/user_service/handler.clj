(ns user-service.handler
  (:require [clojure.walk :refer [keywordize-keys]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :refer [response status]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.cors :refer [wrap-cors]]))

(defn home []
  (response {:message "Hello, there!"}))

(def users [{:id 0 :username "theneva"}
            {:id 1 :username "reservedeler"}])

(defn all-users []
  (response users))

(defn create-user [user]
  (let [user (keywordize-keys user)]
    (def users (conj users (assoc user :id (count users)))) ; Add the user with incremental ID
    (status
      (response (str "Saved user with username: " (:username user)))
      201)))

(defroutes app-routes
           (POST "/users" request (create-user (:body request)))
           (GET "/" [] (home))
           (GET "/users" [] (all-users))
           (route/not-found " Not Found"))

(def app
  (-> app-routes
      (wrap-cors :access-control-allow-origin "*" :access-control-allow-methods "*")
      (wrap-json-response)
      (wrap-keyword-params)
      (wrap-json-body)
      (wrap-defaults (assoc site-defaults :security false))))
