(require [datascript.core :as d]
         [reagent.core :as r]
         [posh.reagent :as p])

;; UTILS

(defn timestamp []
  (.getTime (js/Date.)))


;; DATABASE

(def schema
  {:todo/description     {:db/cardinality :db.cardinality/one}
   :todo/is-completed    {:db/cardinality :db.cardinality/one}
   :todo/is-being-edited {:db/cardinality :db.cardinality/one}
   :todo/creation-date   {:db/cardinality :db.cardinality/one}})

(def initial-facts [{:todo/description   "go shopping"
                     :todo/is-completed  true
                     :todo/creation-date (timestamp)}
                    {:todo/description   "clean kitchen"
                     :todo/creation-date (timestamp)}
                    {:todo/description   "walk the dog"
                     :todo/creation-date (timestamp)}])

(defonce conn (d/create-conn schema))

(p/posh! conn)


;; ACTIONS

(defn delete-todo [conn])

(defn update-todo-status [conn todo-id is-completed]
  (p/transact! conn
               [[:db/add todo-id :todo/is-completed is-completed]]))

(defn update-todo-description [conn todo-id description]
  (p/transact! conn
               [[:db/add todo-id :todo/description description]]))

(defn set-edit-status [conn todo-id is-being-edited]
  (p/transact! conn
               [[:db/add todo-id :todo/is-being-edited is-being-edited]]))

(defn create-todo [conn]
  (p/transact! conn [{:todo/description     "new todo"
                      :todo/is-being-edited true
                      :todo/creation-date   (timestamp)}]))


;; VIEWS

(defn todo-view [conn todo-id]
  (let [todo @(p/pull conn [:todo/description :todo/is-completed :todo/is-being-edited] todo-id)
        description (:todo/description todo)
        is-completed (= (:todo/is-completed todo) true)
        is-being-edited (:todo/is-being-edited todo)]
    [:div.flex.m-1.items-center
     {:style {:height "26px"}}
     [:input.mr-2
      {:type      "checkbox"
       :on-change #(update-todo-status conn todo-id (not is-completed))
       :checked   is-completed}]

     (if is-being-edited
       [:input.p-1
        {:on-change  #(update-todo-description conn todo-id (-> % .-target .-value))
         :on-blur    #(set-edit-status conn todo-id false)
         :on-key-up  (fn [evt]
                       (if (= "Enter" (.-key evt))
                         (set-edit-status conn todo-id false)))
         :value      description
         :auto-focus true}]
       [:div
        {:on-click #(set-edit-status conn todo-id true)
         :style    {:text-decoration (if is-completed
                                       "line-through"
                                       "inherit")}}
        description])]))


(defn app [conn]
  (let [todo-ids (->> @(p/q '[:find ?todo
                              :in $
                              :where
                              [?todo :todo/description _]]
                            conn)
                      (sort)
                      (reverse)
                      (map first))]
    [:div.p-10

     [:div.mb-3.flex.items-center.content-start
      [:h1 "Todos"]

      [:button.bg-blue.p-2.rounded.text-white.ml-4
       {:on-click #(create-todo conn)} "new"]]

     (map
       (fn [todo-id] ^{:key todo-id} [todo-view conn todo-id]) todo-ids)]))


;; INITIALIZATION


(defn render []
  (let [container (.getElementById js/document "app")]
    (r/render [app conn] container)))

(defn init! []
  (p/transact! conn initial-facts)
  (render))

(defn reload! []
  (render))

