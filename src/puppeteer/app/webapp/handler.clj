(ns puppeteer.app.webapp.handler
  (:require [com.stuartsierra.component :as component]))

(defn index
  [{:keys [] :as comp}]
  "OK")

(defrecord WebappHandlerComponent []
  component/Lifecycle
  (start [this]
    (println ";; Starting WebappHandlerComponent")
    this)
  (stop [this]
    (println ";; Stopping WebappHandlerComponent")
    this))

(defn webapp-handler-component
  []
  (map->WebappHandlerComponent {}))
