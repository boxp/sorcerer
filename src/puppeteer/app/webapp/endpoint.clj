(ns puppeteer.app.webapp.endpoint
  (:require [com.stuartsierra.component :as component]
            [compojure.core :refer [defroutes context GET POST routes]]
            [compojure.route :as route]
            [ring.adapter.jetty :as server]
            [ring.middleware.json :refer [wrap-json-params
                                          wrap-json-response
                                          wrap-json-body]]
            [puppeteer.app.webapp.handler :as handler]))

(defn main-routes
  [{:keys [webapp-handler] :as comp}]
  (routes
    (GET "/" [req] (handler/index webapp-handler))
    (POST "/slack/incoming_webhook" [req] (handler/index webapp-handler))
    (route/not-found "<h1>404 page not found</h1>")))

(defn app
  [comp]
  (-> (main-routes comp)
      (wrap-json-body {:keywords? true :bigdecimals? true})))

(defrecord WebappEndpointComponent [port server webapp-handler]
  component/Lifecycle
  (start [this]
    (println ";; Starting WebappEndpointComponent")
    (-> this
        (assoc :server (server/run-jetty (app this) {:port port :join? false}))))
  (stop [this]
    (println ";; Stopping WebappEndpointComponent")
    (.stop (:server this))
    (-> this
        (dissoc :server))))

(defn webapp-endpoint-component
  [port]
  (map->WebappEndpointComponent {:port port}))
