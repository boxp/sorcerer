(ns puppeteer.app.my-webapp.handler
  (:require [com.stuartsierra.component :as component]))

(defn index
  [{:keys [] :as comp}]
  "OK")

(defrecord MyWebappHandlerComponent []
  component/Lifecycle
  (start [this]
    (println ";; Starting MyWebappHandlerComponent")
    this)
  (stop [this]
    (println ";; Stopping MyWebappHandlerComponent")
    this))

(defn my-webapp-handler-component
  []
  (map->MyWebappHandlerComponent {}))
