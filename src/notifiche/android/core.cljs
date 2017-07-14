(ns notifiche.android.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [cljs.core.async :refer [chan close! put! <!]]
            [notifiche.events]
            [notifiche.subs]))

(def ReactNative (js/require "react-native"))

(def app-registry (.-AppRegistry ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def PushNot (js/require "react-native-push-notification"))

(def logo-img (js/require "./images/cljs.png"))

(defn prova [] (-> (js/fetch "http://10.0.3.2:8000/app")
                   (.then (fn [res] (println "This is the body: " (.-_bodyText res))))))
(defn alert [title]
  (.alert (.-Alert ReactNative) title))
(defmacro <?
  "David Nolen's macro that waits for a channel-returning asynchronous call
   to end and throws channel's content if it is an Error. Must be used inside a go block. Requires <!"
  [expr]
  `(let [res# (~'<! ~expr)]
     (if (instance? js/Error res#)
       (throw res#)
       res#)))
(defn fetch
  "Returns a channel where the reponse to the HTTP request req is put.
  If an error occurs, then the error object will be put in the channel instead."
  [address & [req]]
  (let [c (chan)]
    (-> (apply js/fetch (conj [address] req))
        (.then (fn [response] (put! c response)))
        (.catch (fn [error] (put! c error))))
    c))

(.localNotificationSchedule PushNot #js {:message "My notification" :date (new js/Date (+ (.now js/Date) (* 360 1000)))})

(defn app-root []
  (let [prov (r/atom "")]
    (go
      (reset! prov (.-_bodyText (<! (fetch "http://10.0.3.2:8000/app")))))
    (fn []
      [view {:style {:flex-direction "column" :margin 40 :align-items "center"}}
       [text {:style {:font-size 30 :font-weight "100" :margin-bottom 20 :text-align "center"}} "ciao"]
       [image {:source logo-img
               :style  {:width 80 :height 80 :margin-bottom 30}}]
       [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}
                             :on-press #(alert "HELLO!")}
        [text {:style {:color "white" :text-align "center" :font-weight "bold"}} (str @prov)]]])))

(defn init []
  (dispatch-sync [:initialize-db])
  (.registerComponent app-registry "Notifiche" #(r/reactify-component app-root)))
