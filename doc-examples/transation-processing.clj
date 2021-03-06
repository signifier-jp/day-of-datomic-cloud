;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.client.api :as d])

;; Define the configuration for your client:
(def cfg (read-string (slurp "config.edn")))

(def client (d/client cfg))
(d/create-database client {:db-name "tutorial"})
(def conn (d/connect client {:db-name "tutorial"}))
(def db (d/db conn))

(comment "anomalies")
(d/transact conn {:tx-data [[:this "does not" :make "sense"]]})

(comment "tempid-resolution")
(d/transact conn {:tx-data [[:db/add "item-1" :inv/color :green]
                            [:db/add "item-1" :inv/sku "SKU-2001"]
                            [:db/add "item-1" :inv/size :large]
                            {:db/id "item-2"
                             :inv/sku "SKU-2002"
                             :inv/size :small}]})

(-> conn
    (d/transact {:tx-data [[:db/add "item-1" :inv/color :green]
                           [:db/add "item-1" :inv/sku "SKU-2001"]
                           [:db/add "item-1" :inv/size :large]
                           {:db/id "item-2"
                            :inv/sku "SKU-2002"
                            :inv/size :small}]})
    :tempids)

(comment "unique-identity")
(-> conn
    (d/transact {:tx-data [[:db/add "foo" :inv/sku "SKU-42"]]})
    :tempids
    (get "foo"))
;=> 49460431063875660

(-> conn
    (d/transact {:tx-data [[:db/add "bar" :inv/sku "SKU-42"]]})
    :tempids
    (get "bar"))
;=> 49460431063875660

(comment "unique-value")
(d/transact conn {:tx-data [{:db/ident :reservation/code
                             :db/valueType :db.type/string
                             :db/cardinality :db.cardinality/one
                             :db/unique :db.unique/value}]})
(d/transact conn {:tx-data [[:db/add "" :reservation/code "HQJ43P"]]})

(d/transact conn {:tx-data [[:db/add "" :reservation/code "HQJ43P"]]})

(comment "transaction-timeouts")
(d/transact conn {:tx-data [[:db/add "" :db/doc "might not succeed!"]]
                  :timeout 1})

(d/q '[:find ?e ?when
       :where [?e :db/doc "might not succeed!" ?tx]
       [?tx :db/txInstant ?when]]
     (d/db conn))


(comment "reified-transactions")
(d/transact conn {:tx-data [{:db/ident :data/source
                             :db/valueType :db.type/string
                             :db/cardinality :db.cardinality/one
                             :db/doc "URI this transaction was imported from"}]})
(d/transact conn {:tx-data [{:db/id "datomic.tx"
                             :data/source "http://example.com/catalog-2_29_2012.xml"}
                            {:inv/sku "SKU-42" :inv/color :green :inv/size :large}]})


(comment "explicit-txinstant")
(d/transact conn {:tx-data [{:inv/sku "SKU-42"
                             :inv/color :green}
                            [:db/add "datomic.tx" :db/txInstant #inst "2001"]]})


(comment "redundancy-elimination")
(-> conn
    (d/transact {:tx-data [{:inv/sku "SKU-42"
                            :inv/color :green}]})
    :db-after)
(-> conn
    (d/transact {:tx-data [{:inv/sku "SKU-42"
                            :inv/color :green}]})
    :tx-data)

(d/delete-database client {:db-name "tutorial"})
