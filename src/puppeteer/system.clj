(ns puppeteer.system
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [puppeteer.infra.datasource.example :refer [example-datasource-component]]
            [puppeteer.infra.repository.example :refer [example-repository-component]]
            [puppeteer.domain.usecase.example :refer [example-usecase-component]]
            [puppeteer.app.my-webapp.handler :refer [my-webapp-handler-component]]
            [puppeteer.app.my-webapp.endpoint :refer [my-webapp-endpoint-component]])
  (:gen-class))

(defn puppeteer-system
  [{:keys [puppeteer-example-port
           puppeteer-my-webapp-port] :as conf}]
  (component/system-map
    :example-datasource (example-datasource-component puppeteer-example-port)
    :example-repository (component/using
                          (example-repository-component)
                          [:example-datasource])
    :example-usecase (component/using
                       (example-usecase-component)
                       [:example-repository])
    :my-webapp-handler (component/using
                         (my-webapp-handler-component)
                         [:example-usecase])
    :my-webapp-endpoint (component/using
                          (my-webapp-endpoint-component puppeteer-my-webapp-port)
                          [:my-webapp-handler])))

(defn load-config []
  {:puppeteer-example-port (-> (or (env :puppeteer-example-port) "8000") Integer/parseInt)
   :puppeteer-my-webapp-port (-> (or (env :puppeteer-my-webapp-port) "8080") Integer/parseInt)})

(defn -main []
  (component/start
    (puppeteer-system (load-config))))
