(ns user-service.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [user-service.handler :refer :all]))

(deftest test-app
  (testing "main route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (.contains (get-in (:headers response) ["Content-Type"]) "application/json"))
      (is (= (:body response) "{\"message\":\"Hello, there!\"}"))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
