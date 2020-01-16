(ns utopia.core
  (:require [datascript.core :as d]
            [reagent.core :as r]
            [posh.reagent :as p]
            ["/libs/bundle"]))

;; DB

(def initial-facts [
                    ; code bubble
                    {:db/ident       :code-bubble/value
                     :db/valueType   :db.type/string
                     :db/cardinality :db.cardinality/one}

                    {:db/ident       :code-bubble/unsaved-value
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
  [(r/adapt-react-class (aget js/window "codeMirror" "CodeEditor")) attr])

(defn code-bubble-view [conn code-bubble-id]
  (let [{value         :code-bubble/value
         unsaved-value :code-bubble/unsaved-value}
        @(p/pull conn '[:code-bubble/value :code-bubble/unsaved-value] code-bubble-id)]
    [:div.CodeBubble
     [:div.CodeBubble__Content
      {:class [(if (and (not (nil? unsaved-value)) (not= unsaved-value value)) :unsaved-changes)]}
      (code-editor
        {:value     value
         :on-change #(update-code-bubble-value conn code-bubble-id %)
         :on-save #(save-code-bubble conn code-bubble-id)})]
     [:pre.CodeBubble__Preview value unsaved-value]]))

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


;; SETUP

(defn render []
  (r/render-component [app conn] (.getElementById js/document "root")))

(defn init []
  (p/posh! conn)
  (d/transact! conn initial-facts)
  (render))

(render)
