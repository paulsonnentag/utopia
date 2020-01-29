(ns utopia.core
  (:require [datascript.core :as d]
            [reagent.core :as r]
            [posh.reagent :as p]
            [utopia.examples.source :as source]
            [cljs.tools.reader :as reader]
            [zprint.core :as zp]))

;; DB


(def schema
  {:code-bubble/value         {:db/cardinality :db.cardinality/one}
   :code-bubble/unsaved-value {:db/cardinality :db.cardinality/one}})


(defn from-source [source]
  (->> (reader/read-string (str "(" source ")"))
       (map (fn [form] {:code-bubble/value (zp/zprint-str form)}))))

(def initial-facts (from-source source/todo))

(defonce conn (d/create-conn schema))

;; ACTIONS

(defn update-code-bubble-value [conn code-bubble-id value]
  (p/transact! conn [[:db/add code-bubble-id :code-bubble/unsaved-value value]]))

(defn save-code-bubble [conn code-bubble-id]
  (let [{value :code-bubble/unsaved-value} @(p/pull conn [:code-bubble/unsaved-value] code-bubble-id)]
    (if (not (nil? value))
      (p/transact! conn [[:db.fn/retractAttribute code-bubble-id :code-bubble/unsaved-value]
                         [:db/add code-bubble-id :code-bubble/value value]]))))

(defn retract-entities [conn entity-ids]
  (let [retractions (map (fn [entity-id]
                           [:db/retractEntity entity-id])
                         entity-ids)]
    (p/transact! conn retractions)))

;; VIEWS

(defn code-editor [attr]
  [(r/adapt-react-class (aget js/window "codemirror" "CodeEditor")) attr])

(defn code-bubble-view [conn code-bubble-id]
  (let [{value         :code-bubble/value
         unsaved-value :code-bubble/unsaved-value}
        @(p/pull conn '[:code-bubble/value :code-bubble/unsaved-value] code-bubble-id)]
    [:div.flex
     [:div.flex-1.bg-grey-light.rounded.p-1.m-1
      {:class [(if (and (not (nil? unsaved-value)) (not= unsaved-value value)) :b-)]}
      (code-editor
        {:value     value
         :on-change #(update-code-bubble-value conn code-bubble-id %)
         :on-save   #(save-code-bubble conn code-bubble-id)})]
     [:div.flex-1.p-1 "no preview"]]))

(defn code-bubbles-view [conn]
  (let [code-bubbles (-> @(p/q '[:find ?code-bubble
                                 :in $
                                 :where [?code-bubble :code-bubble/value _]] conn)
                         (sort))]

    [:<> (map
           (fn [[search-id]]
             ^{:key search-id} [code-bubble-view conn search-id])
           code-bubbles)]))

(defn app [conn]
  [:div.p-2
   [code-bubbles-view conn]])


;; SETUP

(defn render []
  (r/render [app conn] (.getElementById js/document "app")))

(defn init! []
  (p/posh! conn)
  (p/transact! conn initial-facts)
  (render))

(defn reload! []
  (render))



