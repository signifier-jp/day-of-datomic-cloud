;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.client.api :as d]
         '[datomic.samples.repl :as repl]
         '[clojure.pprint :as pp])

(def conn (repl/scratch-db-conn "config.edn"))

(repl/transact-all conn (repl/resource "day-of-datomic-cloud/social-news.edn"))
(repl/transact-all conn (repl/resource "day-of-datomic-cloud/provenance.edn"))

(def stu [:user/email "stuarthalloway@datomic.com"])

;; Stu loves to pimp his own blog posts...
(def tx-result (d/transact
                 conn
                 {:tx-data
                  [{:story/title "ElastiCache in 6 minutes"
                    :story/url "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html"}
                   {:story/title "Keep Chocolate Love Atomic"
                    :story/url "http://blog.datomic.com/2012/08/atomic-chocolate.html"}
                   {:db/id "datomic.tx"
                    :source/user stu}]}))

;; database t of tx1-result
(def db-t (:t (:db-after tx-result)))

(def editor [:user/email "editor@example.com"])

;; fix spelling error in title
;; note auto-upsert and attribution
(def db (:db-after (d/transact
                     conn
                     {:tx-data
                      [{:story/title "ElastiCache in 5 minutes"
                        :story/url "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html"}
                       {:db/id "datomic.tx"
                        :source/user editor}]})))

(def story [:story/url "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html"])

;; what is the title now?
(d/pull db '[:story/title] story)

;; what was the title as of earlier point in time?
(d/pull (d/as-of db db-t) '[:story/title] story)

;; who changed the title, and when?
(->> (d/q '[:find ?e ?v ?email ?inst ?added
            :in $ ?e
            :where
            [?e :story/title ?v ?tx ?added]
            [?tx :source/user ?user]
            [?tx :db/txInstant ?inst]
            [?user :user/email ?email]]
          (d/history (d/db conn))
          story)
     (sort-by #(nth % 3))
     pp/pprint)

;; what is the entire history of entity e?
(->> (d/q '[:find ?aname ?v ?tx ?inst ?added
            :in $ ?e
            :where
            [?e ?a ?v ?tx ?added]
            [?a :db/ident ?aname]
            [?tx :db/txInstant ?inst]]
          (d/history (d/db conn))
          story)
     seq
     (sort-by #(nth % 2))
     pp/pprint)

(repl/delete-scratch-db conn "config.edn")