(ns utopia.core
  (:require [datascript.core :as d]
            [reagent.core :as r]
            [posh.reagent :as p]))

;; DB

(def initial-facts [
                    ; code bubble
                    {:db/ident       :code-bubble/value
                     :db/valueType   :db.type/string
                     :db/cardinality :db.cardinality/one}

                    {:code-bubble/value "(defn add [a b] (+ a b))"}
                    {:code-bubble/value "(defn hello-view [name] [:div (str \"Hello\" name)])"}
                    ])

(defn get-schema [facts]
  (->> facts
       (filter #(not (nil? (:db/ident %))))
       (reduce
         (fn [schema fact]
           (let [attribute-id (:db/ident fact)
                 attribute-fact (cond-> (dissoc fact :db/ident :db/id)
                                        (not= (:db/valueType fact) :db.type/ref) (dissoc :db/valueType))]


             (assoc schema attribute-id attribute-fact)))
         {})))

(defonce conn (d/create-conn (get-schema initial-facts)))

(defonce initial-data (d/transact! conn initial-facts))


;; ACTIONS

(defn retract-entities [conn entity-ids]
  (let [retractions (map (fn [entity-id]
                           [:db/retractEntity entity-id])
                         entity-ids)]
    (p/transact! conn retractions)))


;; VIEWS

(defn code-bubble-view [conn code-bubble-id]
  (let [code-bubble @(p/pull conn '[:code-bubble/value] code-bubble-id)
        value (:code-bubble/value code-bubble)]
    [:textarea.CodeBubble value]))

(defn code-bubbles-view [conn]
  (let [code-bubbles @(p/q '[:find ?code-bubble
                         :in $
                         :where [?code-bubble :code-bubble/value _]] conn)]
    [:<> (map
           (fn [[search-id]]
             ^{:key search-id} [code-bubble-view conn search-id])
           code-bubbles)]))

(defn app [conn]
  [:div.Editor
   [code-bubbles-view conn]])

;; APPLICATION SETUP

(p/posh! conn)

(defn start [conn]
  (r/render-component [app conn] (.getElementById js/document "root")))

(start conn)
