(ns clojure-file-upload.core
  (:require [compojure.core :refer :all]
            [org.httpkit.server :refer [run-server]]
            [clojure.tools.logging :as log]))               ; httpkit is a server

(defroutes app-file-uploader
           (GET "/" [] "Hello root")
           (GET "/upload" request
             (log/info (str "request is:" request))
             (str "Response is:" request)))

(defn -main []
  (run-server app-file-uploader {:port 5000}))
