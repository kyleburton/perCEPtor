(ns perceptor.test.core
  (:require [perceptor.provider :as perceptor])
  (:use
        [clojure.test]))

(defn create-stocks-provider []
  (perceptor/register-provider
   :stocks
   (fn [mgr]
     (perceptor/declare-type "StockEvent"
                             "stock" "string"
                             "price" "float")
     (perceptor/compile-statement (format "CREATE WINDOW StockEventsLastMinute.win:time(%s seconds) AS StockEvent" 60))
     (perceptor/compile-statement (format "CREATE WINDOW StockEventsLast50.win:length(50) AS StockEvent"))
     (perceptor/compile-statement "insert into StockEventsLastMinute select * from StockEvent")
     (perceptor/compile-statement "insert into StockEventsLast50 select * from StockEvent")

     (perceptor/compile-statement
      "CREATE SCHEMA StockAlertsSchema (stock string, error_count int, success_count int, created_at long, updated_at long, last_alerted long, is_new bool)")

     (perceptor/compile-statement
      "CREATE WINDOW StockAlertsWindow.std:unique(stock) AS StockAlertsSchema"))))

(deftest test-register-provider
  (create-stocks-provider)
  (let [esp (perceptor/make-provider :stocks)]
    (is (not (nil? esp))))
  (perceptor/unregister-provider :stocks))